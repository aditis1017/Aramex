package com.customcharge.shipment.enums;



public enum ProductType {
    EXP("EXP"),
    DOM("DOM");

    private final String name;

    private ProductType(String name) {
        this.name = name;
    }
}
