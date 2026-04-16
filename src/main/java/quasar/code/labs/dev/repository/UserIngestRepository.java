package quasar.code.labs.dev.repository;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hibernate.reactive.mutiny.Mutiny;
import quasar.code.labs.dev.entity.User;

import java.util.List;

@ApplicationScoped
public class UserIngestRepository {
    @Inject
    Mutiny.SessionFactory sessionFactory;

    public Uni<Void> upsertBatch(List<User> users) {
        return sessionFactory
                .withStatelessSession(session ->
                        session.upsertAll(users)
                )
                .replaceWithVoid();
    }
}
