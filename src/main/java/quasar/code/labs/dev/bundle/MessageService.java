package quasar.code.labs.dev.bundle;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

import java.util.Locale;
import java.util.ResourceBundle;

@RequestScoped
@RegisterForReflection
public class MessageService {

    public String getMessage(String key, Locale locale) {
        ResourceBundle bundle = ResourceBundle.getBundle("messages",locale);
        return bundle.getString(key);
    }
}
