package quasar.code.labs.dev.bundle;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;

import java.util.Locale;

@ApplicationScoped
@RegisterForReflection
public class ValidatorConfig {


    public Validator createValidator(Locale locale) {
        // Si el Locale es nulo, usar el predeterminado del sistema
        if (locale == null) {
            locale = Locale.getDefault();  // Por ejemplo, en inglés
        }

        try (ValidatorFactory factory = Validation.byDefaultProvider()
                .configure()
                .messageInterpolator(new ResourceBundleMessageInterpolator(
                ))
                .buildValidatorFactory()) {

            return factory.getValidator();
        }
    }
}

