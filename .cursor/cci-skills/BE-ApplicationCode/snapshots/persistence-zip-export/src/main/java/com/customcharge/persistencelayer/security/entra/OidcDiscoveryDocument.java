package com.customcharge.persistencelayer.security.entra;

import java.net.URI;
import java.util.Collections;
import java.util.List;

/**
 * Subset of fields from OpenID Provider Metadata used for JWT validation.
 */
public record OidcDiscoveryDocument(
        String issuer,
        URI jwksUri,
        List<String> idTokenSigningAlgValuesSupported
) {

    public OidcDiscoveryDocument {
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException("issuer must be present in discovery document");
        }
        if (jwksUri == null) {
            throw new IllegalArgumentException("jwks_uri must be present in discovery document");
        }
        idTokenSigningAlgValuesSupported = idTokenSigningAlgValuesSupported == null
                ? List.of()
                : List.copyOf(idTokenSigningAlgValuesSupported);
    }

    public List<String> signingAlgorithmsOrDefault() {
        if (idTokenSigningAlgValuesSupported.isEmpty()) {
            return List.of("RS256", "RS384", "RS512", "ES256", "ES384", "ES512");
        }
        return idTokenSigningAlgValuesSupported;
    }

    public static OidcDiscoveryDocument fromIssuerAndJwks(String issuer, URI jwksUri) {
        return new OidcDiscoveryDocument(issuer, jwksUri, Collections.emptyList());
    }
}
