package quasar.code.labs.dev.exceptions.user;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import quasar.code.labs.dev.bundle.CustomMessageInterpolator;

import java.util.Locale;
import java.util.stream.Collectors;

@Provider
@RegisterForReflection
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Inject
    ConstraintViolationExceptionMapper(CustomMessageInterpolator messageInterpolator){
        this.messageInterpolator = messageInterpolator;
    }

    private final CustomMessageInterpolator messageInterpolator; // Usamos el interpolador personalizado

    @Override
    public Response toResponse(ConstraintViolationException e) {

        // Interpolar mensajes con el Locale correcto
        String message = e.getConstraintViolations()
                .stream()
                .map(violation -> messageInterpolator.interpolate(violation.getMessageTemplate(), null))
                .collect(Collectors.joining(", "));

        return Response
                .status(Response.Status.BAD_REQUEST)
                .entity(message)
                .build();
    }
}
