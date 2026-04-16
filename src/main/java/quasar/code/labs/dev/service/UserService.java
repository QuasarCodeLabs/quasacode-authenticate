package quasar.code.labs.dev.service;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.redis.client.reactive.ReactiveRedisClient;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniEmitter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.mindrot.jbcrypt.BCrypt;
import quasar.code.labs.dev.bundle.LocaleResolver;
import quasar.code.labs.dev.bundle.MessageService;
import quasar.code.labs.dev.entity.App;
import quasar.code.labs.dev.entity.User;
import quasar.code.labs.dev.exceptions.user.UserException;
import quasar.code.labs.dev.repository.AppRepository;
import quasar.code.labs.dev.repository.RedisRepository;
import quasar.code.labs.dev.repository.UserRepository;
import quasar.code.labs.dev.utils.KeyLoader;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;


@ApplicationScoped
//@CircuitBreaker(failureRatio = 0.5, delay = 5000, successThreshold = 3)
public class UserService {
    private static final String EMAIL = "email";
    private static final Logger logger = Logger.getLogger(UserService.class.getName());
    public static final String ISSUER = "https://quasarcode.dev";
    public static final String USER = "player";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UserRepository userRepository;
    private final AppRepository appRepository;
    private final MessageService messageService;
    private final EmailTemplateRenderer emailTemplateRenderer;
    private final LocaleResolver localeResolver;
    private final EmailService emailService;
    private final KafkaProducerService kafkaProducerService;
    private final RedisRepository redisRepository;

    @Inject
    public UserService(UserRepository userRepository, AppRepository appRepository, MessageService messageService, EmailTemplateRenderer emailTemplateRenderer, LocaleResolver localeResolver, EmailService emailService, KafkaProducerService kafkaProducerService, RedisRepository redisRepository) {
        this.userRepository = userRepository;
        this.appRepository = appRepository;
        this.messageService = messageService;
        this.emailTemplateRenderer = emailTemplateRenderer;
        this.localeResolver = localeResolver;
        this.emailService = emailService;
        this.kafkaProducerService = kafkaProducerService;
        this.redisRepository = redisRepository;
    }

    /**
     * Documentación para el método `authenticate` que utiliza autenticación basada en contraseña y correo electrónico.
     *
     * @param username El nombre de usuario del usuario a autenticar.
     * @param password La contraseña del usuario a autenticar.
     * @param email La dirección de correo electrónico del usuario a autenticar.
     *
     * @return Un `Uni` con el resultado de la autenticación, incluyendo cookies de autenticación y token refresh.
     *
     * @throws `RuntimeException` Si no se encuentra el usuario en el sistema de usuarios.
     */
    @Timeout(value = 10, unit = ChronoUnit.SECONDS)
    @CircuitBreaker(
            requestVolumeThreshold = 10,
            failureRatio = 0.5,
            delay = 5000,
            successThreshold = 5,
            skipOn = UserException.class)
    @Fallback(fallbackMethod = "fallbackSystemOccupied",
            skipOn = UserException.class)
    @WithTransaction
    //@RateLimit(value = 5, window = 10, windowUnit = ChronoUnit.SECONDS)
    public Uni<Response> authenticate(String username, String password, String email) {
        Locale locale = localeResolver.resolveLocale();

           return userRepository.findByUsername(username, email)
                    .onItem().ifNull().failWith(() -> new RuntimeException("User not found"))
                    .onItem().transformToUni(user -> {
                        if (!checkPassword(password, user.getPassword()) && Boolean.TRUE.equals(user.getActive())) {
                            throw new UserException(messageService.getMessage("valid_access", locale));
                        }
                        String token = generateTokenUser(user.getEmail(),user);
                        String refreshToken = generateRefreshTokenUser(user.getEmail(),user);
                       return Uni.createFrom().item(Response.ok("Acceso exitoso")
                               .cookie(generateCookie(token))
                                .cookie(generateRefreshCookie(refreshToken)).build());
                    }).onFailure().recoverWithItem(throwable ->
                        Response.status(Response.Status.UNAUTHORIZED)
                                .entity(throwable.getMessage())
                                .build()
                    );
    }
    /**
     * Este método se ejecuta cuando el circuito está abierto o falla el principal.
     */
    public Uni<Response> fallbackSystemOccupied(String username, String password, String email) {
        Locale locale = localeResolver.resolveLocale();
        // Lanzamos la excepción para que el Mapper haga el trabajo de la respuesta
        return Uni.createFrom().failure(new UserException(messageService.getMessage("system_occupies", locale)));
    }

    /**
     * Registra un usuario con los datos proporcionados.
     *
     * @param user El objeto User que se va a registrar
     * @return Una instancia de Uni<Void> con resultado del registro
     * @throws UserException Si ocurren errores de autenticación como:
     *         - Contraseña inválida
     *         - Usuario ya existe con el mismo nombre
     *         - Correo si ya existente
     */
    @Retry(
            maxRetries = 3,
            delay = 200,
            retryOn = {
                    java.io.IOException.class,
                    java.util.concurrent.TimeoutException.class,
                    //io.vertx.redis.client.RedisException.class,
                    java.sql.SQLException.class
            },
            abortOn = {
                    UserException.class,
                    ConstraintViolationException.class
            }
    )
/*    @Timeout(value = 10, unit = ChronoUnit.SECONDS)
    @CircuitBreaker(
            requestVolumeThreshold = 10,
            failureRatio = 0.5,
            delay = 5000,
            successThreshold = 5,
            skipOn = UserException.class)
    @Fallback(fallbackMethod = "fallbackSystemOccupied",
            skipOn = UserException.class)*/
    @WithTransaction
    public Uni<Void> registerUser(User user) {

        Locale locale = localeResolver.resolveLocale();


        if (!validPassword(user.getPassword())) {
            return Uni.createFrom().failure(
                    new UserException(messageService.getMessage("valid_password", locale))
            );
        }

        if (user.getApps() == null || user.getApps().isEmpty()) {
            return Uni.createFrom().failure(
                    new UserException(messageService.getMessage("app_required", locale))
            );
        }


            List<Uni<App>> validationTasks = user.getApps().stream()
                    .map(app ->  redisRepository.leerClaveReactiva("app:" + app.id.toString())
                            .onItem().ifNotNull().transform(json -> {
                                try {
                                    logger.info("JSON:"+json);
                                    return objectMapper.readValue(json, App.class);
                                } catch (JsonProcessingException e) {
                                    throw new RuntimeException(e);
                                }
                            }).onFailure(RuntimeException.class)
                            .invoke(f -> logger.severe("Error parsing JSON: " + f.getMessage()))
                            .onFailure()
                           .recoverWithNull()
                            .onItem().ifNull().switchTo(
                            appRepository.findById(app.id)
                                    .onItem().ifNull().failWith(() ->
                                            new UserException(messageService.getMessage("app_not_exist", locale))
                                    )
                            )
                    ).toList();

        return Uni.combine().all().unis(validationTasks)
                .with(list -> {
                    List<App> appsEncontradas = list.stream()
                            .filter(Objects::nonNull)
                            .map(App.class::cast)
                            .toList();

                    user.setApps(appsEncontradas);
                    return user;
                })

                // 3️⃣ Buscar si el usuario ya existe para esas apps
                .onItem().transformToUni(validatedUser -> {
                            String indexName = "user:index:username:" + validatedUser.getUsername();
                            String indexEmail = "user:index:email:" + validatedUser.getEmail();

                            // Intentamos buscar por Username en Redis
                            return redisRepository.leerClaveReactiva(indexName)
                                    // Si no hay por username, buscamos por email en Redis
                                    .onItem().ifNull().switchTo(() -> redisRepository.leerClaveReactiva(indexEmail))
                                    // Ahora tenemos un Uni que puede traer el ID (o ser null)
                                    .onItem().transformToUni(userId -> {
                                        if (userId != null) {
                                            // Si encontramos el ID, traemos el objeto completo de Redis
                                            return redisRepository.leerClaveReactiva("user:" + userId)
                                                    .onItem().transform(json -> {
                                                        try {
                                                            return objectMapper.readValue(json, User.class);
                                                        } catch (Exception e) {
                                                            return null;
                                                        }
                                                    });
                                        }
                                        // Si userId es null, devolvemos null para que el siguiente paso sepa que debe ir a DB
                                        return Uni.createFrom().nullItem();
                                    })
                                    .onItem().ifNull().switchTo(() ->
                                            userRepository.find(
                                                            "FROM User u LEFT JOIN FETCH u.apps WHERE u.username = ?1 OR u.email = ?2",
                                                            validatedUser.getUsername(),
                                                            validatedUser.getEmail()
                                                    ).firstResult()
                                                    .onItem().ifNotNull().call(userFromDb -> {
                                                        try {
                                                            logger.info("Usuario encontrado en DB, actualizando Redis para la próxima vez...");
                                                            String userJson = objectMapper.writeValueAsString(userFromDb);
                                                            String userId = userFromDb.id.toString();

                                                            // Guardamos el objeto y sus dos índices (username y email)
                                                            return Uni.combine().all().unis(
                                                                    redisRepository.guardarClaveValor("user:" + userId, userJson),
                                                                    redisRepository.guardarClaveValor("user:index:username:" + userFromDb.getUsername(), userId),
                                                                    redisRepository.guardarClaveValor("user:index:email:" + userFromDb.getEmail(), userId)
                                                            ).discardItems();

                                                        } catch (Exception e) {
                                                            logger.severe("No se pudo serializar el usuario para Redis: " + e.getMessage());
                                                            return Uni.createFrom().voidItem(); // Continuamos aunque falle el caché
                                                        }

                                                    })
                                    )

                                .onItem().transformToUni(existingUser -> {
                                        User userToProcess;
                                    if (existingUser != null) {
                                        Set<Long> existingAppIds = existingUser.getApps().stream()
                                                .map(app -> app.id)
                                                .collect(Collectors.toSet());

                                        Set<Long> requestedAppIds = validatedUser.getApps().stream()
                                                .map(app -> app.id)
                                                .collect(Collectors.toSet());
                                        Set<Long> duplicatedApps = new HashSet<>(existingAppIds);
                                        duplicatedApps.retainAll(requestedAppIds);
                                        if (!duplicatedApps.isEmpty()) {
                                            String errorKey = existingUser.getEmail().equalsIgnoreCase(validatedUser.getEmail())
                                                    ? "email_exist"
                                                    : "user_exist";
                                            return Uni.createFrom().<User>failure(
                                                    new UserException(messageService.getMessage(errorKey, locale))
                                            );
                                        }

                                        validatedUser.getApps().forEach(app -> {
                                            if (!existingUser.getApps().contains(app)) {
                                                existingUser.getApps().add(app);
                                            }
                                        });
                                        userToProcess = existingUser;
                                        //return kafkaProducerService.send(existingUser)
                                                //.replaceWith(existingUser);
                                        //return userRepository.persist(existingUser);
                                    } else {
                                        validatedUser.setPassword(
                                                BCrypt.hashpw(validatedUser.getPassword(), BCrypt.gensalt())
                                        );
                                        validatedUser.setRole("prospect");
                                        validatedUser.setActive(false);
                                        userToProcess = validatedUser;
                                    }
                                        return saveUserInRedis(userToProcess)
                                                .onItem().call(
                                                        savedUser ->
                                                                kafkaProducerService.send(savedUser)
                                                                        .replaceWith(savedUser)
                                                );
                                    //return kafkaProducerService.send(validatedUser)
                                            //.replaceWith(validatedUser);
                                    //return userRepository.persist(validatedUser);
                                }
                )

                .onItem().transformToUni(persistedUser -> {
                    String tokenProspect = generateVerificationToken(persistedUser.getEmail());
                    return sendVerificationEmail(
                            persistedUser.getEmail(),
                            persistedUser.getUsername(),
                            tokenProspect
                    );
                })

                .onFailure().transform(throwable -> {
                    if (throwable instanceof ConstraintViolationException) {
                        return new UserException(
                                messageService.getMessage("constrain_exception", locale)
                        );
                    }
                    if (throwable instanceof UserException) {
                        return throwable;
                    }
                    return new UserException(
                            messageService.getMessage("registration_error", locale)
                    );
                });

    });
    }

    /**
     * Este método se ejecuta cuando el circuito está abierto o falla el principal.
     */
    public Uni<Void> fallbackSystemOccupied(User user) {
        Locale locale = localeResolver.resolveLocale();
        // Lanzamos la excepción para que el Mapper haga el trabajo de la respuesta
        return Uni.createFrom().failure(new UserException(messageService.getMessage("system_occupies", locale)));
    }


    @WithTransaction
    public Uni<NewCookie> activateUser(String token) {
        Locale locale = localeResolver.resolveLocale();

        return Uni.createFrom().deferred(() -> {
                    KeyLoader keyLoader = new KeyLoader();
                    try {
                        RSAPublicKey publicKey = keyLoader.loadPublicKey();
                        return Uni.createFrom().item(publicKey);
                    } catch (NoSuchAlgorithmException | IOException | InvalidKeySpecException e) {
                        return Uni.createFrom().failure(new RuntimeException("Failed to load public key", e));
                    }
                })
                .onItem().transform(publicKey -> {
                    // Verificar el token JWT
                    JWTVerifierService jwtVerifierService = new JWTVerifierService(publicKey);
                    DecodedJWT decodedJWT = jwtVerifierService.verifyToken(token);
                    if (decodedJWT.getSubject() == null || decodedJWT.getSubject().isBlank()) {
                        throw new UserException("Token does not contain subject");
                    }
                    return decodedJWT;
                })
                .onItem().transformToUni(decodedJWT ->
                        // Buscar el usuario en la base de datos
                        userRepository.find("email = ?1 and active = ?2", decodedJWT.getSubject(), Boolean.FALSE)
                                .firstResult()
                                .onItem().ifNull().failWith(() -> new UserException("Token verification failed"))
                )
                .onItem().transformToUni(user -> {
                    // Activar al usuario y generar el token
                    user.setActive(Boolean.TRUE);
                    user.setRole(USER);

                    return userRepository.persist(user) // Persistir cambios
                            .onItem().ifNotNull().transformToUni(v -> {
                                String newToken = generateTokenUser(v.getEmail(), v); // Generar token
                                return Uni.createFrom().item(generateCookie(newToken)); // Generar cookie
                            });

                })
                .onFailure().recoverWithItem(throwable -> {
                    // Manejar errores y devolver un valor predeterminado
                    if (throwable instanceof PersistenceException && throwable.getCause() instanceof ConstraintViolationException) {
                        throw new UserException(messageService.getMessage("constrain_exception", locale));
                    }
                    throw new UserException(messageService.getMessage("registration_error", locale));
                });
    }


    public Uni<NewCookie> refreshCookie(String token) {
        return Uni.createFrom().deferred(() -> {
            KeyLoader keyLoader = new KeyLoader();
            try {
                RSAPublicKey publicKey = keyLoader.loadPublicKey();
                return Uni.createFrom().item(publicKey);
            } catch (NoSuchAlgorithmException | IOException | InvalidKeySpecException e) {
                return Uni.createFrom().failure(new RuntimeException("Failed to load public key", e));
            }
        }).onItem().transform(rsaPublicKey -> {
            JWTVerifierService jwtVerifierService = new JWTVerifierService(rsaPublicKey);
            return jwtVerifierService.verifyToken(token);
        }).onItem().transformToUni(decodedJWT -> {
            String[] groups = decodedJWT.getClaim("groups").asArray(String.class);
            if (groups == null || Arrays.stream(groups).noneMatch("refresh"::equals)) {
                throw new UserException("Invalid or missing roles");
            }
            if (decodedJWT.getSubject().isBlank()) {
                throw new UserException("Token not contain subject");
            }
            return userRepository.find("email = ?1 and active = ?2", decodedJWT.getSubject(), Boolean.TRUE).firstResult()
                    .onItem().ifNull().failWith(() -> new UserException("Token verification failed:"));
        }).onItem().transform(player ->
             generateCookie(generateTokenUser(player.getEmail(), player))
        ).onFailure().recoverWithItem(throwable -> {
            Locale locale = localeResolver.resolveLocale();
            throw new UserException(messageService.getMessage("registration_error", locale));
        });

    }






    public String generateJwt() {
        Set<String> roles = new HashSet<>(Arrays.asList(USER, "admin"));
        long expirationTime = System.currentTimeMillis() / 1000 + 3600;
        return Jwt.issuer(ISSUER)
                .groups(roles)
                .expiresAt(expirationTime)
                .sign();
    }

    private Uni<Void> sendVerificationEmail(String email, String name, String token) {
        return Uni.createFrom().item(() -> {
                    // 1. Renderizar el asunto y el cuerpo del correo de forma síncrona
                    String subject = emailTemplateRenderer.renderTemplate("verify_mail.subject");
                    Map<String, String> variables = Map.of("name", name, "token", token);
                    String emailBody = emailTemplateRenderer.renderTemplate("verify_mail.body", variables);

                    logger.log(Level.INFO, "Preparando envío de correo para: {0}", email);

                    // Retornamos los datos necesarios para el siguiente paso
                    return new AbstractMap.SimpleEntry<>(subject, emailBody);
                })
                .onItem().transformToUni(entry -> {
                    String subject = entry.getKey();
                    String emailBody = entry.getValue();

                    // 2. IMPORTANTE: Retornar el Uni del servicio de correo
                    return emailService.sendVerificationEmail(email, subject, emailBody);
                })
                .onFailure().invoke(throwable -> {
                    logger.severe("Error crítico al enviar el correo a " + email + ": " + throwable.getMessage());
                });
    }

    private String generateVerificationToken(String email) {
        return Jwt.issuer(ISSUER)
                .subject(email) // Usa el correo como "sub"
                .groups("prospect")
                .expiresIn(Duration.ofMinutes(10)) // Expiración de 10 minutos
                .sign();
    }

    private String generateTokenUser(String email, User user) {
        Set<String> roles = new HashSet<>();
        roles.add(user.getRole());
        return Jwt.issuer(ISSUER)
                .subject(email) // Usa el correo como "sub"
                .groups(roles)
                .expiresIn(Duration.ofMinutes(30)) // Expiración de 24 horas
                .sign();
    }

    private String generateRefreshTokenUser(String email, User user) {
        Set<String> roles = new HashSet<>();
        roles.add(user.getRole());
        return Jwt.issuer(ISSUER)
                .subject(email)
                .groups("refresh")
                .expiresIn(Duration.ofDays(30)) // Expiración de 24 horas
                .sign();
    }


    public boolean checkPassword(String plainPassword, String hashedPassword) {
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }

    private boolean validPassword(String password) {
        String regex = "^(?=.*[0-9])(?=.*[!@#$%^&*()_+{}\\\\[\\\\]|:;<>,.?~/-])[A-Za-z0-9!@#$%^&*()_+{}\\\\[\\\\]|:;<>,.?~/-]{6,72}$";
        return (password.matches(regex) && !password.isBlank());
    }

    private NewCookie generateCookie(String token){
        return new NewCookie.Builder("token")
                .value(token)
                .path("/")
                .maxAge(3600) // 1 hora
                .httpOnly(true)
                .secure(true)
                .sameSite(NewCookie.SameSite.NONE)
                .build();
    }
    private NewCookie generateRefreshCookie(String refreshToken){
        return new NewCookie.Builder("tokenRefresh")
                .value(refreshToken)
                .path("/")
                .maxAge(2592000) // 30 días
                .httpOnly(true)
                .secure(true)
                .sameSite(NewCookie.SameSite.NONE)
                .build();
    }

    private Uni<User> saveUserInRedis(User user) {
        try {
            UUID uuid = UUID.randomUUID();
            user.setId(uuid);
            String json = objectMapper.writeValueAsString(user);

            return Uni.combine().all().unis(
                    redisRepository.guardarClaveValor("user:" + uuid.toString(), json),
                    redisRepository.guardarClaveValor("user:index:username:" + user.getUsername(), uuid.toString()),
                    redisRepository.guardarClaveValor("user:index:email:" + user.getEmail(), uuid.toString())
            ).asTuple().replaceWith(user); // Retorna el objeto User original al terminar

        } catch (JsonProcessingException e) {
            logger.severe("Error serializando usuario para Redis: " + e.getMessage());
            // Si falla el caché, podrías decidir si fallar todo o solo loguear
            return Uni.createFrom().item(user);
        }
    }
}
