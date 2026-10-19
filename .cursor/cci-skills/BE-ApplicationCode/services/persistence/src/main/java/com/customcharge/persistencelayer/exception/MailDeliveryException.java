package com.customcharge.persistencelayer.exception;

public class MailDeliveryException extends AbstractApiException {

    public MailDeliveryException(ErrorCode errorCode) {
        super(errorCode);
    }

    public MailDeliveryException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public MailDeliveryException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
