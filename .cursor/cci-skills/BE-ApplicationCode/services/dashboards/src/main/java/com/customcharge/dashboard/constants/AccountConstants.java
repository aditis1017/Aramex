package com.customcharge.dashboard.constants;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Account API search, validation, and sample-data constants.
 */
public final class AccountConstants {

    public static final String SEARCH_FIELD_ACCOUNT_NUMBER = "ACCOUNT_NUMBER";
    public static final String SEARCH_FIELD_ACCOUNT_NAME = "ACCOUNT_NAME";

    public static final String SEARCH_TYPE_CONTAINS = "CONTAINS";
    public static final String SEARCH_TYPE_EXACT_MATCH = "EXACT_MATCH";

    public static final int DEFAULT_SEARCH_PAGE = 0;
    public static final int DEFAULT_SEARCH_PAGE_SIZE = 100;
    public static final int MAX_SEARCH_PAGE_SIZE = 500;
    public static final int ACCOUNT_NUMBER_MIN_LENGTH = 5;
    public static final int ACCOUNT_NUMBER_MAX_LENGTH = 50;
    public static final int ACCOUNT_NAME_MIN_LENGTH = 2;
    public static final int ACCOUNT_NAME_MAX_LENGTH = 50;
    public static final int PLAIN_TEXT_MAX_LENGTH = 30;
    public static final String PLAIN_TEXT_PATTERN = "^[a-zA-Z0-9\\s_-]+$";
    public static final String ACCOUNT_SERVICES = "Services";

    public static final DateTimeFormatter SEARCH_RESULT_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MM yyyy");

    public static final String DEFAULT_AP_COUNTRY_CODE = "SA";

    public static final List<String> SAMPLE_ENTITIES = List.of("RUH", "JED", "DHA");
    public static final String SAMPLE_ACCOUNT_NUMBER_PRIMARY = "8001";
    public static final String SAMPLE_ACCOUNT_NUMBER_SECONDARY = "8002";
    public static final String SAMPLE_ACCOUNT_NUMBER_TERTIARY = "8003";
    public static final String SAMPLE_ACCOUNT_NUMBER_QUATERNARY = "8004";
    public static final String SAMPLE_ACCOUNT_NUMBER_QUINARY = "8005";

    public static final String SAMPLE_ENTITY_RUH = "RUH";
    public static final String SAMPLE_ENTITY_DHA = "DHA";
    public static final String SAMPLE_ENTITY_JED = "JED";

    public static final String SAMPLE_AR_ACCOUNT_NAME_8001_RUH = "Sample AR 8001 RUH";
    public static final String SAMPLE_AR_ACCOUNT_NAME_8002_DHA = "Sample AR 8002 DHA";
    public static final String SAMPLE_AR_ACCOUNT_NAME_8003_JED = "Sample AR 8003 JED";
    public static final String SAMPLE_AR_ACCOUNT_NAME_8004_RUH = "Sample AR 8004 RUH";
    public static final String SAMPLE_AR_ACCOUNT_NAME_8005_DHA = "Sample AR 8005 DHA";

    public static final String SAMPLE_ACCOUNT_STATUS_ACTIVE = "ACTIVE";
    public static final String SAMPLE_LOADER_CREATED_BY = "sample-loader";

    public static final String SEARCH_RESULT_DEDUPE_SEPARATOR = "|";
    public static final String CACHE_PAYLOAD_DROPDOWN_VALUES_KEY = "dropdownValues";
    public static final String REDIS_ACCOUNT_KEY_PREFIX = "acc:";

    public static final int DEFAULT_ACCOUNT_NUMBER_MAX_LENGTH = 100;

    public static final String FIELD_LABEL_COUNTRY = "Country";
    public static final String FIELD_LABEL_ENTITY = "Entity";
    public static final String FIELD_LABEL_RATE_CURRENCY = "Rate Currency";
    public static final String FIELD_LABEL_BILLING_CURRENCY = "Billing Currency";
    public static final String FIELD_LABEL_STATUS = "Status";
    public static final String FIELD_LABEL_ACCOUNT_TYPE = "Account Type";
    public static final String FIELD_LABEL_AR_ACCOUNT_NUMBER = "AR Account Number";
    public static final String FIELD_LABEL_SAP_COUNTRY_CODE = "SAP Country Code";
    public static final String FIELD_LABEL_DEFAULT_CASH_ACCOUNT = "Default Cash Account";
    public static final String FIELD_LABEL_INVOICE_LIMIT = "Invoice Limit";
    public static final String FIELD_LABEL_ENTRY_LIMIT = "Entry Limit";

    public static final String DROPDOWN_KEY_RATE_CURRENCY = "rateCurrency";
    public static final String DROPDOWN_KEY_BILLING_CURRENCY = "billingCurrency";
    public static final String DROPDOWN_KEY_MARKET_STRUCTURE = "marketStructure";
    public static final String DROPDOWN_KEY_STATUS = "status";
    public static final String DROPDOWN_KEY_INVOICE_SHIPMENT_LIMIT = "invoiceShipmentLimit";
    public static final String DROPDOWN_KEY_INVOICE_AMOUNT_LIMIT = "invoiceAmountLimit";
    public static final String DROPDOWN_KEY_POTENTIAL_DELAY = "potentialDelay";
    public static final String DROPDOWN_KEY_CUSTOMS_TYPE = "customsType";
    public static final String DROPDOWN_KEY_BUSINESS_TYPE = "businessType";
    public static final String DROPDOWN_KEY_BUSINESS_SEGMENT = "businessSegment";
    public static final String DROPDOWN_KEY_ELIGIBLE_FOR_BILLING = "eligibleForBilling";
    public static final String DROPDOWN_KEY_INVOICE_TRIGGER = "invoiceTrigger";
    public static final String DROPDOWN_KEY_INVOICE_BREAK_BY = "invoiceBreakBy";
    public static final String DROPDOWN_KEY_INVOICE_FREQUENCY = "invoiceFrequency";
    public static final String DROPDOWN_KEY_SPLIT_ENTRY = "splitEntry";
    public static final String DROPDOWN_KEY_ACCOUNT = "account";
    public static final String DROPDOWN_KEY_SPLIT_INVOICE = "split_invoice";
    public static final String DROPDOWN_KEY_INVOICE_FREQUENCY_SNAKE = "invoice_frequency";
    public static final String DROPDOWN_KEY_TRIGGER_INVOICE = "trigger_invoice";
    public static final String DROPDOWN_KEY_ELIGIBLE_FOR_BILLING_SNAKE = "eligible_for_billing";

    public static final String UPDATE_FIELD_ACCOUNT_STATUS = "accountStatus";
    public static final String UPDATE_FIELD_ACCOUNT_TYPE = "accountType";
    public static final String UPDATE_FIELD_RATE_CURRENCY_CODE = "rateCurrencyCode";
    public static final String UPDATE_FIELD_BILLING_CURRENCY_CODE = "billingCurrencyCode";
    public static final String UPDATE_FIELD_MARKET_STRUCTURE_CODE = "marketStructureCode";
    public static final String UPDATE_FIELD_SPLIT_ENTRY_CODE = "splitEntryCode";
    public static final String UPDATE_FIELD_INVOICE_AMOUNT_LIMIT_CODE = "invoiceAmountLimitCode";
    public static final String UPDATE_FIELD_POTENTIAL_DELAY_CODE = "potentialDelayCode";
    public static final String UPDATE_FIELD_AMOUNT_CODE = "amountCode";
    public static final String UPDATE_FIELD_INVOICE_SHIPMENT_LIMIT_CODE = "invoiceShipmentLimitCode";
    public static final String UPDATE_FIELD_INVOICE_BREAK_BY_CODE = "invoiceBreakByCode";
    public static final String UPDATE_FIELD_ELIGIBLE_FOR_BILLING_CODE = "eligibleForBillingCode";
    public static final String UPDATE_FIELD_TRIGGER_INVOICE_CODE = "triggerInvoiceCode";
    public static final String UPDATE_FIELD_INVOICE_FREQUENT_CODE = "invoiceFrequentCode";
    public static final String UPDATE_FIELD_INVOICE_LIMIT = "invoiceLimit";
    public static final String UPDATE_FIELD_SAP_COUNTRY_CODE = "sapCountryCode";
    public static final String UPDATE_FIELD_ENTRY_LIMIT = "entryLimit";
    public static final String UPDATE_FIELD_SPLIT_INVOICE = "splitInvoice";

    private AccountConstants() {
    }
}
