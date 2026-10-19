package com.customcharge.dashboard.constants;

/**
 * User-facing validation and API messages for account operations.
 */
public final class AccountMessageConstants {

    public static final String ACTIVE_CURRENCY_NOT_FOUND_FOR_COUNTRY =
            "Active currency not found for country";
    public static final String ENTITY_NOT_LINKED_TO_COUNTRY =
            "Entity not found, inactive, or not associated with the country per master lookup";
    public static final String COUNTRY_NOT_FOUND_OR_INACTIVE =
            "Country not found or inactive per master lookup";
    public static final String ENTITY_CODE_REQUIRED = "entityCode is required";
    public static final String ENTITY_CODE_IS_REQUIRED = "Entity code is required";
    public static final String ENTITY_NOT_FOUND_OR_INACTIVE_PREFIX = "Entity not found or inactive: ";
    public static final String AR_ACCOUNT_NOT_FOUND = "AR account not found";
    public static final String AP_ACCOUNT_NOT_FOUND = "AP account not found";
    public static final String MULTIPLE_AP_CONFIGS_FOUND =
            "Multiple AP account configs found; expected a single entry";
    public static final String ACCOUNT_NATURE_REQUIRED_FOR_SYNC =
            "accountNature is required when syncing a specific account";
    public static final String ENTITY_REQUIRED_FOR_SYNC =
            "entity is required when syncing a specific account";
    public static final String AR_ACCOUNT_NUMBER_AND_ENTITY_REQUIRED_FOR_SYNC =
            "accountNumber and entity are required when syncing a specific AR account";
    public static final String AP_ACCOUNT_NUMBER_AND_ENTITY_REQUIRED_FOR_SYNC =
            "accountNumber and entity are required when syncing a specific AP account";
    public static final String AR_ACCOUNT_MASTER_NOT_FOUND = "AR account master not found";
    public static final String AP_ACCOUNT_MASTER_NOT_FOUND = "AP account master not found";
    public static final String AR_ACCOUNT_SYNCED_TO_REDIS_PREFIX = "AR account synced to Redis: acc:";
    public static final String ENTITY_SETTINGS_SYNCED_TO_REDIS_PREFIX =
            "Entity settings synced to Redis: acc:";
    public static final String AP_ACCOUNT_SYNCED_TO_REDIS_PREFIX = "AP account synced to Redis: acc:";
    public static final String ACCOUNT_SYNCED_TO_REDIS_PREFIX = "Account synced to Redis: ";
    public static final String REDIS_CACHE_NOT_ENABLED = "Redis account cache is not enabled; nothing synced";
    public static final String SYNCED_ALL_ACCOUNTS_TO_REDIS_PREFIX = "Synced all accounts to Redis: ";
    public static final String SYNCED_ALL_AR_ACCOUNTS_TO_REDIS_PREFIX = "Synced all AR accounts to Redis: ";
    public static final String SYNCED_ALL_AP_ACCOUNTS_TO_REDIS_PREFIX = "Synced all AP accounts to Redis: ";
    public static final String SYNCED_NATURE_ACCOUNTS_SUFFIX = " account(s)";
    public static final String SYNCED_AR_MASTERS_SUFFIX = " AR master(s), ";
    public static final String SYNCED_AP_SUFFIX = " AP, ";
    public static final String SYNCED_ENTITY_SETTINGS_SUFFIX = " entity settings";
    public static final String SYNC_FAILURES_SUFFIX = "; ";
    public static final String SYNC_FAILURES_COUNT_SUFFIX = " failed";
    public static final String CHARGE_DATA_SYNC_STARTED = "Account Redis charge-data sync started";
    public static final String CHARGE_DATA_SYNC_COMPLETED_PREFIX = "Account Redis charge-data sync completed. succeeded=";
    public static final String ACCOUNT_NATURE_REQUIRED = "accountNature is required";
    public static final String ACCOUNT_SEARCH_NATURE_MUST_BE_AP_OR_AR =
            "accountNature must be AP or AR for account search";
    public static final String AR_UPDATE_ACCOUNT_NUMBER_AND_ENTITY_REQUIRED =
            "accountNumber and entity are required for AR update";
    public static final String AP_UPDATE_ACCOUNT_NUMBER_AND_ENTITY_REQUIRED =
            "accountNumber and entity are required for AP update";
    public static final String ENTITY_SETTINGS_ENTITY_REQUIRED =
            "entity is required for entity settings update";
    public static final String ENTITY_SETTINGS_CHARGE_TYPE_REQUIRED =
            "chargeType is required for entity settings update";
    public static final String MARKET_STRUCTURE_NOT_FOUND = "Market structure not found";
    public static final String RATE_CURRENCY_NOT_FOUND = "Rate currency not found";
    public static final String BILLING_CURRENCY_NOT_FOUND = "Billing currency not found";
    public static final String POTENTIAL_DELAY_NOT_FOUND = "Potential Delay not found";
    public static final String INVOICE_AMOUNT_LIMIT_NOT_FOUND = "Invoice Amount Limit not found";
    public static final String INVOICE_SHIPMENT_LIMIT_NOT_FOUND = "Invoice Shipment Limit not found";
    public static final String INVOICE_BREAK_BY_NOT_FOUND = "Invoice break By not found";
    public static final String TRIGGER_INVOICE_NOT_FOUND = "Trigger invoice not found";
    public static final String ELIGIBLE_FOR_BILLING_NOT_FOUND = "Eligible for billing not found";
    public static final String BUSINESS_TYPE_NOT_FOUND = "Business Type not found";
    public static final String BUSINESS_SEGMENT_NOT_FOUND = "Business Segment not found";
    public static final String AR_BUSINESS_TYPE_INVALID = "Business Type must be B2B or B2C";
    public static final String CUSTOM_TYPE_NOT_FOUND = "Custom Type not found";
    public static final String INVOICE_FREQUENCY_NOT_FOUND = "Invoice frequency not found";
    public static final String SPLIT_INVOICE_NOT_FOUND = "Split invoice not found";
    public static final String SPLIT_ENTRY_NOT_FOUND = "Split entry not found";
    public static final String ACCOUNT_CATEGORY_NOT_FOUND = "Account not found";
    public static final String FIELD_NOT_APPLICABLE_SUFFIX = " is not applicable for this account";
    public static final String FIELD_NOT_APPLICABLE_CHARGE_TYPE_PREFIX = " (chargeType=";
    public static final String FIELD_NOT_APPLICABLE_CHARGE_TYPE_SUFFIX = ")";
    public static final String COUNTRY_IS_REQUIRED = "Country is required";
    public static final String ENTITY_IS_REQUIRED = "Entity is required";
    public static final String RATE_CURRENCY_IS_REQUIRED = "Rate currency is required";
    public static final String BILLING_CURRENCY_IS_REQUIRED = "Billing currency is required";
    public static final String STATUS_IS_REQUIRED = "Status is required";
    public static final String ACCOUNT_TYPE_IS_REQUIRED = "Account type is required";
    public static final String ID_OR_ACCOUNT_KEYS_REQUIRED =
            "Id is required unless updating AR, AP, or entity settings with account keys";
    public static final String ACCOUNT_NUMBER_REQUIRED =
            "Account Number: Account number is required";
    public static final String ACCOUNT_NUMBER_LENGTH_PREFIX =
            "Account Number: Account number must be between ";
    public static final String ACCOUNT_NUMBER_LENGTH_AND = " and ";
    public static final String ACCOUNT_NUMBER_LENGTH_SUFFIX = " characters";
    public static final String ACCOUNT_NAME_REQUIRED = "Account Name: Account name is required";
    public static final String ACCOUNT_NAME_LENGTH_PREFIX =
            "Account Name: Account name must be between ";
    public static final String DEFAULT_ACCOUNT_NUMBER_BLANK = "Default Account Number must not be blank";
    public static final String DEFAULT_ACCOUNT_NUMBER_MAX_LENGTH =
            "Default Account Number must not exceed 100 characters";
    public static final String FIELD_MUST_NOT_BE_BLANK_SUFFIX = " must not be blank";
    public static final String FIELD_MAX_LENGTH_PREFIX = " must not exceed ";
    public static final String FIELD_MAX_LENGTH_SUFFIX = " characters";
    public static final String FIELD_INVALID_CHARACTERS_SUFFIX = " contains invalid characters";
    public static final String AR_ACCOUNT_ALREADY_EXISTS =
            "An AR account already exists for this account number and entity";
    public static final String ACCOUNT_ALREADY_EXISTS =
            "Account already exists for this number and entity";
    public static final String FIELD_LABEL_SEPARATOR = ": ";

    private AccountMessageConstants() {
    }
}
