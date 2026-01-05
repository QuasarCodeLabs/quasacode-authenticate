package quasar.code.labs.dev.exceptions.user;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import quasar.code.labs.dev.bundle.LocaleResolver;
import quasar.code.labs.dev.bundle.MessageService;

import java.util.Locale;

@Provider
@RegisterForReflection
public class CircuitBreakerOpenExceptionMapper  implements ExceptionMapper<CircuitBreakerOpenException> {
    private final MessageService messageService;
    private final LocaleResolver localeResolver;

    @Inject
    CircuitBreakerOpenExceptionMapper(MessageService messageService, LocaleResolver localeResolver) {
        this.messageService = messageService;
        this.localeResolver = localeResolver;
    }


    @Override
    public Response toResponse(CircuitBreakerOpenException exception) {
        Locale locale = localeResolver.resolveLocale();
        return Response.status(Response.Status.TOO_MANY_REQUESTS)
                .entity(messageService.getMessage("system_occupies", locale))
                .build();
    }
}