package com.customcharge.persistencelayer.security.entra;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates JWTs using JWKS-backed signature verification and standard OIDC claim checks.
 */
@Service
@ConditionalOnEntraOidcSecurityEnabled
public class JwtValidationService {

    private static final Logger log = LoggerFactory.getLogger(JwtValidationService.class);

    private static final long DEFAULT_CLOCK_SKEW_SECONDS = 60;

    private final JwksCacheService jwksCacheService;
    private final OidcDiscoveryService discoveryService;
    private final EntraOidcSecurityProperties properties;
    private final Clock clock;

    public JwtValidationService(
            JwksCacheService jwksCacheService,
            OidcDiscoveryService discoveryService,
            EntraOidcSecurityProperties properties,
            @Qualifier("entraSecurityClock") Clock clock) {
        this.jwksCacheService = jwksCacheService;
        this.discoveryService = discoveryService;
        this.properties = properties;
        this.clock = clock;
    }

    public JWTClaimsSet validateAndParseClaims(String bearerToken, EntraRuntimeSecrets secrets) throws ParseException, JOSEException {
        log.debug("Entra JWT validation: JWT_PARSE {}", EntraJwtLogSupport.tokenHint(bearerToken));
        SignedJWT signedJwt = SignedJWT.parse(bearerToken);
        log.debug("Entra JWT validation: OIDC_DISCOVERY_START issuerBase={}", secrets.normalizedIssuer());
        OidcDiscoveryDocument discovery = discoveryService.getDiscovery(secrets.normalizedIssuer());
        log.debug("Entra JWT validation: OIDC_DISCOVERY_OK issuer={} jwksUri={}", discovery.issuer(), discovery.jwksUri());
        if (!discovery.issuer().equals(secrets.normalizedIssuer()) && !discovery.issuer().equals(secrets.issuerUrl().trim())) {
            log.debug("Discovery issuer {} differs from configured issuer base {}", discovery.issuer(), secrets.issuerUrl());
        }

        log.debug("Entra JWT validation: JWKS_FETCH jwksUri={}", discovery.jwksUri());
        String jwksJson = jwksCacheService.getJwksJson(discovery.jwksUri().toString());
        JWTClaimsSet claims = tryVerify(signedJwt, jwksJson, discovery.issuer(), secrets);
        if (claims != null) {
            log.debug("Entra JWT validation: JWT_SIGNATURE_OK kid={}", signedJwt.getHeader().getKeyID());
            log.debug("Entra JWT validation: JWT_CLAIMS_OK audience check passed for clientId={}", secrets.clientId());
            return claims;
        }

        log.debug("Entra JWT validation: JWKS_RETRY (invalidate cache and retry once)");
        jwksJson = jwksCacheService.forceRefresh(discovery.jwksUri().toString());
        claims = tryVerify(signedJwt, jwksJson, discovery.issuer(), secrets);
        if (claims == null) {
            log.debug("Entra JWT validation: JWT_SIGNATURE_FAILED after JWKS refresh");
            throw new JOSEException("JWT signature verification failed after JWKS refresh");
        }
        log.debug("Entra JWT validation: JWT_SIGNATURE_OK after JWKS refresh kid={}", signedJwt.getHeader().getKeyID());
        return claims;
    }

    private JWTClaimsSet tryVerify(
            SignedJWT signedJwt,
            String jwksJson,
            String expectedIssuer,
            EntraRuntimeSecrets secrets) throws ParseException, JOSEException {

        JWKSet jwkSet = JWKSet.parse(jwksJson);
        String kid = signedJwt.getHeader().getKeyID();
        List<JWK> candidates = selectKeys(jwkSet, kid);

        for (JWK jwk : candidates) {
            JWSVerifier verifier = buildVerifier(jwk);
            if (verifier == null) {
                continue;
            }
            boolean signatureOk;
            try {
                signatureOk = signedJwt.verify(verifier);
            } catch (JOSEException ex) {
                log.trace("JWT signature verification failed for kid={}: {}", kid, ex.toString());
                continue;
            }
            if (signatureOk) {
                JWTClaimsSet claims = signedJwt.getJWTClaimsSet();
                assertClaims(claims, expectedIssuer, secrets);
                return claims;
            }
        }
        return null;
    }

    private List<JWK> selectKeys(JWKSet jwkSet, String kid) throws ParseException {
        List<JWK> keys = new ArrayList<>();
        if (kid != null && !kid.isBlank()) {
            JWKMatcher matcher = new JWKMatcher.Builder()
                    .keyID(kid)
                    .publicOnly(true)
                    .build();
            keys.addAll(new JWKSelector(matcher).select(jwkSet));
        }
        if (keys.isEmpty()) {
            for (JWK jwk : jwkSet.getKeys()) {
                if (!Boolean.TRUE.equals(jwk.isPrivate())) {
                    keys.add(jwk);
                }
            }
        }
        return keys;
    }

    private JWSVerifier buildVerifier(JWK jwk) throws JOSEException {
        if (jwk instanceof RSAKey rsaKey) {
            RSAPublicKey pub = rsaKey.toRSAPublicKey();
            return new RSASSAVerifier(pub);
        }
        if (jwk instanceof ECKey ecKey) {
            ECPublicKey pub = ecKey.toECPublicKey();
            return new ECDSAVerifier(pub);
        }
        log.debug("Unsupported JWK type for JWT verification: {}", jwk.getKeyType());
        return null;
    }

    private void assertClaims(JWTClaimsSet claims, String discoveryIssuer, EntraRuntimeSecrets secrets) throws JOSEException {
        String issuer = claims.getIssuer();
        if (!isAcceptedIssuer(issuer, discoveryIssuer, secrets)) {
            throw new JOSEException(
                    "Invalid issuer claim: token iss="
                            + issuer
                            + " (expected discovery iss="
                            + discoveryIssuer
                            + " or tenant sts.windows.net / login.microsoftonline.com variant)");
        }

        Set<String> acceptedAudiences = buildAcceptedAudiences(secrets);

        List<String> tokenAudiences = claims.getAudience();
        if (tokenAudiences == null || tokenAudiences.isEmpty()) {
            throw new JOSEException("Missing audience claim");
        }
        boolean audOk = tokenAudiences.stream().anyMatch(acceptedAudiences::contains);
        if (!audOk) {
            throw new JOSEException("Invalid audience claim");
        }

        Instant now = clock.instant();
        Instant exp = claims.getExpirationTime() != null
                ? claims.getExpirationTime().toInstant()
                : null;
        Instant nbf = claims.getNotBeforeTime() != null
                ? claims.getNotBeforeTime().toInstant()
                : null;

        if (exp == null) {
            throw new JOSEException("Missing exp claim");
        }
        if (now.isAfter(exp.plus(DEFAULT_CLOCK_SKEW_SECONDS, ChronoUnit.SECONDS))) {
            throw new EntraAccessTokenExpiredException();
        }
        if (nbf != null && now.plus(DEFAULT_CLOCK_SKEW_SECONDS, ChronoUnit.SECONDS).isBefore(nbf)) {
            throw new JOSEException("Token not yet valid");
        }
    }

    /**
     * API access tokens often use {@code api://{appId}} as {@code aud}; Redis may store the bare app id.
     */
    private Set<String> buildAcceptedAudiences(EntraRuntimeSecrets secrets) {
        Set<String> accepted = new HashSet<>();
        String clientId = secrets.clientId().trim();
        accepted.add(clientId);
        if (!clientId.startsWith("api://")) {
            accepted.add("api://" + clientId);
        } else {
            accepted.add(clientId.substring("api://".length()));
        }
        if (properties.getAdditionalAcceptedAudiences() != null) {
            accepted.addAll(properties.getAdditionalAcceptedAudiences());
        }
        return accepted;
    }

    /**
     * Microsoft access tokens may use v1 {@code sts.windows.net} or v2 {@code login.microsoftonline.com} issuers
     * for the same tenant; OIDC discovery returns v2.0 while many API tokens still carry v1 {@code iss}.
     */
    private boolean isAcceptedIssuer(String tokenIssuer, String discoveryIssuer, EntraRuntimeSecrets secrets) {
        if (tokenIssuer == null || tokenIssuer.isBlank()) {
            return false;
        }
        String normalizedToken = normalizeIssuer(tokenIssuer);
        if (normalizeIssuer(discoveryIssuer).equals(normalizedToken)) {
            return true;
        }
        String tenantId = secrets.tenantId();
        return knownIssuersForTenant(tenantId).stream()
                .map(JwtValidationService::normalizeIssuer)
                .anyMatch(normalizedToken::equals);
    }

    static Set<String> knownIssuersForTenant(String tenantId) {
        return Set.of(
                "https://login.microsoftonline.com/" + tenantId + "/v2.0",
                "https://login.microsoftonline.com/" + tenantId,
                "https://sts.windows.net/" + tenantId + "/",
                "https://sts.windows.net/" + tenantId);
    }

    private static String normalizeIssuer(String issuer) {
        return issuer.trim().replaceAll("/+$", "");
    }
}
