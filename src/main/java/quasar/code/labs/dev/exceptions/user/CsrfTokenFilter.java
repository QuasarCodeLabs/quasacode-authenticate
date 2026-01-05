/*package com.example.filters;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import io.quarkus.vertx.http.runtime.VertxHttpRecorder;

import java.util.UUID;

@ApplicationScoped
public class CsrfTokenFilter {

    public void registerFilter(@Observes VertxHttpRecorder recorder) {
        recorder.addRoute(routingContext -> {
            HttpServerRequest request = routingContext.request();

            // Generar un token CSRF único
            String csrfToken = UUID.randomUUID().toString();

            // Agregar el token CSRF como una cookie
            routingContext.response()
                    .addCookie(io.vertx.core.http.Cookie.cookie("X-CSRF-Token", csrfToken)
                            .setPath("/")
                            .setDomain("localhost") // Ajusta según tu entorno
                            .setMaxAge(3600) // 1 hora
                            .setHttpOnly(true)
                            .setSecure(true));

            // Continuar con la cadena de filtros
            routingContext.next();
        });
    }
}*/