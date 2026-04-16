package quasar.code.labs.dev.service;

import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.annotations.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import quasar.code.labs.dev.entity.User;

@ApplicationScoped
public class KafkaProducerService {

    @Inject
    @Channel("authenticate")
    Emitter<User> emitter;

    public Uni<Void> send(User evento) {
        return Uni.createFrom().completionStage(
                emitter.send(evento).toCompletableFuture()
        ).replaceWithVoid();
    }
}
