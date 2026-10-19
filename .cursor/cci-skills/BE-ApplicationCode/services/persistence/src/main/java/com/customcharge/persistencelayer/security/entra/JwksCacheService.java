package com.customcharge.persistencelayer.security.entra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Objects;

/**
 * JWKS cache on the same Redis as {@link com.customcharge.persistencelayer.config.redis.RedisConfiguration}
 * (key {@link EntraRedisConfigurationKeys#JWKS_CACHE}), with network refresh, TTL, and invalidation on rotation.
 */
@Service
@ConditionalOnEntraOidcSecurityEnabled
public class JwksCacheService {

    private static final Logger log = LoggerFactory.getLogger(JwksCacheService.class);

    private final StringRedisTemplate redis;
    private final WebClient webClient;
    private final EntraOidcSecurityProperties properties;

    public JwksCacheService(
            StringRedisTemplate redis,
            @Qualifier("entraSecurityWebClient") WebClient entraSecurityWebClient,
            EntraOidcSecurityProperties properties) {
        this.redis = redis;
        this.properties = properties;
        this.webClient = entraSecurityWebClient;
    }

    /**
     * Returns raw JWKS JSON, using Redis when possible.
     */
    public String getJwksJson(String jwksUri) {
        Objects.requireNonNull(jwksUri, "jwksUri");
        try {
            String cached = redis.opsForValue().get(EntraRedisConfigurationKeys.JWKS_CACHE);
            if (cached != null && !cached.isBlank()) {
                log.debug("Entra JWKS cache hit key={}", EntraRedisConfigurationKeys.JWKS_CACHE);
                return cached;
            }
        } catch (DataAccessException ex) {
            log.warn("Redis unavailable while reading JWKS cache; falling back to direct HTTP fetch", ex);
        }
        log.debug("Entra JWKS cache miss jwksUri={}", jwksUri);
        return fetchAndCache(jwksUri);
    }

    /**
     * Drops cached JWKS so the next lookup refetches keys (e.g. unknown {@code kid}).
     */
    public void invalidate() {
        log.debug("Entra JWKS cache invalidate key={}", EntraRedisConfigurationKeys.JWKS_CACHE);
        try {
            redis.delete(EntraRedisConfigurationKeys.JWKS_CACHE);
        } catch (DataAccessException ex) {
            log.warn("Redis unavailable while invalidating JWKS cache", ex);
        }
    }

    private synchronized String fetchAndCache(String jwksUri) {
        try {
            String cached = redis.opsForValue().get(EntraRedisConfigurationKeys.JWKS_CACHE);
            if (cached != null && !cached.isBlank()) {
                return cached;
            }
        } catch (DataAccessException ex) {
            log.warn("Redis unavailable before JWKS refresh; continuing with HTTP only", ex);
        }

        String json = ResilientExecutor.executeWithRetry(
                "JWKS GET " + jwksUri,
                properties.getRetryMaxAttempts(),
                properties.getRetryBackoff(),
                () -> webClient.get()
                        .uri(jwksUri)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block(properties.getHttpReadTimeout()));
        if (json == null || json.isBlank()) {
            throw new IllegalStateException("Empty JWKS response");
        }
        log.debug("Entra JWKS HTTP OK jwksUri={} bytes={}", jwksUri, json.length());
        try {
            Duration ttl = properties.getJwksRedisTtl();
            redis.opsForValue().set(EntraRedisConfigurationKeys.JWKS_CACHE, json, ttl);
            log.debug("Entra JWKS cache stored ttl={}", ttl);
        } catch (DataAccessException ex) {
            log.warn("Redis unavailable while writing JWKS cache; returning freshly fetched keys only for this process", ex);
        }
        return json;
    }

    public String forceRefresh(String jwksUri) {
        invalidate();
        return fetchAndCache(jwksUri);
    }
}
