package quasar.code.labs.dev.controller;


import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import quasar.code.labs.dev.bundle.LocaleResolver;
import quasar.code.labs.dev.bundle.MessageService;
import quasar.code.labs.dev.dto.TokenResponse;
import quasar.code.labs.dev.entity.User;
import quasar.code.labs.dev.service.UserService;

import java.util.Locale;
import java.util.Optional;

@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {
    @Inject
    AuthResource(UserService userService, MessageService messageService,LocaleResolver localeResolver){
        this.userService = userService;
        this.messageService = messageService;
        this.localeResolver = localeResolver;
    }
    private final UserService userService;
    private final MessageService messageService;
    private final LocaleResolver localeResolver;


    @POST
    @Path("/prospect")
    @PermitAll
    public Response prospectus(User loginRequest) {
        Locale locale = localeResolver.resolveLocale();
        userService.registerUser(loginRequest);
        return Response.status(Response.Status.CREATED).entity(messageService.getMessage("send_mail",locale)).build();
    }

    @POST
    @Path("/sing_in")
    @PermitAll
    public Response login(User loginRequest) {
        Optional<String> token = userService.authenticate(loginRequest.getUsername(), loginRequest.getPassword(), loginRequest.getEmail());
        if (token.isPresent()) {
            return Response.ok(new TokenResponse(token.get())).build();
        }
        return Response.status(Response.Status.UNAUTHORIZED).entity("invalid_access").build();
    }

    @POST
    @Path("/verify-email")
    @PermitAll
    public Response verify(@QueryParam("token") String token){
        try{
        userService.activateUser(token);
          return Response.ok("Your email has been verified successfully!").build();
      } catch (Exception e) {
          // Manejo de errores (token inválido o expirado)
          return Response.status(Response.Status.BAD_REQUEST)
                  .entity("Invalid or expired token.")
                  .build();
      }
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
