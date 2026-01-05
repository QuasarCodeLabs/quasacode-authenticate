package quasar.code.labs.dev.utils;

import java.util.concurrent.ConcurrentHashMap;

public class TokenCache {

    private final ConcurrentHashMap<String, Long> usedTokens = new ConcurrentHashMap<>();
    private final long expirationTimeMillis;

    public TokenCache(long expirationTimeMillis) {
        this.expirationTimeMillis = expirationTimeMillis;
    }

    public boolean isTokenUsed(String token) {
        Long timestamp = usedTokens.get(token);
        if (timestamp == null) {
            return false;
        }
        if (System.currentTimeMillis() - timestamp > expirationTimeMillis) {
            usedTokens.remove(token); // Expired token
            return false;
        }
        return true;
    }

    public void markTokenAsUsed(String token) {
        usedTokens.put(token, System.currentTimeMillis());
    }
}