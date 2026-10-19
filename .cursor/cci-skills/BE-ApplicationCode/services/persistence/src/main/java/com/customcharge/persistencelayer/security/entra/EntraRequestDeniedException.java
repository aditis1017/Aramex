package com.customcharge.persistencelayer.security.entra;

import org.springframework.http.HttpStatus;

/**
 * Signals authentication/authorization failure after JWT processing (mapped to HTTP by filters/handlers).
 */
public class EntraRequestDeniedException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String errorCode;

    public EntraRequestDeniedException(HttpStatus httpStatus, String errorCode, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
