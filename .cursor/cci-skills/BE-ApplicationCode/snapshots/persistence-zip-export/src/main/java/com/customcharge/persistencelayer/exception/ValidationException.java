package com.customcharge.persistencelayer.exception;

public class ValidationException extends AbstractApiException {

    public ValidationException() {
        super(ErrorCode.VALIDATION_FAILED);
    }

    public ValidationException(String message) {
        super(ErrorCode.VALIDATION_FAILED, message);
    }
}