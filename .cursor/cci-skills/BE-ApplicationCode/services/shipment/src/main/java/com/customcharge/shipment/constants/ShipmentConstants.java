package com.customcharge.shipment.constants;

import java.util.List;

public final class ShipmentConstants {

    public static final String BUSINESS_TYPE_E_COMMERCE = "E-Commerce";
    public static final String AR_BUSINESS_TYPE_E_COMMERCE_CODE = "E-Commerce_BT";
    public static final String AR_BUSINESS_TYPE_WHOLESALE = "Wholesale";
    public static final String AR_BUSINESS_TYPE_WHOLESALE_CODE = "WHOLESALE_BT";
    public static final String AR_BUSINESS_TYPE_CORPORATE = "Corporate Business";
    public static final String AR_BUSINESS_TYPE_CORPORATE_CODE = "CORPORATE_BUISNESS_BT";
    public static final String AR_BUSINESS_TYPE_CORPORATE_LEGACY_CODE = "Corporate Business_BT";
    public static final String AR_BUSINESS_TYPE_OIL_AND_GAS = "Oil and Gas";
    public static final String AR_BUSINESS_TYPE_OIL_AND_GAS_CODE = "OIL_AND_GAS_BT";
    public static final String AR_BUSINESS_TYPE_RETAIL_CASH = "Retail (Cash)";
    public static final String AR_BUSINESS_TYPE_RETAIL_CASH_CODE = "Retail_BT";
    public static final String SHIPMENT_TYPE_DDU = "DDU";
    public static final String SHIPMENT_TYPE_DDP = "DDP";
    public static final String SHIPMENT_TYPE_DDO = "DDO";
    public static final String SHIPMENT_TYPE_B2B = "B2B";
    public static final String SHIPMENT_BUSINESS_TYPE_B2B = "B2B";
    public static final String SHIPMENT_BUSINESS_TYPE_B2C = "B2C";
    public static final String PAYMENT_TYPE_COLLECT = "C";
    public static final String PAYMENT_TYPE_PREPAID = "P";
    public static final String PAYMENT_TYPE_FREE = "F";
    public static final String PAYMENT_TYPE_THIRD_PARTY = "3";
    public static final String CUSTOM_TYPE_DDO = "DDO";

    /** Primary Drools evaluation codes: DDU and DDO rules are merged by priority. */
    public static final List<String> SHIPMENT_TYPE_PRIMARY_RULE_EVALUATION_CODES =
            List.of(SHIPMENT_TYPE_DDU, SHIPMENT_TYPE_DDO);
    /** Fallback Drools evaluation when no primary rule matches. */
    public static final List<String> SHIPMENT_TYPE_FALLBACK_RULE_EVALUATION_CODES = List.of(SHIPMENT_TYPE_DDP);
    public static final String ENTITY_RUH = "RUH";
    public static final String ACCOUNT_SHIPPER_RUH_OVERRIDE = "72104493";
    public static final List<String> FULL_SHIPMENT_DESTINATION_ENTITIES = List.of("RUH", "JED", "DHA");
    public static final String PENDING_CALCULATION = "PENDING CALCULATION";
    public static final String DUE = "DUE";
    public static final String CHARGE_STATUS_EXEMPTED = "Exempted";

    public static final String CATEGORY_TYPE_HV = "HV";
    public static final String CATEGORY_TYPE_EXEMPTED = "Exempted";
    public static final String CATEGORY_TYPE_LV = "LV";

    public static final int DEFAULT_MONETARY_SCALE = 4;

    private ShipmentConstants() {
    }
}
