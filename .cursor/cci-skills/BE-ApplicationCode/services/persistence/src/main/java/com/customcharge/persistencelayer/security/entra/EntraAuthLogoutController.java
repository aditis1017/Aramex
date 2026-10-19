package com.customcharge.persistencelayer.security.entra;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.JWTClaimsSet;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;

/**
 * Revokes the caller's Bearer token in Redis (until JWT {@code exp}). Public route; requires a valid token body.
 */
@RestController
@RequestMapping("/api/auth")
@ConditionalOnEntraOidcSecurityEnabled
public class EntraAuthLogoutController {

    private final EntraRedisConfigurationService secretsService;
    private final JwtValidationService jwtValidationService;
    private final JwtTokenRevocationService tokenRevocationService;

    public EntraAuthLogoutController(
            EntraRedisConfigurationService secretsService,
            JwtValidationService jwtValidationService,
            JwtTokenRevocationService tokenRevocationService) {
        this.secretsService = secretsService;
        this.jwtValidationService = jwtValidationService;
        this.tokenRevocationService = tokenRevocationService;
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        String token = extractBearerToken(authorization);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            EntraRuntimeSecrets secrets = secretsService.requireCurrentSecrets();
            JWTClaimsSet claims = jwtValidationService.validateAndParseClaims(token, secrets);
            tokenRevocationService.revoke(token, claims);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        } catch (ParseException | JOSEException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    private static String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        String token = authorization.substring(7).trim();
        return token.isEmpty() ? null : token;
    }
}
