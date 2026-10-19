package com.customcharge.persistencelayer.security.entra;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Boot configuration for Entra OIDC JWT security (non-secret tuning only; Entra IdP secrets live in Redis).
 * The lifecycle webhook secret is an optional application property for server-to-server deactivation calls.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "mvp.persistence.security.entra")
public class EntraOidcSecurityProperties {

    private boolean enabled = false;

    /**
     * Paths that skip JWT processing (Ant-style). Use servlet paths ({@code /swagger-ui/**}) or
     * full URI paths ({@code /dashboard/swagger-ui/**}) when a context path is configured.
     */
    private List<String> publicPaths = new ArrayList<>(List.of(
            "/actuator/health",
            "/actuator/health/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/swagger-ui/index.html",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/api/auth/logout",
            "/chargesCalculator/health",
            "/dashboard/api/auth/logout",
            "/globalSettings/api/auth/logout",
            "/api/auth/entra/lifecycle/user/deactivate",
            "/dashboard/api/auth/entra/lifecycle/user/deactivate"
    ));

    /**
     * Redis TTL for cached JWKS JSON ({@code entra:jwks}).
     */
    private Duration jwksRedisTtl = Duration.ofHours(1);

    /**
     * In-memory TTL for OIDC discovery document (per issuer URL).
     */
    private Duration discoveryCacheTtl = Duration.ofMinutes(30);

    /**
     * How often Entra tenant/client/issuer values are re-read from Redis (milliseconds).
     */
    private long entraConfigRefreshIntervalMs = 60_000L;

    private Duration httpConnectTimeout = Duration.ofSeconds(5);
    private Duration httpReadTimeout = Duration.ofSeconds(10);

    private int retryMaxAttempts = 3;
    private Duration retryBackoff = Duration.ofMillis(200);

    /**
     * When set, the authenticated user must have this application role code (see {@code roles.code}, e.g. {@code SUPER_ADMIN}).
     */
    private String requiredApplicationRoleName;

    /**
     * Application role code ({@code roles.code}) assigned when JIT-provisioning a user whose token {@code roles}
     * claims do not map to the database ({@code users.role_id} is NOT NULL). Configure per environment
     * (e.g. {@code VIEWER}). When unset, JIT provisioning is denied for unmapped token roles.
     */
    private String jitProvisioningRoleCode;

    /**
     * Optional extra audience values accepted in addition to {@code clientId} from Redis.
     */
    private List<String> additionalAcceptedAudiences = new ArrayList<>();

    /**
     * Shared secret for the dashboard lifecycle webhook ({@code POST /api/auth/entra/lifecycle/user/deactivate}):
     * callers must send the same value
     * in header {@code X-Entra-Lifecycle-Secret}. Leave unset to disable the endpoint (503). Prefer an environment
     * variable or secret manager, not committed YAML.
     */
    private String lifecycleWebhookSecret;

    /** When true, logout adds {@code entra:revoked:*} keys in Redis until JWT {@code exp}. */
    private boolean tokenRevocationEnabled = true;

    /** TTL for revoked entries when the token has no {@code exp} claim. */
    private Duration tokenRevocationDefaultTtl = Duration.ofHours(1);
}
