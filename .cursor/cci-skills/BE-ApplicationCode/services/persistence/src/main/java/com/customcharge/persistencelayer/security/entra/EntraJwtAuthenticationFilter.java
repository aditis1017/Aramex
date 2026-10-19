package com.customcharge.persistencelayer.security.entra;

import com.customcharge.persistencelayer.entities.User;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.text.ParseException;

/**
 * Validates Bearer JWTs for non-public routes and populates the {@link org.springframework.security.core.context.SecurityContext}.
 * Registered only on the Spring Security filter chain (not as a servlet {@code @Component} filter).
 */
@ConditionalOnEntraOidcSecurityEnabled
public class EntraJwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(EntraJwtAuthenticationFilter.class);

    private final RequestMatcher publicPaths;
    private final EntraRedisConfigurationService secretsService;
    private final JwtValidationService jwtValidationService;
    private final JwtUserIdentityExtractor identityExtractor;
    private final EntraUserValidationService userValidationService;
    private final JwtTokenRevocationService tokenRevocationService;
    private final EntraSecurityResponseWriter responseWriter;
    private final ApplicationUserAuthoritiesService applicationUserAuthoritiesService;

    public EntraJwtAuthenticationFilter(
            EntraOidcSecurityProperties properties,
            EntraRedisConfigurationService secretsService,
            JwtValidationService jwtValidationService,
            JwtUserIdentityExtractor identityExtractor,
            EntraUserValidationService userValidationService,
            JwtTokenRevocationService tokenRevocationService,
            EntraSecurityResponseWriter responseWriter,
            ApplicationUserAuthoritiesService applicationUserAuthoritiesService) {
        this.publicPaths = EntraPublicPathMatchers.fromPatterns(properties.getPublicPaths());
        this.secretsService = secretsService;
        this.jwtValidationService = jwtValidationService;
        this.identityExtractor = identityExtractor;
        this.userValidationService = userValidationService;
        this.tokenRevocationService = tokenRevocationService;
        this.responseWriter = responseWriter;
        this.applicationUserAuthoritiesService = applicationUserAuthoritiesService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (publicPaths.matches(request)) {
            log.debug("Entra JWT filter skipped public path method={} uri={}", request.getMethod(), request.getRequestURI());
            return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        log.debug("Entra JWT request enter method={} uri={}", request.getMethod(), request.getRequestURI());
        try {
            SecurityContextHolder.clearContext();

            String header = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, 7)) {
                log.debug("Entra JWT: TOKEN_MISSING (Authorization absent or not Bearer)");
                responseWriter.write(response, HttpStatus.UNAUTHORIZED, "TOKEN_MISSING", "Authorization Bearer token is required");
                return;
            }

            String token = header.substring(7).trim();
            if (token.isEmpty()) {
                log.debug("Entra JWT: TOKEN_MISSING (Bearer prefix present but token empty)");
                responseWriter.write(response, HttpStatus.UNAUTHORIZED, "TOKEN_MISSING", "Authorization Bearer token is required");
                return;
            }

            log.debug("Entra JWT: TOKEN_RECEIVED {}", EntraJwtLogSupport.tokenHint(token));

            try {
                EntraRuntimeSecrets secrets = secretsService.requireCurrentSecrets();

                log.debug("Entra JWT: JWT_VALIDATE_START");
                JWTClaimsSet claims = jwtValidationService.validateAndParseClaims(token, secrets);
                log.debug("Entra JWT: JWT_VALIDATE_OK subject={} issuer={}", claims.getSubject(), claims.getIssuer());

                if (tokenRevocationService.isRevoked(token, claims)) {
                    log.debug("Entra JWT: TOKEN_REVOKED {}", EntraJwtLogSupport.tokenHint(token));
                    responseWriter.write(
                            response,
                            HttpStatus.UNAUTHORIZED,
                            "TOKEN_REVOKED",
                            "Access token has been revoked");
                    return;
                }

                JwtUserIdentity identity = identityExtractor.extract(claims);
                log.debug(
                        "Entra JWT: IDENTITY_EXTRACTED primaryLookupKey={} tokenRoles={} tokenGroups={}",
                        identity.primaryLookupKey(),
                        identity.tokenRoles(),
                        identity.tokenGroups());

                User user = userValidationService.loadAndValidate(identity);
                log.debug(
                        "Entra JWT: USER_AUTHORIZED userId={} email={} role={}",
                        user.getId(),
                        user.getEmail(),
                        user.getRole() != null ? user.getRole().getCode() : null);

                SecurityContextHolder.getContext().setAuthentication(
                        new EntraUserAuthenticationToken(
                                user, identity, applicationUserAuthoritiesService.resolveAuthorities(user)));
                log.debug("Entra JWT: FILTER_CHAIN_CONTINUE");
                filterChain.doFilter(request, response);
                log.debug("Entra JWT: REQUEST_HANDLED downstream completed");
            } catch (EntraRequestDeniedException ex) {
                log.debug(
                        "Entra JWT: AUTH_DENIED code={} status={} message={}",
                        ex.getErrorCode(),
                        ex.getHttpStatus().value(),
                        ex.getMessage());
                responseWriter.write(response, ex.getHttpStatus(), ex.getErrorCode(), ex.getMessage());
            } catch (IllegalArgumentException ex) {
                log.debug("Entra JWT: IDENTITY_INVALID message={}", ex.getMessage());
                responseWriter.write(response, HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", ex.getMessage());
            } catch (IllegalStateException ex) {
                log.warn("Entra OIDC configuration unavailable: {}", ex.getMessage());
                responseWriter.write(
                        response,
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "ENTRA_CONFIG_UNAVAILABLE",
                        "Identity provider configuration is temporarily unavailable");
            } catch (EntraAccessTokenExpiredException ex) {
                log.debug("Entra JWT: TOKEN_EXPIRED {}", ex.getMessage());
                responseWriter.write(
                        response,
                        HttpStatus.UNAUTHORIZED,
                        EntraAccessTokenExpiredException.ERROR_CODE,
                        EntraAccessTokenExpiredException.USER_FACING_MESSAGE);
            } catch (ParseException | JOSEException ex) {
                log.debug("Entra JWT: JWT_INVALID reason={}", ex.toString());
                responseWriter.write(response, HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "JWT validation failed");
            } catch (Exception ex) {
                log.error("Unexpected error during Entra JWT authentication", ex);
                responseWriter.write(response, HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected authentication error");
            }
        } finally {
            log.debug("Entra JWT: REQUEST_LEAVE");
        }
    }
}
