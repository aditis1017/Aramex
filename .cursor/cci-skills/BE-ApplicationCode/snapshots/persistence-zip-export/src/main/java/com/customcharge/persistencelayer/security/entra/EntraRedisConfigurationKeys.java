package com.customcharge.persistencelayer.security.entra;

/**
 * Redis keys for Entra configuration and JWKS cache.
 */
public final class EntraRedisConfigurationKeys {

    public static final String TENANT_ID = "entra.tenant.id";
    public static final String CLIENT_ID = "entra.client.id";
    public static final String ISSUER_URL = "entra.issuer.url";

    public static final String JWKS_CACHE = "entra:jwks";

    private static final String REVOKED_TOKEN_PREFIX = "entra:revoked:token:";
    private static final String REVOKED_JTI_PREFIX = "entra:revoked:jti:";

    public static String revokedTokenDigestKey(String sha256Hex) {
        return REVOKED_TOKEN_PREFIX + sha256Hex;
    }

    public static String revokedJtiKey(String jti) {
        return REVOKED_JTI_PREFIX + jti;
    }

    private EntraRedisConfigurationKeys() {
    }
}
