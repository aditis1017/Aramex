package com.customcharge.shipment.constants;

public final class RuleCacheConstants {

    public static final String RULE_KEY_PREFIX = "rule:";
    public static final String GLOBAL_SETTINGS_FACT_CLASS = "com.customcharge.persistencelayer.entities.Shipment";
    public static final String SHIPMENT_DTO_FACT_CLASS = "com.customcharge.persistencelayer.dto.ShipmentDto";
    public static final String ENTITY_PATTERN = " : Shipment(";
    public static final String DTO_PATTERN = " : ShipmentDto(";
    public static final String SET_CATEGORY_TYPE_CODE = ".setCategoryTypeCode(";
    public static final String SET_CATEGORY_TYPE = ".setCategoryType(";
    public static final String SET_SHIPMENT_TYPE = ".setShipmentType(";
    public static final String SET_IS_SHIPMENT_FULL = ".setIsShipmentFull(true)";

    private RuleCacheConstants() {
    }
}
