package com.customcharge.dashboard.constants;

/**
 * User-facing messages for entity charge search operations.
 */
public final class EntityChargeMessageConstants {

    public static final String SIZE_MUST_BE_AT_LEAST_ONE =
            "Query parameter 'size' must be at least 1.";
    public static final String PAGE_MUST_BE_ZERO_OR_GREATER =
            "Query parameter 'page' must be zero or greater.";
    public static final String INVALID_SEARCH_CRITERIA_OR_PAGINATION =
            "Invalid search criteria or pagination parameters.";
    public static final String UNABLE_TO_LOAD_ENTITY_CHARGES =
            "Unable to load entity charges for the given filters. Please adjust your criteria and try again.";

    private EntityChargeMessageConstants() {
    }
}
