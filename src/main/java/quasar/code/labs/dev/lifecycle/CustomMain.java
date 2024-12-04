package quasar.code.labs.dev.lifecycle;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class CustomMain {
    public static void main(String ... args){
        Quarkus.run(CustomApp.class, args);
    }
}
