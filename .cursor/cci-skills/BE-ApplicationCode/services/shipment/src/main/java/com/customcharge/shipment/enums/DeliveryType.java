package com.customcharge.shipment.enums;

public enum DeliveryType {
    DDU("DDU"),
    DDP("DDP"),
    DDO("DDO");

    private final String name;

    private DeliveryType(String name) {
        this.name = name;
    }
}
