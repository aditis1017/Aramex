package com.customcharge.persistencelayer.security.entra;

import com.customcharge.persistencelayer.dto.response.ApiResponse;
import com.customcharge.persistencelayer.dto.response.ExceptionResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Writes {@link ApiResponse} JSON bodies for security failures.
 */
@Component
@ConditionalOnEntraOidcSecurityEnabled
public class EntraSecurityResponseWriter {

    private final ObjectMapper objectMapper;

    public EntraSecurityResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletResponse response, HttpStatus status, String errorCode, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String traceId = MDC.get("traceId");
        ApiResponse<Object, ExceptionResponse> body =
                ApiResponse.failure(new ExceptionResponse(errorCode, message), traceId);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
