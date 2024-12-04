package quasar.code.labs.dev.lifecycle;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import java.util.logging.Logger;

@ApplicationScoped
public class AppLifecycleBean {
    private final Logger logger = Logger.getLogger(this.getClass().getName());

    void onStart(@Observes StartupEvent startupEvent){
        logger.info("The application is starting . . . ");
    }

    void onStop(@Observes ShutdownEvent shutdownEvent){
        logger.info("The Application is stopping . . . ");
    }
}