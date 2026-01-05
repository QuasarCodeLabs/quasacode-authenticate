package quasar.code.labs.dev.exceptions.user;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;


@RegisterForReflection
@Provider
public class UserExceptionMapper implements ExceptionMapper<UserException> {

    @Override
    public Response toResponse(UserException exception) {
        return Response.status(Response.Status.CONFLICT)
                .entity(exception.getMessage())
                .cookie(new NewCookie.Builder("token").value("").path("/").build())
                .cookie(new NewCookie.Builder("tokenRefresh").value("").path("/").build()).build();
    }
}