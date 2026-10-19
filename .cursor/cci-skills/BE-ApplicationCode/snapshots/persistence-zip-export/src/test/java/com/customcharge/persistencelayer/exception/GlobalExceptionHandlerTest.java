package com.customcharge.persistencelayer.exception;

import com.customcharge.persistencelayer.dto.response.ApiResponse;
import com.customcharge.persistencelayer.dto.response.ExceptionResponse;
import com.customcharge.persistencelayer.security.entra.EntraRequestDeniedException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @SuppressWarnings("unused")
    private static void dummyEndpoint(SampleBody body) {
    }

    private static class SampleBody {
    }

    @BeforeEach
    void traceId() {
        org.slf4j.MDC.put("traceId", "trace-1");
    }

    @AfterEach
    void clearMdc() {
        org.slf4j.MDC.clear();
    }

    @Test
    void handleApiException_badRequest() {
        ResponseEntity<ApiResponse<Object, ExceptionResponse>> res =
                handler.handleApiException(new BadRequestException("nope"));
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleApiException_notFound() {
        ResponseEntity<ApiResponse<Object, ExceptionResponse>> res =
                handler.handleApiException(new ResourceNotFoundException("missing"));
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void handleApiException_globalSettingNotFound() {
        ResponseEntity<ApiResponse<Object, ExceptionResponse>> res =
                handler.handleApiException(new GlobalSettingNotFoundException(UUID.randomUUID()));
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void handleApiException_duplicateGlobalSetting() {
        ResponseEntity<ApiResponse<Object, ExceptionResponse>> res = handler.handleApiException(
                new ConflictException(ErrorCode.DUPLICATE_GLOBAL_SETTING, "dup"));
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void handleMethodArgumentNotValid() throws NoSuchMethodException {
        SampleBody target = new SampleBody();
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(target, "sampleBody");
        errors.addError(new FieldError("sampleBody", "f", "bad"));
        var method = GlobalExceptionHandlerTest.class.getDeclaredMethod("dummyEndpoint", SampleBody.class);
        MethodParameter param = new MethodParameter(method, 0);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(param, errors);

        ResponseEntity<ApiResponse<Object, ExceptionResponse>> res = handler.handleMethodArgumentNotValid(ex);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleConstraintViolation() {
        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> v = mock(ConstraintViolation.class);
        when(v.getMessage()).thenReturn("violation");
        ConstraintViolationException ex = new ConstraintViolationException(Set.of(v));

        ResponseEntity<ApiResponse<Object, ExceptionResponse>> res = handler.handleConstraintViolation(ex);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleIllegalArgument() {
        ResponseEntity<ApiResponse<Object, ExceptionResponse>> res =
                handler.handleIllegalArgument(new IllegalArgumentException("bad arg"));
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleEntraRequestDenied() {
        ResponseEntity<ApiResponse<Object, ExceptionResponse>> res = handler.handleEntraRequestDenied(
                new EntraRequestDeniedException(HttpStatus.FORBIDDEN, "INSUFFICIENT_ROLE", "Insufficient role"));
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(res.getBody().getException().getErrorCode()).isEqualTo("INSUFFICIENT_ROLE");
    }

    @Test
    void handleAccessDenied() {
        ResponseEntity<ApiResponse<Object, ExceptionResponse>> res =
                handler.handleAccessDenied(new AccessDeniedException("Denied"));
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(res.getBody().getException().getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN.getCode());
    }

    @Test
    void handleAuthorizationDenied() {
        ResponseEntity<ApiResponse<Object, ExceptionResponse>> res = handler.handleAccessDenied(
                new AuthorizationDeniedException("Denied", () -> false));
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(res.getBody().getException().getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN.getCode());
    }

    @Test
    void handleGenericException() {
        ResponseEntity<ApiResponse<Object, ExceptionResponse>> res =
                handler.handleException(new RuntimeException("boom"));
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void handleApiException_validationFailed() {
        ResponseEntity<ApiResponse<Object, ExceptionResponse>> res =
                handler.handleApiException(new ValidationException("invalid"));
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleApiException_internalError() {
        ResponseEntity<ApiResponse<Object, ExceptionResponse>> res =
                handler.handleApiException(new InternalServerException("fail"));
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void handleApiException_unauthorized() {
        ResponseEntity<ApiResponse<Object, ExceptionResponse>> res = handler.handleApiException(
                new ConflictException(ErrorCode.UNAUTHORIZED, "unauthorized"));
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void handleApiException_mailDeliveryFailed() {
        ResponseEntity<ApiResponse<Object, ExceptionResponse>> res = handler.handleApiException(
                new MailDeliveryException(ErrorCode.MAIL_DELIVERY_FAILED, "smtp down"));
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
    }

    @Test
    void handleApiException_mailTemplateFailed() {
        ResponseEntity<ApiResponse<Object, ExceptionResponse>> res = handler.handleApiException(
                new MailDeliveryException(ErrorCode.MAIL_TEMPLATE_FAILED, "template error"));
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void handleIllegalArgument_shouldUseDefaultMessage_whenNull() {
        ResponseEntity<ApiResponse<Object, ExceptionResponse>> res =
                handler.handleIllegalArgument(new IllegalArgumentException());
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody().getException().getErrorMessage()).isEqualTo(ErrorCode.BAD_REQUEST.getMessage());
    }
}
