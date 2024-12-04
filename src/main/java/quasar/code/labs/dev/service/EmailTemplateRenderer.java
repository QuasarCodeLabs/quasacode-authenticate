package quasar.code.labs.dev.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.text.StringSubstitutor;
import quasar.code.labs.dev.bundle.LocaleResolver;
import quasar.code.labs.dev.exceptions.user.UserException;

import java.util.Locale;
import java.util.Map;
@ApplicationScoped
public class EmailTemplateRenderer {

    @Inject
    private LocaleResolver localeResolver;

    @Inject
    private EmailTemplateService templateService;

    public String renderTemplate(String key, Map<String, String> variables) {
        Locale local = localeResolver.resolveLocale();
        String template = templateService.getTemplate(key,local);
        if (template == null) {
            throw new UserException("Template not found: " + key);
        }
        if (variables == null) {
            throw new UserException("Variables not found:");
        }
            return StringSubstitutor.replace(template, variables, "{", "}");
    }

    public String renderTemplate(String key) {
        Locale local = localeResolver.resolveLocale();
        String template = templateService.getTemplate(key,local);
        if (template == null) {
            throw new UserException("Template not found: " + key);
        }
        return template;
    }
}