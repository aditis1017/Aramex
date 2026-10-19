package com.customcharge.persistencelayer.security.entra;

import com.customcharge.persistencelayer.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * JSON {@link AccessDeniedHandler} for authenticated users lacking authorization.
 */
@Component
@ConditionalOnEntraOidcSecurityEnabled
public class EntraAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(EntraAccessDeniedHandler.class);

    private final EntraSecurityResponseWriter responseWriter;

    public EntraAccessDeniedHandler(EntraSecurityResponseWriter responseWriter) {
        this.responseWriter = responseWriter;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        log.debug(
                "Entra security access denied method={} uri={} message={}",
                request.getMethod(),
                request.getRequestURI(),
                accessDeniedException != null ? accessDeniedException.getMessage() : "forbidden");
        responseWriter.write(
                response,
                HttpStatus.FORBIDDEN,
                ErrorCode.FORBIDDEN.getCode(),
                ErrorCode.FORBIDDEN.getMessage());
    }
}
