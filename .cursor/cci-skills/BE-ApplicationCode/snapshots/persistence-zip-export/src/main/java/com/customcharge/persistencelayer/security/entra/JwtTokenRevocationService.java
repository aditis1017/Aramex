package com.customcharge.persistencelayer.security.entra;

import com.nimbusds.jwt.JWTClaimsSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Redis denylist for logged-out access tokens ({@code entra:revoked:token:*}, {@code entra:revoked:jti:*}).
 */
@Service
@ConditionalOnEntraOidcSecurityEnabled
public class JwtTokenRevocationService {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenRevocationService.class);

    private static final String REVOKED_VALUE = "1";
    private static final long MIN_TTL_SECONDS = 60;
    private static final long CLOCK_SKEW_SECONDS = 60;

    private final StringRedisTemplate redis;
    private final EntraOidcSecurityProperties properties;
    private final Clock clock;

    public JwtTokenRevocationService(
            StringRedisTemplate redis,
            EntraOidcSecurityProperties properties,
            @Qualifier("entraSecurityClock") Clock clock) {
        this.redis = redis;
        this.properties = properties;
        this.clock = clock;
    }

    public void revoke(String rawToken, JWTClaimsSet claims) {
        if (!properties.isTokenRevocationEnabled()) {
            return;
        }
        Duration ttl = ttlUntilExpiry(claims);
        String digestKey = EntraRedisConfigurationKeys.revokedTokenDigestKey(sha256Hex(rawToken));
        redis.opsForValue().set(digestKey, REVOKED_VALUE, ttl);
        log.debug("Entra token revocation: stored digest key={} ttlSeconds={}", digestKey, ttl.getSeconds());

        String jti = claims.getJWTID();
        if (jti != null && !jti.isBlank()) {
            String jtiKey = EntraRedisConfigurationKeys.revokedJtiKey(jti.trim());
            redis.opsForValue().set(jtiKey, REVOKED_VALUE, ttl);
            log.debug("Entra token revocation: stored jti key={}", jtiKey);
        }
    }

    public boolean isRevoked(String rawToken, JWTClaimsSet claims) {
        if (!properties.isTokenRevocationEnabled()) {
            return false;
        }
        String digestKey = EntraRedisConfigurationKeys.revokedTokenDigestKey(sha256Hex(rawToken));
        if (Boolean.TRUE.equals(redis.hasKey(digestKey))) {
            return true;
        }
        String jti = claims.getJWTID();
        if (jti == null || jti.isBlank()) {
            return false;
        }
        return Boolean.TRUE.equals(redis.hasKey(EntraRedisConfigurationKeys.revokedJtiKey(jti.trim())));
    }

    private Duration ttlUntilExpiry(JWTClaimsSet claims) {
        if (claims.getExpirationTime() == null) {
            return properties.getTokenRevocationDefaultTtl();
        }
        Instant exp = claims.getExpirationTime().toInstant();
        Instant now = clock.instant();
        long seconds = Duration.between(now, exp).getSeconds() + CLOCK_SKEW_SECONDS;
        if (seconds < MIN_TTL_SECONDS) {
            seconds = MIN_TTL_SECONDS;
        }
        return Duration.ofSeconds(seconds);
    }

    static String sha256Hex(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
