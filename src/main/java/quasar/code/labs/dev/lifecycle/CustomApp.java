package quasar.code.labs.dev.lifecycle;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class CustomApp implements QuarkusApplication {
    private final Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * @param args
     * @return
     * @throws Exception
     */
    @Override
    public int run(String... args) throws Exception {
        logger.info("Running main method from CustomApp");
        Quarkus.waitForExit();
        return 0;
    }
}
