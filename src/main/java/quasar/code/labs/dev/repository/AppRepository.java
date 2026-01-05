package quasar.code.labs.dev.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import quasar.code.labs.dev.entity.App;

@ApplicationScoped
public class AppRepository implements PanacheRepository<App> {
}
