package com.customcharge.persistencelayer.exception;

public enum ErrorCode {

    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", "Requested resource was not found"),
    GLOBAL_SETTING_NOT_FOUND("GLOBAL_SETTING_NOT_FOUND", "Global setting was not found"),
    VALIDATION_FAILED("VALIDATION_FAILED", "Validation failed for the request"),
    CONFLICT("CONFLICT", "Resource conflict"),
    DUPLICATE_GLOBAL_SETTING("DUPLICATE_GLOBAL_SETTING", "A global setting with the same criteria already exists"),
    BAD_REQUEST("BAD_REQUEST", "Bad request"),
    INTERNAL_ERROR("INTERNAL_ERROR", "An internal error occurred"),
    UNAUTHORIZED("UNAUTHORIZED", "Unauthorized"),
    FORBIDDEN("FORBIDDEN", "Access forbidden"),
    MAIL_DELIVERY_FAILED("MAIL_DELIVERY_FAILED", "Email could not be sent"),
    MAIL_TEMPLATE_FAILED("MAIL_TEMPLATE_FAILED", "Email template processing failed"),
    GATEWAY_TRACKING_FAILED("GATEWAY_TRACKING_FAILED", "Gateway tracking request failed");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
