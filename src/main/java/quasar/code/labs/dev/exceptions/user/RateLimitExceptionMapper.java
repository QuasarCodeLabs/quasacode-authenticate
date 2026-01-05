package quasar.code.labs.dev.exceptions.user;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.smallrye.faulttolerance.api.RateLimitException;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import quasar.code.labs.dev.bundle.LocaleResolver;
import quasar.code.labs.dev.bundle.MessageService;

import java.util.Locale;

@Provider
@RegisterForReflection
public class RateLimitExceptionMapper implements ExceptionMapper<RateLimitException> {
   private final MessageService messageService;
    private final LocaleResolver localeResolver;

    @Inject
    RateLimitExceptionMapper(MessageService messageService,LocaleResolver localeResolver ){
        this.messageService = messageService;
        this.localeResolver = localeResolver;
    }

    @Override
    public Response toResponse(RateLimitException e) {
        Locale locale = localeResolver.resolveLocale();
        return Response.status(Response.Status.TOO_MANY_REQUESTS)
                .header("Retry-After", "10")
                .entity(messageService.getMessage("rate_limit_exceeded", locale))
                .build();
    }
}