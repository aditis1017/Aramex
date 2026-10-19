package com.customcharge.dashboard.constants;

import java.util.Map;

/**
 * Entity charge grid search filter field names and JPA path mappings.
 */
public final class EntityChargeFilterConstants {

    public static final String ACCOUNT_NUMBER_FIELD = "accountnumber";
    public static final String ACCOUNT_TYPE_FIELD = "accounttype";
    public static final String BOE_FIELD = "boe";
    public static final String FILTER_CONDITION_IN = "in";
    public static final String FILTER_CONDITION_NOT_IN = "notin";
    public static final String FILTER_CONDITION_BETWEEN_DATE = "betweendate";
    public static final String FILTER_CONDITION_BETWEEN_AND_EQUAL_DATE = "betweenandequaldate";
    public static final String SORT_FIELD_CREATED_TIME = "createdTime";

    public static final String PATH_STATUS = "status";
    public static final String PATH_SHIPMENT_PAYMENT_TYPE = "shipment.paymentType";
    public static final String PATH_SHIPMENT_BUSINESS_TYPE = "shipment.shipmentBusinessType";

    /** Allowed UI filter fields for the dashboard view (property names == filter field names). */
    public static final java.util.Set<String> VIEW_FILTER_FIELDS = java.util.Set.of(
            "entity",
            "chargetype",
            "chargestatus",
            "businesstype",
            "category",
            "trackingnumber",
            "originentity",
            "origincountry",
            "destinationentity",
            "destinationcountry",
            "thirdpartentity",
            "thirdpartcountry",
            "console",
            "pickupdate",
            "alerteddate"
    );

    public static final String VIEW_PATH_STATUS = "chargestatus";
    public static final String VIEW_PATH_BUSINESS_TYPE = "businesstype";
    public static final String VIEW_SORT_FIELD_CREATED_TIME = "createdTime";

    public static final Map<String, String> PAYMENT_TYPE_DISPLAY_TO_DB = PaymentTypeConstants.DISPLAY_TO_DB;

    private EntityChargeFilterConstants() {
    }
}
