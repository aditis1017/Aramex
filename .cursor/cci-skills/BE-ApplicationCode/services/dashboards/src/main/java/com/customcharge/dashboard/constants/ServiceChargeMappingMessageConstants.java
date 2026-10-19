package com.customcharge.dashboard.constants;

public final class ServiceChargeMappingMessageConstants {

    public static final String MAPPING_COMBINATION_EXISTS =
            "Mapping already exists for selected combination. Please use Edit functionality.";
    public static final String MAPPING_ROW_EXISTS = "Mapping already exists.";
    public static final String MAPPING_NOT_FOUND = "Service charge mapping not found";
    public static final String INVALID_DESTINATION_CHARGE_TYPE =
            "Charge type must be ECS or EC3 for service charge mapping";
    public static final String FROM_SERVICE_CODE_INVALID =
            "From service code must belong to the source entity with charge type DTA or ECD";
    public static final String TO_SERVICE_CODE_INVALID =
            "To service code must belong to the destination entity with the selected charge type";
    public static final String ALL_FROM_SERVICE_CODES_MUST_BE_MAPPED =
            "All DTA and ECD service codes must be mapped. Missing: ";
    public static final String NO_CHANGES = "No changes";

    public static final String MAPPING_DOES_NOT_BELONG_TO_CONTEXT =
            "Mapping does not belong to the given entity pair and charge type";
    public static final String SERVICE_CODE_IDS_NOT_FOUND_PREFIX = "One or more ";
    public static final String SERVICE_CODE_IDS_NOT_FOUND_SUFFIX = " ids were not found";
    public static final String FROM_SERVICE_CODE_LABEL = "from service code";
    public static final String TO_SERVICE_CODE_LABEL = "to service code";

    private ServiceChargeMappingMessageConstants() {
    }
}
