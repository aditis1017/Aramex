package com.customcharge.persistencelayer.security.entra;

import com.nimbusds.jwt.JWTClaimsSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Reads common Entra / OIDC claim shapes ({@code roles}, {@code groups}) from parsed JWTs.
 */
final class JwtClaimReaders {

    private JwtClaimReaders() {
    }

    static List<String> stringListClaim(JWTClaimsSet claims, String name) {
        if (claims == null || name == null || name.isBlank()) {
            return List.of();
        }
        try {
            Object raw = claims.getClaim(name);
            return normalizeClaimList(raw);
        } catch (Exception ex) {
            return List.of();
        }
    }

    private static List<String> normalizeClaimList(Object raw) {
        if (raw == null) {
            return List.of();
        }
        if (raw instanceof String single) {
            String trimmed = single.trim();
            return trimmed.isEmpty() ? List.of() : List.of(trimmed);
        }
        if (raw instanceof Collection<?> collection) {
            List<String> out = new ArrayList<>();
            for (Object item : collection) {
                if (item == null) {
                    continue;
                }
                String value = item.toString().trim();
                if (!value.isEmpty()) {
                    out.add(value);
                }
            }
            return List.copyOf(out);
        }
        if (raw.getClass().isArray()) {
            int len = java.lang.reflect.Array.getLength(raw);
            List<String> out = new ArrayList<>(len);
            for (int i = 0; i < len; i++) {
                Object item = java.lang.reflect.Array.get(raw, i);
                if (item != null) {
                    String value = item.toString().trim();
                    if (!value.isEmpty()) {
                        out.add(value);
                    }
                }
            }
            return List.copyOf(out);
        }
        String value = raw.toString().trim();
        return value.isEmpty() ? List.of() : List.of(value);
    }
}
