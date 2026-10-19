package com.customcharge.dashboard.constants;

/**
 * User-facing messages for service code operations.
 */
public final class ServiceCodesMessageConstants {

    public static final String SERVICE_CODE_NOT_FOUND =
            "Service code not found for the given entity, charge type, and service code";
    public static final String CHARGE_TYPE_REQUIRED_FOR_RESOLUTION =
            "At least one charge type is required to resolve service code";
    public static final String SERVICE_CODE_NOT_FOUND_ACROSS_CHARGE_TYPES_PREFIX =
            "Service code not found for the given entity and service code across charge types: ";
    public static final String ENTITY_NOT_FOUND_OR_INACTIVE_PREFIX = "Entity not found or inactive: ";
    public static final String SERVICE_CODE_ALREADY_EXISTS_PREFIX =
            "Service code already exists for entity/charge type: ";
    public static final String CHARGE_TYPE_REQUIRED = "Charge type is required";
    public static final String CHARGE_TYPE_MAX_LENGTH_PREFIX = "Charge type must not exceed ";
    public static final String CHARGE_TYPE_MAX_LENGTH_SUFFIX = " characters";
    public static final String ENTITY_CODE_REQUIRED = "Entity code is required";
    public static final String SERVICE_CODE_REQUIRED = "Service code is required";
    public static final String SERVICE_CODE_MAX_LENGTH_PREFIX = "Service code must not exceed ";
    public static final String SERVICE_CODE_MAX_LENGTH_SUFFIX = " characters";
    public static final String SERVICE_DESCRIPTION_MAX_LENGTH_PREFIX =
            "Service description must not exceed ";
    public static final String SERVICE_DESCRIPTION_MAX_LENGTH_SUFFIX = " characters";
    public static final String TAX_CODE_MAX_LENGTH_PREFIX = "Tax code must not exceed ";
    public static final String TAX_CODE_MAX_LENGTH_SUFFIX = " characters";

    private ServiceCodesMessageConstants() {
    }
}
