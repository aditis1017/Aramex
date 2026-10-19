package com.customcharge.dashboard.constants;

/**
 * Shared validation and error messages for search, specification, controllers, and config.
 */
public final class ValidationConstants {

    // Entity charge filter validation
    public static final String ACCOUNT_NUMBER_FILTER_IN_ONLY =
            "accountNumber filter only supports condition 'in'";
    public static final String ACCOUNT_TYPE_FILTER_IN_ONLY =
            "accountType filter only supports condition 'in'";
    public static final String BOE_FILTER_IN_ONLY =
            "boe filter only supports condition 'in'";
    public static final String ACCOUNT_TYPE_FILTER_VALUE_BLANK =
            "accountType filter value must not be blank";
    public static final String ACCOUNT_TYPE_FILTER_VALUES =
            "accountType filter only supports values Cash or Account";
    public static final String FILTER_FIELD_NAME_BLANK = "Filter fieldName must not be blank";
    public static final String UNKNOWN_FILTER_FIELD_PREFIX = "Unknown filter field: ";
    public static final String FILTER_CONDITION_BLANK = "Filter condition must not be blank";

    // Pagination validation
    public static final String PAGE_ZERO_OR_GREATER = "page must be zero or greater.";
    public static final String PAGE_SIZE_AT_LEAST_ONE = "pageSize must be at least 1.";
    public static final String PAGE_SIZE_MAX_EXCEEDED_PREFIX = "pageSize must not exceed ";
    public static final String PAGE_SIZE_MAX_EXCEEDED_SUFFIX = ".";

    // Account search specification validation
    public static final String SEARCH_TYPE_REQUIRED = "searchType is required when searching by text";
    public static final String INVALID_SEARCH_TYPE_PREFIX = "Invalid search type: ";
    public static final String INVALID_SEARCH_FIELD_PREFIX = "Invalid search field: ";

    // Controller responses
    public static final String APPLICATION_RUNNING = "Application is running";
    public static final String ITEM_DELETED = "Item deleted";
    public static final String DEFAULT_USERS_CREATED = "5 default users created";
    public static final String MODULES_CREATED = "Modules created successfully";
    public static final String INVALID_CONVERTED_AMOUNT = "Invalid convertedAmount from exchange API";
    public static final String EXCHANGE_RATE_HTTP_PREFIX = "Exchange rate HTTP ";
    public static final String EXCHANGE_RATE_HTTP_SEPARATOR = ": ";
    public static final String EXCHANGE_RATE_CALL_FAILED_PREFIX = "Exchange rate call failed: ";

    // Config validation
    public static final String S3_REGION_REQUIRED = "mvp.persistence.aws.s3.region must be set";

    // Import screen catalog
    public static final String IMPORT_CONFIG_MISSING_PREFIX =
            "Direct import configuration is missing: classpath resource '";
    public static final String IMPORT_CONFIG_MISSING_SUFFIX = "' was not found.";
    public static final String IMPORT_CONFIG_SCREENS_MAPPING_PREFIX = "'";
    public static final String IMPORT_CONFIG_SCREENS_MAPPING_SUFFIX =
            "' must be a YAML document with a top-level 'screens' mapping.";
    public static final String IMPORT_CONFIG_SCREENS_OBJECT_SUFFIX =
            "' must define a 'screens' object mapping screen names to column lists.";
    public static final String IMPORT_CONFIG_SCREEN_LIST_PREFIX =
            "Invalid direct import config: 'screens.";
    public static final String IMPORT_CONFIG_SCREEN_LIST_SUFFIX = "' must be a list of column names.";
    public static final String IMPORT_CONFIG_SCREEN_COLUMNS_SUFFIX =
            "' must list at least one column.";
    public static final String IMPORT_CONFIG_SCREENS_EMPTY =
            "Invalid direct import config: 'screens' must contain at least one screen definition.";
    public static final String IMPORT_CONFIG_LOAD_FAILED =
            "Failed to load direct import screen configuration.";

    private ValidationConstants() {
    }
}
