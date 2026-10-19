package com.customcharge.chargescalculator.shipmentingest.coredata;

import com.customcharge.persistencelayer.exception.AbstractApiException;
import com.customcharge.persistencelayer.exception.ErrorCode;

public class CoreDataShipmentException extends AbstractApiException {

    public CoreDataShipmentException(String message) {
        super(ErrorCode.GATEWAY_TRACKING_FAILED, message);
    }

    public CoreDataShipmentException(String message, Throwable cause) {
        super(ErrorCode.GATEWAY_TRACKING_FAILED, message, cause);
    }
}
