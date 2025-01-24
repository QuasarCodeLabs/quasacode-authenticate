package quasar.code.labs.dev.service;

import com.auth0.jwt.interfaces.DecodedJWT;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.mindrot.jbcrypt.BCrypt;
import quasar.code.labs.dev.bundle.LocaleResolver;
import quasar.code.labs.dev.bundle.MessageService;
import quasar.code.labs.dev.entity.User;
import quasar.code.labs.dev.exceptions.user.UserException;
import quasar.code.labs.dev.repository.UserRepository;
import quasar.code.labs.dev.utils.KeyLoader;

import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


@ApplicationScoped
@CircuitBreaker(failureRatio = 0.5, delay = 5000, successThreshold = 3)
public class UserService {
    private static final String EMAIL = "email";
    private static final Logger logger = Logger.getLogger(UserService.class.getName());
    public static final String ISSUER = "https://quasarcode.dev";
    public static final String USER = "user";

    private final UserRepository userRepository;
    private final MessageService messageService;
    private final EmailTemplateRenderer emailTemplateRenderer;
    private final LocaleResolver localeResolver;

    @Inject
    public UserService(UserRepository userRepository, MessageService messageService,EmailTemplateRenderer emailTemplateRenderer, LocaleResolver localeResolver) {
        this.userRepository = userRepository;
        this.messageService = messageService;
        this.emailTemplateRenderer = emailTemplateRenderer;
        this.localeResolver = localeResolver;
    }


    @Transactional
    public Optional<String> authenticate(String username, String password, String email) {
        Optional<User> userOpt = userRepository.findByUsername(username, email);
        if (userOpt.isPresent() && checkPassword(password, userOpt.get().getPassword()) && Boolean.TRUE.equals(userOpt.get().getActive())) {
            User user = userOpt.get();
            Set<String> roles = new HashSet<>();
            roles.add(user.getRole());
            String token = Jwt.issuer(ISSUER)
                    .upn(user.getUsername())
                    .groups(roles)
                    .expiresAt(System.currentTimeMillis() + 3600)
                    .sign();
            return Optional.of(token);
        }
        return Optional.empty();
    }

    @Transactional
    public void registerUser(User user) {
        Locale locale = localeResolver.resolveLocale();
        if (userRepository.find("username", user.getUsername()).singleResultOptional().isPresent()) {
            throw new UserException(messageService.getMessage("user_exist",locale));
        } else if (userRepository.find(EMAIL, user.getEmail()).singleResultOptional().isPresent()) {
            throw new UserException(messageService.getMessage("email_exist",locale));
        }
        user.setRole("prospect");
        user.setActive(false);
        userRepository.persist(user);

        String tokenProspect =  messageService.getMessage("verify_mail",locale)+generateVerificationToken(user.getEmail());
        sendVerificationEmail(user.getEmail(),user.getUsername(), tokenProspect);
    }

    @Transactional
    public void activateUser(String token) {
        try {
            KeyLoader keyLoader = new KeyLoader();
            RSAPublicKey publicKey = keyLoader.loadPublicKey();
            JWTVerifierService jwtVerifierService = new JWTVerifierService(publicKey);
            DecodedJWT decodedJWT = jwtVerifierService.verifyToken(token);
            if(decodedJWT.getSubject().isBlank()){
                throw new UserException("Token not contain subject");
            }
            Optional<User> prospect = userRepository.find("email = ?1 and active = ?2",decodedJWT.getSubject() , Boolean.FALSE).firstResultOptional();
            if(prospect.isPresent()){
                User user = prospect.get();
                user.setActive(Boolean.TRUE);
                user.setRole(USER);
                userRepository.persist(user);
            }
        } catch (Exception e) {
            throw new UserException("Token verification failed:"+e.getMessage());
        }
    }


    public String generateJwt() {
        Set<String> roles = new HashSet<>(Arrays.asList(USER, "admin"));
        long duration = System.currentTimeMillis() + 3600;
        return Jwt.issuer(ISSUER)
                .groups(roles)
                .expiresAt(duration)
                .sign();
    }

    private void sendVerificationEmail(String email,String name ,String token) {
        try {

// Configuración del servicio de correo
            Properties props = EmailServiceConfig.loadEmailConfig();
            EmailService emailService = new EmailService(
                    props.getProperty("email.host"),
                    props.getProperty("email.port"),
                    props.getProperty("email.username"),
                    props.getProperty("email.password")
            );


            String subject = emailTemplateRenderer.renderTemplate("verify_mail.subject");
            Map<String, String> variables = Map.of("name", name, "token", token);
            String emailBody = emailTemplateRenderer.renderTemplate("verify_mail.body", variables);
            logger.log(Level.INFO, "Subject: {0}", subject);
            logger.log(Level.INFO, "Email Body: {0}", emailBody);
            emailService.send(
                    email,
                    subject,
                    emailBody
            );
            logger.info("Correo enviado con éxito.");
        } catch (MessagingException e) {
            logger.info(e.fillInStackTrace().getMessage());
        }
    }

    private String generateVerificationToken(String email) {
        return Jwt.issuer(ISSUER)
                .subject(email) // Usa el correo como "sub"
                .groups("prospect")
                .expiresIn(Duration.ofHours(24)) // Expiración de 24 horas
                .sign();
    }

    public boolean checkPassword(String plainPassword, String hashedPassword) {
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }
}
