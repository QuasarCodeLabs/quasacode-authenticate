package quasar.code.labs.dev.bundle;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;

import java.util.Locale;
@RequestScoped
@RegisterForReflection
public class LocaleResolver {


    private final ContainerRequestContext requestContext;

    @Inject
    public LocaleResolver(ContainerRequestContext requestContext) {
        this.requestContext = requestContext;
    }

    public Locale resolveLocale() {
        Locale.setDefault(Locale.ENGLISH);
        String lang = requestContext.getUriInfo().getQueryParameters().getFirst("lang");
        if (lang != null && !lang.isEmpty()) {
            Locale locale = Locale.forLanguageTag(lang);
            if (isSupportedLocale(locale)) {
                return Locale.of(locale.getLanguage()); // Solo conserva el idioma
            }

        }
        return Locale.getDefault();
    }
    private boolean isSupportedLocale(Locale locale) {
        // Lista de idiomas soportados
        return locale.getLanguage().equals("en") || locale.getLanguage().equals("es") || locale.getLanguage().equals("fr");
    }
}
