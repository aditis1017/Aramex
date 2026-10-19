package com.customcharge.dashboard.constants;

/**
 * User-facing messages for service charge operations.
 */
public final class ServiceChargeMessageConstants {

    public static final String AT_LEAST_ONE_UPDATE_REQUIRED =
            "At least one service charge update is required";
    public static final String SAMPLE_SERVICE_CODES_SEEDED_PREFIX =
            "Sample service codes ensured for RUH, DHA, and JED. New masters created: ";
    public static final String SERVICE_CODE_REQUIRED_ON_CREATE =
            "Service code is required when creating a service charge";
    public static final String ACCOUNT_NATURE_REQUIRED_WITHOUT_ID =
            "Account nature is required when account id is not provided";
    public static final String ACCOUNT_NATURE_MUST_BE_AR_OR_ENTITY_SETTINGS =
            "Account nature must be AR or ENTITY_SETTINGS when account id is not provided";
    public static final String ACCOUNT_NUMBER_REQUIRED_FOR_AR =
            "Account number is required for AR when account id is not provided";
    public static final String SERVICE_CHARGE_NOT_FOUND = "Service charge not found";
    public static final String SERVICE_CHARGE_ACCOUNT_MISMATCH =
            "Service charge does not belong to the given account";
    public static final String AR_ACCOUNT_MASTER_NOT_FOUND = "AR account master not found";
    public static final String ENTITY_CODE_REQUIRED = "Entity code is required";
    public static final String ENTITY_NOT_FOUND_OR_INACTIVE_PREFIX = "Entity not found or inactive: ";
    public static final String RESOLVED_ACCOUNT_NATURE_MISMATCH =
            "Resolved account does not match the requested account nature";
    public static final String SERVICE_CHARGE_ALREADY_EXISTS =
            "A service charge already exists for this account and service code";
    public static final String TAX_VALUE_NON_NEGATIVE = "Tax value must be greater than or equal to 0";
    public static final String DELETED_SUCCESSFULLY = "Deleted successfully";
    public static final String INVALID_ACCOUNT_ID_PREFIX = "Invalid account id: ";
    public static final String ENTITY_CODE_MISMATCH_PREFIX =
            "Entity code does not match the account entity: ";
    public static final String CHARGE_TYPE_MISMATCH_PREFIX =
            "Charge type does not match the account charge type: ";
    public static final String NULL_ACCOUNT_ID_PLACEHOLDER = "null";

    private ServiceChargeMessageConstants() {
    }
}
