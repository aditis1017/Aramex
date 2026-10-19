package com.customcharge.persistencelayer.security.entra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * After the application is up, pre-fetches OIDC discovery and JWKS into Redis/in-memory caches so the first real
 * request does not pay the full cold-start cost and operators can confirm {@code entra:jwks} is written when Redis
 * IdP keys are present.
 */
@Component
@ConditionalOnEntraOidcSecurityEnabled
@Order(Ordered.LOWEST_PRECEDENCE)
public class EntraOidcStartupWarmup implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EntraOidcStartupWarmup.class);

    private final EntraRedisConfigurationService secretsService;
    private final OidcDiscoveryService discoveryService;
    private final JwksCacheService jwksCacheService;

    public EntraOidcStartupWarmup(
            EntraRedisConfigurationService secretsService,
            OidcDiscoveryService discoveryService,
            JwksCacheService jwksCacheService) {
        this.secretsService = secretsService;
        this.discoveryService = discoveryService;
        this.jwksCacheService = jwksCacheService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            Optional<EntraRuntimeSecrets> secrets = secretsService.getCurrentSecrets();
            if (secrets.isEmpty()) {
                log.warn(
                        "Entra OIDC startup warm-up skipped: Redis IdP keys are missing or incomplete ({} / {} / {}). "
                                + "Seed Redis before traffic; JWKS will load on first successful auth once keys exist.",
                        EntraRedisConfigurationKeys.TENANT_ID,
                        EntraRedisConfigurationKeys.CLIENT_ID,
                        EntraRedisConfigurationKeys.ISSUER_URL);
                return;
            }
            EntraRuntimeSecrets s = secrets.get();
            OidcDiscoveryDocument discovery = discoveryService.getDiscovery(s.normalizedIssuer());
            String jwksUri = discovery.jwksUri().toString();
            jwksCacheService.getJwksJson(jwksUri);
            log.info("Entra OIDC startup warm-up completed (JWKS loaded from {})", jwksUri);
        } catch (Exception ex) {
            log.warn("Entra OIDC startup warm-up failed (non-fatal); first authenticated request will retry: {}", ex.getMessage());
        }
    }
}
