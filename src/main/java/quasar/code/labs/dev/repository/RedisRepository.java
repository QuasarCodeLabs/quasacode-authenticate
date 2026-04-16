package quasar.code.labs.dev.repository;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.keys.ReactiveKeyCommands;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import quasar.code.labs.dev.service.UserService;

import java.util.logging.Logger;

@ApplicationScoped
public class RedisRepository {
    private static final Logger logger = Logger.getLogger(RedisRepository.class.getName());


    private final ReactiveValueCommands<String, String> valueCommands;
    private final ReactiveKeyCommands<String> keyCommands;
    private final ReactiveRedisDataSource reactiveRedisDataSource;

    @Inject
    public RedisRepository(ReactiveRedisDataSource reactiveRedisDataSource) {
        this.valueCommands = reactiveRedisDataSource.value(String.class);
        this.keyCommands = reactiveRedisDataSource.key();
        this.reactiveRedisDataSource = reactiveRedisDataSource;
    }

    // Nota: Para guardar en Redis reactivo, el metodo debe retornar Uni o suscribirse
    public Uni<Void> guardarClaveValor(String clave, String valor) {
        return valueCommands.set(clave, valor);
    }

    public Uni<Void> actualizarValor(String clave, String nuevoValor) {
        return valueCommands.set(clave, nuevoValor);
    }

    public Uni<String> actualizarYObtenerAnterior(String clave, String nuevoValor) {
        return valueCommands.getset(clave, nuevoValor);
    }

    // Versión Imperativa (Bloqueante)
    // Nota: Si realmente necesitas que sea bloqueante, usa RedisDataSource (no el Reactive)
    public String leerClave(String clave) {
        return valueCommands.get(clave).await().indefinitely();
    }

    // Versión Reactiva (Recomendado)
    public Uni<String> leerClaveReactiva(String clave) {
        return valueCommands.get(clave);
        // No necesitas transformar a String manualmente, el genérico ya lo hace.
    }

    // Retorna true si la clave existía y fue eliminada
    public Uni<Boolean> eliminarClave(String clave) {
        return keyCommands.del(clave).map(count -> count > 0);
    }

    // "user:id" es la clave que Redis usará para llevar el contador
    public Uni<Long> generarId() {
        return valueCommands.incr("user:id");
    }

}

