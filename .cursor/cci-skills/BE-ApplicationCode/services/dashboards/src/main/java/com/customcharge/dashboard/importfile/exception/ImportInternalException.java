package com.customcharge.dashboard.importfile.exception;

import com.customcharge.persistencelayer.exception.AbstractApiException;
import com.customcharge.persistencelayer.exception.ErrorCode;

public class ImportInternalException extends AbstractApiException {

    public ImportInternalException(String message) {
        super(ErrorCode.INTERNAL_ERROR, message);
    }

    public ImportInternalException(String message, Throwable cause) {
        super(ErrorCode.INTERNAL_ERROR, message, cause);
    }
}
