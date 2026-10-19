package com.customcharge.persistencelayer.security.entra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches OIDC discovery documents in memory with TTL per issuer URL.
 */
@Service
@ConditionalOnEntraOidcSecurityEnabled
public class OidcDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(OidcDiscoveryService.class);

    private final OidcDiscoveryClient discoveryClient;
    private final EntraOidcSecurityProperties properties;
    private final Clock clock;
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public OidcDiscoveryService(
            OidcDiscoveryClient discoveryClient,
            EntraOidcSecurityProperties properties,
            @Qualifier("entraSecurityClock") Clock clock) {
        this.discoveryClient = discoveryClient;
        this.properties = properties;
        this.clock = clock;
    }

    public OidcDiscoveryDocument getDiscovery(String issuerUrl) {
        String key = Objects.requireNonNull(issuerUrl, "issuerUrl").trim();
        Instant now = clock.instant();
        CacheEntry entry = cache.get(key);
        if (entry != null && entry.expiresAt.isAfter(now)) {
            log.debug("Entra OIDC discovery cache hit issuerUrl={}", key);
            return entry.document;
        }
        log.debug("Entra OIDC discovery cache miss issuerUrl={}", key);
        OidcDiscoveryDocument doc = discoveryClient.fetchDiscovery(key);
        Instant expires = now.plus(properties.getDiscoveryCacheTtl());
        cache.put(key, new CacheEntry(doc, expires));
        log.debug("Cached OIDC discovery for issuer {} until {}", key, expires);
        return doc;
    }

    public void invalidate(String issuerUrl) {
        if (issuerUrl != null) {
            cache.remove(issuerUrl.trim());
        }
    }

    private record CacheEntry(OidcDiscoveryDocument document, Instant expiresAt) {
    }
}
