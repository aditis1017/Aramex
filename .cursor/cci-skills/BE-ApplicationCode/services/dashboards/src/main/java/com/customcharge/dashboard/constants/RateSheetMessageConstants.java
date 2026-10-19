package com.customcharge.dashboard.constants;

/**
 * User-facing messages for rate sheet operations.
 */
public final class RateSheetMessageConstants {

    public static final String AT_LEAST_ONE_ITEM_REQUIRED = "At least one rate sheet item is required";
    public static final String RATE_SHEET_ITEM_ID_REQUIRED = "rateSheetItemId is required for update";
    public static final String ITEM_NOT_FOUND = "Item not found";
    public static final String RATE_SHEET_ITEM_NOT_FOUND = "RateSheetItem not found";
    public static final String UPDATED_SUCCESSFULLY = "Updated successfully";
    public static final String RATE_SHEET_NOT_FOUND_FOR_ITEM = "Rate sheet not found for item";
    public static final String ACCOUNT_NOT_FOUND_FOR_RATE_SHEET = "Account not found for rate sheet";
    public static final String ADD_ACCOUNT_INFO = "Add account info before configuring rate sheet";
    public static final String RATE_SHEET_ID_OR_ACCOUNT_NATURE_REQUIRED =
            "rateSheetId or accountNature is required";
    public static final String ENTITY_REQUIRED_WITHOUT_RATE_SHEET_ID =
            "entity is required when rateSheetId is not provided";
    public static final String CHARGE_TYPE_REQUIRED_WITHOUT_RATE_SHEET_ID =
            "chargeType is required when rateSheetId is not provided";
    public static final String ACCOUNT_NUMBER_REQUIRED_WITHOUT_RATE_SHEET_ID =
            "accountNumber is required when rateSheetId is not provided";
    public static final String ADD_ACCOUNT_CONFIGURATION_BEFORE_RATE_SHEET =
            "Add account configuration before adding new rate sheet";
    public static final String ACCOUNT_NUMBER_REQUIRED_FOR_AR =
            "accountNumber is required for AR rate sheet";
    public static final String ADD_ACCOUNT_CONFIGURATION_FOR_CHARGE_TYPE_PREFIX =
            "Add account configuration for charge type ";
    public static final String ADD_ACCOUNT_CONFIGURATION_FOR_CHARGE_TYPE_SUFFIX =
            " before rate sheet";
    public static final String ADD_ENTITY_SETTINGS_FOR_CHARGE_TYPE_PREFIX =
            "Add entity settings for charge type ";
    public static final String ADD_ENTITY_SETTINGS_FOR_CHARGE_TYPE_SUFFIX =
            " before rate sheet";
    public static final String ACCOUNT_NUMBER_REQUIRED_FOR_AP =
            "accountNumber is required for AP rate sheet";
    public static final String UNSUPPORTED_ACCOUNT_NATURE_PREFIX = "Unsupported account nature: ";

    public static final String AT_LEAST_ONE_RATE_CONFIGURATION_REQUIRED =
            "At least one rate configuration is required";
    public static final String RATE_TYPE_REQUIRED = "rateType is required";
    public static final String ROW_PREFIX = "Row ";
    public static final String ROW_PREFIX_SEPARATOR = ": ";
    public static final String PERCENTAGE_OF_REQUIRED_FOR_PERCENTAGE_RATE =
            "Percentage Of is required when Rate Type is Percentage.";
    public static final String VALUE_REQUIRED = "value is required";
    public static final String PERCENTAGE_CANNOT_EXCEED_100 = "Percentage cannot exceed 100.";
    public static final String VALUE_REQUIRED_FOR_FIXED = "value is required for FIXED";
    public static final String INVALID_PERCENTAGE_OF_VALUE = "Invalid Percentage Of value.";
    public static final String INVALID_RATE_TYPE_VALUE = "Invalid Rate Type value.";
    public static final String INVALID_PERCENTAGE_OF_PREFIX = "Invalid PercentageOf";

    public static final String SERVICE_CODE_REQUIRED = "Service code is required";
    public static final String SERVICE_CODE_INVALID_PREFIX = "Service code '";
    public static final String SERVICE_CODE_INVALID_SUFFIX =
            "' is not valid. It must be one of the configured service codes.";
    public static final String SHIPMENT_TYPE_REQUIRED = "Shipment Type is required";
    public static final String SHIPMENT_TYPE_INVALID_PREFIX = "Shipment Type '";
    public static final String SHIPMENT_TYPE_INVALID_SUFFIX =
            "' is not valid. It must be one of the configured shipment types.";
    public static final String BUSINESS_TYPE_REQUIRED = "Business Type is required";
    public static final String CATEGORY_TYPE_REQUIRED = "Category Type is required";

    public static final String DUPLICATE_ITEM_PREFIX = "Service Code '";
    public static final String DUPLICATE_ITEM_MIDDLE = "' with Shipment Type '";
    public static final String DUPLICATE_ITEM_BUSINESS_TYPE_MIDDLE = "', Business Type '";
    public static final String DUPLICATE_ITEM_CATEGORY_TYPE_MIDDLE = "', and Category Type '";
    public static final String DUPLICATE_ITEM_SUFFIX = "' already exists on this rate sheet.";

    public static final String CATCH_ALL_EXISTS_PREFIX =
            "A rate sheet item for Service Code '";
    public static final String CATCH_ALL_EXISTS_SUFFIX =
            "' with Shipment Type 'ALL', Business Type 'ALL', and Category Type 'ALL' already exists. "
                    + "No other Shipment Type, Business Type, or Category Type combination can be added "
                    + "for this Service Code.";
    public static final String CATCH_ALL_NOT_ALLOWED_PREFIX = "Cannot add Service Code '";
    public static final String CATCH_ALL_NOT_ALLOWED_SUFFIX =
            "' with Shipment Type 'ALL', Business Type 'ALL', and Category Type 'ALL' because other "
                    + "rate configurations already exist for this Service Code.";

    public static final String OVERLAP_PREFIX = "Service Code '";
    public static final String OVERLAP_MIDDLE = "' with Shipment Type '";
    public static final String OVERLAP_BUSINESS_TYPE_MIDDLE = "', Business Type '";
    public static final String OVERLAP_CATEGORY_TYPE_MIDDLE = "', and Category Type '";
    public static final String OVERLAP_SUFFIX =
            "' overlaps with an existing rate configuration for this Service Code.";

    public static final String IMPORT_REJECTED_SUFFIX = " Import rejected; no items were saved.";

    public static final String CANNOT_DEACTIVATE_SERVICE_CODE_PREFIX =
            "Cannot deactivate service code '";
    public static final String CANNOT_DEACTIVATE_SERVICE_CODE_SUFFIX =
            "' because it is used on the account rate sheet.";

    private RateSheetMessageConstants() {
    }
}
