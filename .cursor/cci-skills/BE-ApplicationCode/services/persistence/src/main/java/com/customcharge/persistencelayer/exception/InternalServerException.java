package com.customcharge.persistencelayer.exception;

public class InternalServerException extends AbstractApiException {

    public InternalServerException() {
        super(ErrorCode.INTERNAL_ERROR);
    }

    public InternalServerException(String message) {
        super(ErrorCode.INTERNAL_ERROR, message);
    }

    public InternalServerException(Throwable cause) {
        super(ErrorCode.INTERNAL_ERROR, cause);
    }
}