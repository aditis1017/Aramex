package com.customcharge.persistencelayer.security.entra;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Loads Entra tenant/client/issuer from the application Redis ({@code mvp.persistence.redis.*}) using the shared
 * {@link org.springframework.data.redis.core.StringRedisTemplate} from {@link com.customcharge.persistencelayer.config.redis.RedisConfiguration}.
 */
@Service
@ConditionalOnEntraOidcSecurityEnabled
public class EntraRedisConfigurationService {

    private static final Logger log = LoggerFactory.getLogger(EntraRedisConfigurationService.class);

    private final StringRedisTemplate redis;
    private final EntraOidcSecurityProperties properties;
    private final AtomicReference<EntraRuntimeSecrets> cachedSecrets = new AtomicReference<>();

    public EntraRedisConfigurationService(StringRedisTemplate redis, EntraOidcSecurityProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    @PostConstruct
    public void warmUpFromRedis() {
        try {
            loadFromRedis(true);
        } catch (Exception ex) {
            log.warn("Initial Entra Redis configuration load did not complete; scheduled refresh will retry", ex);
        }
    }

    /**
     * Scheduled refresh so runtime changes in Redis are picked up without restart.
     */
    @Scheduled(fixedDelayString = "${mvp.persistence.security.entra.entra-config-refresh-interval-ms:60000}")
    public void scheduledRefresh() {
        try {
            loadFromRedis(true);
        } catch (Exception ex) {
            log.warn("Scheduled Entra Redis config refresh failed; retaining last known configuration if present", ex);
        }
    }

    /**
     * Returns cached configuration when available; otherwise attempts Redis read.
     */
    public Optional<EntraRuntimeSecrets> getCurrentSecrets() {
        EntraRuntimeSecrets existing = cachedSecrets.get();
        if (existing != null) {
            return Optional.of(existing);
        }
        return loadFromRedis(true);
    }

    /**
     * Forces a Redis read and updates the in-memory snapshot on success.
     */
    public Optional<EntraRuntimeSecrets> refreshFromRedis() {
        return loadFromRedis(true);
    }

    public EntraRuntimeSecrets requireCurrentSecrets() {
        log.debug("Entra Redis config: resolving current Entra secrets");
        EntraRuntimeSecrets secrets = getCurrentSecrets()
                .or(() -> refreshFromRedis())
                .orElseThrow(() -> new IllegalStateException(
                        "Entra configuration is not available in Redis (keys: "
                                + EntraRedisConfigurationKeys.TENANT_ID + ", "
                                + EntraRedisConfigurationKeys.CLIENT_ID + ", "
                                + EntraRedisConfigurationKeys.ISSUER_URL + ")"));
        log.debug(
                "Entra Redis config: resolved tenantId={} clientId={} issuerUrl={}",
                secrets.tenantId(),
                secrets.clientId(),
                secrets.issuerUrl());
        return secrets;
    }

    private Optional<EntraRuntimeSecrets> loadFromRedis(boolean allowStaleFallback) {
        try {
            Optional<EntraRuntimeSecrets> loaded = ResilientExecutor.executeWithRetry(
                    "Redis Entra configuration read",
                    properties.getRetryMaxAttempts(),
                    properties.getRetryBackoff(),
                    this::readSecretsFromRedis);
            loaded.ifPresent(cachedSecrets::set);
            if (loaded.isPresent()) {
                return loaded;
            }
            return allowStaleFallback ? Optional.ofNullable(cachedSecrets.get()) : Optional.empty();
        } catch (IllegalStateException | DataAccessException ex) {
            log.warn("Could not load Entra configuration from Redis", ex);
            return Optional.ofNullable(cachedSecrets.get());
        }
    }

    private Optional<EntraRuntimeSecrets> readSecretsFromRedis() {
        String tenant = trimToNull(redis.opsForValue().get(EntraRedisConfigurationKeys.TENANT_ID));
        String client = trimToNull(redis.opsForValue().get(EntraRedisConfigurationKeys.CLIENT_ID));
        String issuer = trimToNull(redis.opsForValue().get(EntraRedisConfigurationKeys.ISSUER_URL));
        if (tenant == null || client == null || issuer == null) {
            log.warn(
                    "Incomplete Entra Redis configuration ({} / {} / {}): tenantIdPresent={} clientIdPresent={} issuerUrlPresent={}",
                    EntraRedisConfigurationKeys.TENANT_ID,
                    EntraRedisConfigurationKeys.CLIENT_ID,
                    EntraRedisConfigurationKeys.ISSUER_URL,
                    tenant != null,
                    client != null,
                    issuer != null);
            return Optional.empty();
        }
        return Optional.of(new EntraRuntimeSecrets(tenant, client, issuer));
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }
}
