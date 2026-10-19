package com.customcharge.shipment.service;

/**
 * Thrown when an incomplete shipment message arrives before a persisted shipment record exists.
 */
public class IncompleteShipmentException extends RuntimeException {

    public IncompleteShipmentException(String message) {
        super(message);
    }
}
