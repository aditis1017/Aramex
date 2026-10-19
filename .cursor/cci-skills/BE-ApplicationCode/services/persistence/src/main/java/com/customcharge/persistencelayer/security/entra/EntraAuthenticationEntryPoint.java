package com.customcharge.persistencelayer.security.entra;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * JSON {@link AuthenticationEntryPoint} aligned with {@link com.customcharge.persistencelayer.dto.response.ApiResponse}.
 */
@Component
@ConditionalOnEntraOidcSecurityEnabled
public class EntraAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger log = LoggerFactory.getLogger(EntraAuthenticationEntryPoint.class);

    private final EntraSecurityResponseWriter responseWriter;

    public EntraAuthenticationEntryPoint(EntraSecurityResponseWriter responseWriter) {
        this.responseWriter = responseWriter;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        log.debug(
                "Entra security entry point method={} uri={} message={}",
                request.getMethod(),
                request.getRequestURI(),
                authException != null ? authException.getMessage() : "unauthenticated");
        responseWriter.write(
                response,
                HttpStatus.UNAUTHORIZED,
                "TOKEN_MISSING",
                "Authorization Bearer token is required");
    }
}
