package quasar.code.labs.dev.controller;


import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.security.UnauthorizedException;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import quasar.code.labs.dev.bundle.LocaleResolver;
import quasar.code.labs.dev.bundle.MessageService;
import quasar.code.labs.dev.dto.TokenResponse;
import quasar.code.labs.dev.entity.User;
import quasar.code.labs.dev.exceptions.user.UserException;
import quasar.code.labs.dev.service.UserService;
import quasar.code.labs.dev.utils.TokenCache;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.smallrye.mutiny.helpers.spies.Spy.onItem;

@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {


    TokenCache tokenCache = new TokenCache(240000);

    @Inject
    AuthResource(UserService userService, MessageService messageService, LocaleResolver localeResolver) {
        this.userService = userService;
        this.messageService = messageService;
        this.localeResolver = localeResolver;
    }

    private final UserService userService;
    private final MessageService messageService;
    private final LocaleResolver localeResolver;
    private static final Logger logger = Logger.getLogger(AuthResource.class.getName());


    @POST
    @Path("/prospect")
    @PermitAll
    public Uni<Response> prospectus(User loginRequest) {
        Locale locale = localeResolver.resolveLocale();

        // Registrar al usuario de forma reactiva
        return userService.registerUser(loginRequest)
                .onItem().transform(ignored ->
                     Response.status(Response.Status.CREATED)
                            .entity(messageService.getMessage("send_mail", locale))
                            .build()
                )
                .onFailure().recoverWithItem(throwable ->
                    // Manejar errores y devolver una respuesta HTTP 400
                     Response.status(Response.Status.BAD_REQUEST)
                            .entity(throwable.getMessage())
                            .build()
                );
    }

    @POST
    @Path("/sing_in")
    @PermitAll
    public Uni<Response> login(User loginRequest) {
            return userService.authenticate(loginRequest.getUsername(), loginRequest.getPassword(), loginRequest.getEmail())
            .onItem().transform(response -> response) // Transforma el Response si es necesario
                    .onFailure().recoverWithItem(throwable ->
                        Response.status(Response.Status.UNAUTHORIZED)
                                .entity("invalid_access")
                                .build()
                    );
    }

    @POST
    @Path("/verify-email")
    @PermitAll
    public Uni<Response> verify(@QueryParam("token") String token) {
        Locale locale = localeResolver.resolveLocale();
        // Verificar si el token ya ha sido usado
        if (tokenCache.isTokenUsed(token)) {
            return Uni.createFrom().item(
                    Response.status(Response.Status.BAD_REQUEST)
                            .entity(messageService.getMessage("verify_success", locale))
                            .build()
            );
        }
        return userService.activateUser(token)
                .onItem().ifNotNull().transform(newCookie -> {
                    tokenCache.markTokenAsUsed(token);
                    return Response.ok(messageService.getMessage("verify_success", locale))
                            .cookie(newCookie)
                            .build();

                })
                .onFailure().recoverWithItem(throwable ->
                    // Si ocurre un error, devolvemos una respuesta de error
                     Response.status(Response.Status.BAD_REQUEST)
                            .entity(messageService.getMessage("verify_unsuccess", locale))
                            .build()
                );
    }

    @POST
    @Path("/refresh")
    @PermitAll
    public Uni<Response> refresh(@CookieParam("tokenRefresh") String token) {
        if (token == null) {
            return Uni.createFrom().item(
                    Response.status(Response.Status.UNAUTHORIZED)
                            .entity("Token no encontrado en la cookie")
                            .build()
            );
        }
        return userService.refreshCookie(token)
                .onItem().ifNotNull().transform(newCookie ->
                    Response.ok().cookie(newCookie).build()
                ).onFailure().recoverWithItem(throwable -> {
                    if(throwable instanceof UnauthorizedException){
                        logger.info("Entro a UnauthorizedException:"+throwable.toString());
                        return Response.status(Response.Status.BAD_REQUEST)
                                .cookie(new NewCookie.Builder("token").value("").path("/").build())
                                .entity(throwable.getMessage())
                                .build();
                    }
                    return Response.status(Response.Status.BAD_REQUEST)
                            .cookie(new NewCookie.Builder("token").value("").path("/").build())
                            .entity("Invalid or expired token.")
                            .build();
                });
    }

    @POST
    @Path("/validate")
    @RolesAllowed("player")
    public Response validateSession(@CookieParam("token") String token) {
            return Response.ok().build(); // Si el token es válido, retorna 200 OK
    }



    @GET
    @Path("/jwt")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getJwt() {
        String jwt = "";
        jwt = userService.generateJwt();
        return Response.ok(jwt).build();
    }
}
