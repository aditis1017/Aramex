package com.customcharge.dashboard.constants;

import java.util.List;

/**
 * Excel export column header labels.
 */
public final class ExportHeaderConstants {

    public static final String ID = "Id";
    public static final String CODE = "Code";
    public static final String DISPLAY_NAME = "Display name";

    public static final String SERVICE_CODE = "Service Code";
    public static final String DESCRIPTION = "Description";
    public static final String SERVICE_DESCRIPTION = "Service Description";
    public static final String ACTIVE = "Active";
    public static final String TAX_CODE = "Tax Code";
    public static final String TAX_VALUE = "Tax Value";
    public static final String DEFAULT_AP_ACCOUNT = "Default AP Account";
    public static final String BILLABLE_NON = "Billable / Non";
    public static final String SPLIT = "Split";

    public static final String SHIPMENT_TYPE = "Shipment Type";
    public static final String BUSINESS_TYPE = "Business Type";
    public static final String CATEGORY_TYPE = "Category Type";
    public static final String RATE_TYPE = "Rate Type";
    public static final String PERCENTAGE_OF = "Percentage Of";
    public static final String VALUE = "Value";
    public static final String RATE_TYPE_1 = "Rate Type 1";
    public static final String PERCENTAGE_OF_1 = "Percentage Of 1";
    public static final String VALUE_1 = "Value 1";
    public static final String RATE_TYPE_2 = "Rate Type 2";
    public static final String PERCENTAGE_OF_2 = "Percentage Of 2";
    public static final String VALUE_2 = "Value 2";
    public static final String RATE_TYPE_3 = "Rate Type 3";
    public static final String PERCENTAGE_OF_3 = "Percentage Of 3";
    public static final String VALUE_3 = "Value 3";

    public static final String ENTITY = "Entity";
    public static final String TRACKING_NUMBER = "Tracking Number";
    public static final String ACCOUNT_NUMBER = "Account Number";
    public static final String ACCOUNT_NAME = "Account Name";
    public static final String ORIGIN = "Origin";
    public static final String DESTINATION = "Destination";
    public static final String THIRD_PARTY = "Third Party";
    public static final String CHARGE_GROUP = "Charge Group";
    public static final String CHARGE_CODE = "Charge Code";
    public static final String CURRENCY = "Currency";
    public static final String TOTAL_OUTSTANDING = "Total Outstanding";
    public static final String TOTAL_BALANCE = "Total Balance";
    public static final String STATUS = "Status";

    public static final String KEYWORD = "Keyword";
    public static final String MATCH_TYPE = "Match Type";

    public static final List<String> ROLE_HEADERS = List.of(
            ID,
            CODE,
            DISPLAY_NAME
    );

    public static final List<String> SERVICE_CHARGE_HEADERS_COST_AP = List.of(
            SERVICE_CODE,
            DESCRIPTION,
            SERVICE_DESCRIPTION,
            ACTIVE,
            TAX_CODE,
            TAX_VALUE,
            DEFAULT_AP_ACCOUNT
    );

    public static final List<String> SERVICE_CHARGE_HEADERS_REVENUE_DT = List.of(
            SERVICE_CODE,
            DESCRIPTION,
            SERVICE_DESCRIPTION,
            ACTIVE,
            TAX_CODE,
            TAX_VALUE,
            BILLABLE_NON,
            SPLIT
    );

    public static final List<String> RATE_SHEET_HEADERS = List.of(
            SERVICE_CODE,
            DESCRIPTION,
            SHIPMENT_TYPE,
            BUSINESS_TYPE,
            CATEGORY_TYPE,
            RATE_TYPE_1,
            PERCENTAGE_OF_1,
            VALUE_1,
            RATE_TYPE_2,
            PERCENTAGE_OF_2,
            VALUE_2,
            RATE_TYPE_3,
            PERCENTAGE_OF_3,
            VALUE_3
    );

    public static final List<String> ENTITY_CHARGE_HEADERS = List.of(
            ENTITY,
            TRACKING_NUMBER,
            ACCOUNT_NUMBER,
            ACCOUNT_NAME,
            ORIGIN,
            DESTINATION,
            THIRD_PARTY,
            CHARGE_GROUP,
            CHARGE_CODE,
            CURRENCY,
            TOTAL_OUTSTANDING,
            TOTAL_BALANCE,
            STATUS
    );

    public static final List<String> RATE_SHEET_MINIMUM_CONFIGURED_COLUMNS = List.of(
            SERVICE_CODE,
            DESCRIPTION,
            SHIPMENT_TYPE,
            BUSINESS_TYPE,
            CATEGORY_TYPE,
            RATE_TYPE_1,
            VALUE_1
    );

    private ExportHeaderConstants() {
    }
}
