package quasar.code.labs.dev.service;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import quasar.code.labs.dev.bundle.LocaleResolver;

import java.util.Locale;
import java.util.ResourceBundle;

@RequestScoped
public class EmailTemplateService {
    private final Locale locale;

    @Inject
    public EmailTemplateService(LocaleResolver localeResolver) {
        this.locale = localeResolver.resolveLocale();
    }

    public String getTemplate(String key, Locale locale) {
        ResourceBundle bundle = ResourceBundle.getBundle("email_templates", locale);
        return  bundle.getString(key);
    }
}
