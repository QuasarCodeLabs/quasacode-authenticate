package quasar.code.labs.dev.exceptions.user;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class UserException extends RuntimeException {
    public UserException(String message) {
        super(message);
    }
}