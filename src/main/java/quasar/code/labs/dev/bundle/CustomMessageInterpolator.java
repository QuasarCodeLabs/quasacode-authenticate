package quasar.code.labs.dev.bundle;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.MessageInterpolator;
import jakarta.validation.Validation;
import java.util.Locale;

@ApplicationScoped
@RegisterForReflection
public class CustomMessageInterpolator implements MessageInterpolator {

    private final MessageInterpolator defaultInterpolator;

    @Inject
    LocaleResolver localeResolver;

    public CustomMessageInterpolator() {
        // ESTA ES LA SOLUCIÓN:
        // Usamos byDefaultProvider().configure() para obtener el interpolador
        // base sin disparar la creación de una nueva Factory.
        this.defaultInterpolator = Validation.byDefaultProvider()
                .configure()
                .getDefaultMessageInterpolator();
    }

    @Override
    public String interpolate(String messageTemplate, Context context) {
        // Si localeResolver es null aquí (por ser inyección CDI),
        // asegúrate de manejarlo o usar un Locale por defecto.
        Locale locale = (localeResolver != null) ? localeResolver.resolveLocale() : Locale.getDefault();
        return defaultInterpolator.interpolate(messageTemplate, context, locale);
    }

    @Override
    public String interpolate(String messageTemplate, Context context, Locale locale) {
        return defaultInterpolator.interpolate(messageTemplate, context, locale);
    }
}