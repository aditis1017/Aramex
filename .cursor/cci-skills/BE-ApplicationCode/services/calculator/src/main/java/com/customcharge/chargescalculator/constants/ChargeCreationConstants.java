package com.customcharge.chargescalculator.constants;

public final class ChargeCreationConstants {

    private ChargeCreationConstants() {
    }

    public static final String MISSING_ENTITY_CODE =
            "Shipment origin/destination entity code is missing for charge calculation";

    public static final String MISSING_ORIGIN_ENTITY_CODE =
            "Shipment origin entity code is missing for charge calculation";

    public static final String MISSING_THIRD_PARTY_ENTITY_CODE =
            "Shipment third party entity code is missing for charge calculation";

    public static final String ENTITY_MASTER_NOT_FOUND =
            "Entity master record not found for entity code";

    public static final String NO_ACCOUNTS_FOR_CHARGE =
            "No eligible accounts found for charge code";

    public static final String INVALID_FILE_REFERENCE_ID =
            "upload file id must be a valid UUID when present";

    public static final String INVALID_NUMERIC_AMOUNT_FOR_COLUMN =
            "Invalid numeric amount for mapped column '%s': '%s' is not a valid number";

    public static final String INVALID_NEGATIVE_AMOUNT_FOR_COLUMN =
            "Negative amount not allowed for mapped column '%s': '%s'";

    public static final String MISSING_CHARGE_FOR_ESTIMATE_CONVERSION =
            "Charge is required for estimate line amount conversion";

    public static final String MISSING_SHIPMENT_FOR_ESTIMATE_CONVERSION =
            "Shipment is required for estimate line amount conversion";

    public static final String MISSING_SERVICE_CHARGE_FOR_ESTIMATE_CONVERSION =
            "Service charge is required for estimate line amount conversion";

    public static final String INVALID_RATE_SHEET_RATE =
            "Rate sheet item contains an invalid rate entry";
}
