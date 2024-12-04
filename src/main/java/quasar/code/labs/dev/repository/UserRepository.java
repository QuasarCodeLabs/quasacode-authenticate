package quasar.code.labs.dev.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import quasar.code.labs.dev.entity.User;

import java.util.Optional;

@ApplicationScoped
public class UserRepository implements PanacheRepository<User> {

    public Optional<User> findByUsername(String username, String email) {
        return find("username = ?1 or email = ?2", username, email).firstResultOptional();
    }
}