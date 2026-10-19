package com.customcharge.persistencelayer.security.entra;

import java.util.Objects;

/**
 * Entra identity settings loaded from Redis (tenant, client id, issuer base URL).
 */
public record EntraRuntimeSecrets(String tenantId, String clientId, String issuerUrl) {

    public EntraRuntimeSecrets {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(clientId, "clientId");
        Objects.requireNonNull(issuerUrl, "issuerUrl");
        if (tenantId.isBlank() || clientId.isBlank() || issuerUrl.isBlank()) {
            throw new IllegalArgumentException("tenantId, clientId and issuerUrl must be non-blank");
        }
    }

    public String normalizedIssuer() {
        return issuerUrl.trim().replaceAll("/+$", "");
    }
}
