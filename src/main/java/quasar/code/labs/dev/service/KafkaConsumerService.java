package quasar.code.labs.dev.service;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.virtual.threads.VirtualThreads;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import quasar.code.labs.dev.entity.User;
import quasar.code.labs.dev.exceptions.user.UserException;
import quasar.code.labs.dev.repository.UserIngestRepository;
import quasar.code.labs.dev.repository.UserRepository;

import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@ApplicationScoped
public class KafkaConsumerService {

    private static final Logger logger =
            Logger.getLogger(KafkaConsumerService.class.getName());

    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private final UserIngestRepository userIngestRepository;

    @Inject
    public KafkaConsumerService(
            UserIngestRepository userIngestRepository) {
        this.userIngestRepository = userIngestRepository;
    }

    @Retry(maxRetries = 5, delay = 200,
            retryOn = {IOException.class,
                    TimeoutException.class,
                    SQLException.class},
            abortOn = {UserException.class,
                    ConstraintViolationException.class,
                    IllegalArgumentException.class})
    @Incoming("authenticate-processed")
    @WithTransaction
    public Uni<Void> consume(List<User> eventos) {
        return userIngestRepository.upsertBatch(eventos).
                onFailure().invoke(e ->
                        logger.warning("Error procesando batch: " + e.getMessage())
                );
    }
}