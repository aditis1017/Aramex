package com.customcharge.persistencelayer.security.entra;

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtValidationServiceTest {

    @Mock
    private JwksCacheService jwksCacheService;

    @Mock
    private OidcDiscoveryService oidcDiscoveryService;

    private JwtValidationService jwtValidationService;

    private RSAKey rsaJwk;
    private String issuer;
    private String clientId;

    @BeforeEach
    void setUp() throws Exception {
        issuer = "https://issuer.example/v2.0";
        clientId = "api-client-id";
        rsaJwk = new RSAKeyGenerator(2048).keyID("kid-1").generate();
        EntraOidcSecurityProperties properties = new EntraOidcSecurityProperties();
        Clock clock = Clock.fixed(Instant.parse("2026-05-14T12:00:00Z"), ZoneOffset.UTC);
        jwtValidationService = new JwtValidationService(jwksCacheService, oidcDiscoveryService, properties, clock);
    }

    @Test
    void validateAndParseClaims_success() throws Exception {
        String jwksJson = new com.nimbusds.jose.jwk.JWKSet(rsaJwk.toPublicJWK()).toJSONObject().toString();
        when(oidcDiscoveryService.getDiscovery(anyString())).thenReturn(
                new OidcDiscoveryDocument(issuer, URI.create("https://issuer.example/jwks"), java.util.List.of("RS256")));
        when(jwksCacheService.getJwksJson(anyString())).thenReturn(jwksJson);

        JWTClaimsSet claimSet = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .audience(clientId)
                .subject("sub-1")
                .expirationTime(Date.from(Instant.parse("2026-05-14T14:00:00Z")))
                .notBeforeTime(Date.from(Instant.parse("2026-05-14T11:00:00Z")))
                .issueTime(Date.from(Instant.parse("2026-05-14T12:00:00Z")))
                .build();

        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaJwk.getKeyID()).build(),
                claimSet);
        jwt.sign(new RSASSASigner(rsaJwk.toRSAPrivateKey()));

        EntraRuntimeSecrets secrets = new EntraRuntimeSecrets("tenant", clientId, issuer);

        JWTClaimsSet parsed = jwtValidationService.validateAndParseClaims(jwt.serialize(), secrets);

        assertThat(parsed.getSubject()).isEqualTo("sub-1");
        assertThat(parsed.getIssuer()).isEqualTo(issuer);
    }

    @Test
    void validateAndParseClaims_acceptsApiPrefixAudienceWhenRedisHasBareGuid() throws Exception {
        String jwksJson = new com.nimbusds.jose.jwk.JWKSet(rsaJwk.toPublicJWK()).toJSONObject().toString();
        when(oidcDiscoveryService.getDiscovery(anyString())).thenReturn(
                new OidcDiscoveryDocument(issuer, URI.create("https://issuer.example/jwks"), java.util.List.of("RS256")));
        when(jwksCacheService.getJwksJson(anyString())).thenReturn(jwksJson);

        String bareGuid = "719881d3-46c0-47d2-acbf-976d01188530";
        JWTClaimsSet claimSet = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .audience("api://" + bareGuid)
                .subject("sub-1")
                .expirationTime(Date.from(Instant.parse("2026-05-14T14:00:00Z")))
                .notBeforeTime(Date.from(Instant.parse("2026-05-14T11:00:00Z")))
                .issueTime(Date.from(Instant.parse("2026-05-14T12:00:00Z")))
                .build();

        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaJwk.getKeyID()).build(),
                claimSet);
        jwt.sign(new RSASSASigner(rsaJwk.toRSAPrivateKey()));

        EntraRuntimeSecrets secrets = new EntraRuntimeSecrets("tenant", bareGuid, issuer);

        JWTClaimsSet parsed = jwtValidationService.validateAndParseClaims(jwt.serialize(), secrets);

        assertThat(parsed.getSubject()).isEqualTo("sub-1");
    }

    @Test
    void validateAndParseClaims_acceptsV1StsIssuerWhenDiscoveryIsV2() throws Exception {
        String tenant = "43aa4ce1-f125-4390-a30c-5375aae87717";
        String discoveryIss = "https://login.microsoftonline.com/" + tenant + "/v2.0";
        String tokenIss = "https://sts.windows.net/" + tenant + "/";
        String jwksJson = new com.nimbusds.jose.jwk.JWKSet(rsaJwk.toPublicJWK()).toJSONObject().toString();
        when(oidcDiscoveryService.getDiscovery(anyString())).thenReturn(
                new OidcDiscoveryDocument(
                        discoveryIss, URI.create("https://issuer.example/jwks"), java.util.List.of("RS256")));
        when(jwksCacheService.getJwksJson(anyString())).thenReturn(jwksJson);

        String bareGuid = "719881d3-46c0-47d2-acbf-976d01188530";
        JWTClaimsSet claimSet = new JWTClaimsSet.Builder()
                .issuer(tokenIss)
                .audience("api://" + bareGuid)
                .subject("sub-v1")
                .expirationTime(Date.from(Instant.parse("2026-05-14T14:00:00Z")))
                .notBeforeTime(Date.from(Instant.parse("2026-05-14T11:00:00Z")))
                .issueTime(Date.from(Instant.parse("2026-05-14T12:00:00Z")))
                .build();

        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaJwk.getKeyID()).build(),
                claimSet);
        jwt.sign(new RSASSASigner(rsaJwk.toRSAPrivateKey()));

        EntraRuntimeSecrets secrets = new EntraRuntimeSecrets(tenant, bareGuid, discoveryIss);

        JWTClaimsSet parsed = jwtValidationService.validateAndParseClaims(jwt.serialize(), secrets);

        assertThat(parsed.getSubject()).isEqualTo("sub-v1");
        assertThat(parsed.getIssuer()).isEqualTo(tokenIss);
    }

    @Test
    void validateAndParseClaims_badAudience() throws Exception {
        String jwksJson = new com.nimbusds.jose.jwk.JWKSet(rsaJwk.toPublicJWK()).toJSONObject().toString();
        when(oidcDiscoveryService.getDiscovery(anyString())).thenReturn(
                new OidcDiscoveryDocument(issuer, URI.create("https://issuer.example/jwks"), java.util.List.of()));
        when(jwksCacheService.getJwksJson(anyString())).thenReturn(jwksJson);

        JWTClaimsSet claimSet = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .audience("wrong-aud")
                .subject("sub-1")
                .expirationTime(Date.from(Instant.parse("2026-05-14T14:00:00Z")))
                .notBeforeTime(Date.from(Instant.parse("2026-05-14T11:00:00Z")))
                .build();

        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaJwk.getKeyID()).build(),
                claimSet);
        jwt.sign(new RSASSASigner(rsaJwk.toRSAPrivateKey()));

        EntraRuntimeSecrets secrets = new EntraRuntimeSecrets("tenant", clientId, issuer);

        assertThatThrownBy(() -> jwtValidationService.validateAndParseClaims(jwt.serialize(), secrets))
                .isInstanceOf(JOSEException.class)
                .hasMessageContaining("Invalid audience");
    }

    @Test
    void validateAndParseClaims_expiredTokenThrowsEntraAccessTokenExpiredException() throws Exception {
        String jwksJson = new com.nimbusds.jose.jwk.JWKSet(rsaJwk.toPublicJWK()).toJSONObject().toString();
        when(oidcDiscoveryService.getDiscovery(anyString())).thenReturn(
                new OidcDiscoveryDocument(issuer, URI.create("https://issuer.example/jwks"), java.util.List.of("RS256")));
        when(jwksCacheService.getJwksJson(anyString())).thenReturn(jwksJson);

        JWTClaimsSet claimSet = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .audience(clientId)
                .subject("sub-expired")
                .expirationTime(Date.from(Instant.parse("2026-05-14T10:00:00Z")))
                .notBeforeTime(Date.from(Instant.parse("2026-05-14T08:00:00Z")))
                .issueTime(Date.from(Instant.parse("2026-05-14T09:00:00Z")))
                .build();

        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaJwk.getKeyID()).build(),
                claimSet);
        jwt.sign(new RSASSASigner(rsaJwk.toRSAPrivateKey()));

        EntraRuntimeSecrets secrets = new EntraRuntimeSecrets("tenant", clientId, issuer);

        assertThatThrownBy(() -> jwtValidationService.validateAndParseClaims(jwt.serialize(), secrets))
                .isInstanceOf(EntraAccessTokenExpiredException.class);
    }
}
