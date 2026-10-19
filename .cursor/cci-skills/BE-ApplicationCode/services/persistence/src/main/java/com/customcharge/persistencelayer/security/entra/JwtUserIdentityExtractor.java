package com.customcharge.persistencelayer.security.entra;

import com.nimbusds.jwt.JWTClaimsSet;
import org.springframework.stereotype.Component;

/**
 * Maps JWT claims to {@link JwtUserIdentity} using Entra / OIDC conventions.
 */
@Component
@ConditionalOnEntraOidcSecurityEnabled
public class JwtUserIdentityExtractor {

    public JwtUserIdentity extract(JWTClaimsSet claims) {
        String sub = claims.getSubject();
        String preferred = stringClaim(claims, "preferred_username");
        String email = stringClaim(claims, "email");
        String oid = stringClaim(claims, "oid");
        String upn = stringClaim(claims, "upn");

        String primary = JwtUserIdentity.firstPresent(email, preferred, upn)
                .orElseGet(() -> sub != null ? sub.trim() : "");

        if (primary.isBlank()) {
            throw new IllegalArgumentException("Token does not contain a usable user identifier claim");
        }

        return new JwtUserIdentity(
                sub,
                preferred,
                email,
                oid,
                upn,
                primary,
                JwtClaimReaders.stringListClaim(claims, "roles"),
                JwtClaimReaders.stringListClaim(claims, "groups"));
    }

    private static String stringClaim(JWTClaimsSet claims, String name) {
        try {
            String v = claims.getStringClaim(name);
            return v == null ? null : v.trim();
        } catch (Exception ex) {
            return null;
        }
    }
}
