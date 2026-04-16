package quasar.code.labs.dev.exceptions.user;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Provider
@ApplicationScoped
@Priority(1)
@RegisterForReflection
public class RateLimitInterceptor implements ContainerRequestFilter {

    private static final int MAX_REQUESTS = 400000;
    private static final long TIME_WINDOW_SECONDS = 1;

    // Caché con auto-limpieza
    private final Cache<String, RequestInfo> requestCounts = Caffeine.newBuilder()
            .maximumSize(5000) // Nunca habrá más de 5000 registros en RAM
            .expireAfterWrite(Duration.ofSeconds(TIME_WINDOW_SECONDS + 1)) // Se borra solo después de que expire la ventana
            .build();

    @Context
    HttpHeaders headers;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String userKey = getUserKey();

        // Obtenemos o creamos la info de forma atómica
        RequestInfo requestInfo = requestCounts.get(userKey, k -> new RequestInfo());

        if (requestInfo.isRateLimited(MAX_REQUESTS, TIME_WINDOW_SECONDS * 1000)) {
            requestContext.abortWith(Response.status(Response.Status.TOO_MANY_REQUESTS)
                    .entity("Has alcanzado el límite de peticiones. Intenta más tarde.")
                    .build());
        } else {
            requestInfo.increment();
        }
    }

    private String getUserKey() {
        Map<String, Cookie> cookies = headers.getCookies();
        Cookie tokenCookie = cookies.get("token");

        if (tokenCookie != null) {
            return tokenCookie.getValue();
        }

        String forwardedFor = headers.getHeaderString("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            return forwardedFor.split(",")[0].trim();
        }

        return "anonymous";
    }

    // Clase interna simplificada
    static class RequestInfo {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long startTime = Instant.now().toEpochMilli();

        public boolean isRateLimited(int max, long windowMs) {
            long now = Instant.now().toEpochMilli();
            synchronized (this) {
                if (now - startTime > windowMs) {
                    count.set(0);
                    startTime = now;
                }
                return count.get() >= max;
            }
        }

        public void increment() {
            count.incrementAndGet();
        }
    }
}