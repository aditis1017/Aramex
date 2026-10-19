package com.customcharge.persistencelayer.exception;

import com.customcharge.persistencelayer.dto.response.ApiResponse;
import com.customcharge.persistencelayer.dto.response.ExceptionResponse;
import com.customcharge.persistencelayer.security.entra.EntraRequestDeniedException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@RestControllerAdvice(basePackages = "com.customcharge")
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AbstractApiException.class)
    public ResponseEntity<ApiResponse<Object, ExceptionResponse>> handleApiException(AbstractApiException ex) {

        String requestId = MDC.get("traceId");

        ExceptionResponse error = new ExceptionResponse(
                ex.getErrorCode().getCode(),
                ex.getMessage()
        );

        HttpStatus status = mapToHttpStatus(ex.getErrorCode());

        log.error("[{}] {} - {}", requestId, ex.getErrorCode().getCode(), ex.getMessage(), ex);

        return ResponseEntity
                .status(status)
                .body(ApiResponse.failure(error, requestId));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object, ExceptionResponse>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex) {

        String requestId = MDC.get("traceId");
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse(ErrorCode.VALIDATION_FAILED.getMessage());

        ExceptionResponse error = new ExceptionResponse(ErrorCode.VALIDATION_FAILED.getCode(), message);
        log.warn("[{}] Validation failed: {}", requestId, message);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(error, requestId));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object, ExceptionResponse>> handleConstraintViolation(
            ConstraintViolationException ex) {

        String requestId = MDC.get("traceId");
        String message = ex.getConstraintViolations().stream()
                .findFirst()
                .map(ConstraintViolation::getMessage)
                .orElse(ErrorCode.VALIDATION_FAILED.getMessage());

        ExceptionResponse error = new ExceptionResponse(ErrorCode.VALIDATION_FAILED.getCode(), message);
        log.warn("[{}] Constraint violation: {}", requestId, message);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(error, requestId));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object, ExceptionResponse>> handleAccessDenied(AccessDeniedException ex) {
        String requestId = MDC.get("traceId");
        ExceptionResponse error = new ExceptionResponse(
                ErrorCode.FORBIDDEN.getCode(),
                ErrorCode.FORBIDDEN.getMessage());
        log.warn("[{}] Access denied: {}", requestId, ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.failure(error, requestId));
    }

    @ExceptionHandler(EntraRequestDeniedException.class)
    public ResponseEntity<ApiResponse<Object, ExceptionResponse>> handleEntraRequestDenied(EntraRequestDeniedException ex) {
        String requestId = MDC.get("traceId");
        ExceptionResponse error = new ExceptionResponse(ex.getErrorCode(), ex.getMessage());
        log.warn("[{}] Entra security denial {}: {}", requestId, ex.getErrorCode(), ex.getMessage());
        return ResponseEntity
                .status(ex.getHttpStatus())
                .body(ApiResponse.failure(error, requestId));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object, ExceptionResponse>> handleException(Exception ex) {

        String requestId = MDC.get("traceId");

        ExceptionResponse error = new ExceptionResponse(
                ErrorCode.INTERNAL_ERROR.getCode(),
                ErrorCode.INTERNAL_ERROR.getMessage()
        );

        log.error("[{}] Unexpected error: {}", requestId, ex.getMessage(), ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure(error, requestId));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object, ExceptionResponse>> handleIllegalArgument(IllegalArgumentException ex) {

        String requestId = MDC.get("traceId");
        String message = ex.getMessage() != null ? ex.getMessage() : ErrorCode.BAD_REQUEST.getMessage();
        ExceptionResponse error = new ExceptionResponse(ErrorCode.BAD_REQUEST.getCode(), message);

        log.warn("[{}] BAD_REQUEST (IllegalArgumentException): {}", requestId, message);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(error, requestId));
    }

    private HttpStatus mapToHttpStatus(ErrorCode errorCode) {
        return switch (errorCode) {
            case RESOURCE_NOT_FOUND, GLOBAL_SETTING_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case VALIDATION_FAILED, BAD_REQUEST -> HttpStatus.BAD_REQUEST;
            case CONFLICT, DUPLICATE_GLOBAL_SETTING -> HttpStatus.CONFLICT;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case MAIL_DELIVERY_FAILED -> HttpStatus.BAD_GATEWAY;
            case MAIL_TEMPLATE_FAILED -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}