package com.customcharge.persistencelayer.security.entra;

import java.util.List;
import java.util.Optional;

/**
 * Normalized identity extracted from Entra-issued access tokens.
 */
public record JwtUserIdentity(
        String subject,
        String preferredUsername,
        String email,
        String oid,
        String upn,
        String primaryLookupKey,
        List<String> tokenRoles,
        List<String> tokenGroups
) {

    public JwtUserIdentity {
        if (primaryLookupKey == null || primaryLookupKey.isBlank()) {
            throw new IllegalArgumentException("primaryLookupKey must be derived from token claims");
        }
        tokenRoles = tokenRoles == null ? List.of() : List.copyOf(tokenRoles);
        tokenGroups = tokenGroups == null ? List.of() : List.copyOf(tokenGroups);
    }

    public static Optional<String> firstPresent(String... values) {
        if (values == null) {
            return Optional.empty();
        }
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return Optional.of(v.trim());
            }
        }
        return Optional.empty();
    }
}
