package com.customcharge.dashboard.constants;

import java.util.Map;
import java.util.Set;

/**
 * Account audit snapshot sanitization field names.
 */
public final class AccountAuditConstants {

    public static final String FIELD_SERVICE_CODE = "serviceCode";
    public static final String FIELD_SERVICE_DESCRIPTION = "serviceDescription";
    public static final String FIELD_SERVICE_CODES = "serviceCodes";
    public static final String FIELD_RATE_TYPE = "rateType";
    public static final String FIELD_PERCENTAGE_OF = "percentageOf";
    public static final String FIELD_VALUE = "value";

    public static final Map<String, String> LABEL_FIELD_CATEGORIES = Map.ofEntries(
            Map.entry("potentialDelay", MasterCategoryConstants.POTENTIAL_DELAY),
            Map.entry("invoiceAmountLimit", MasterCategoryConstants.INVOICE_AMOUNT_LIMIT),
            Map.entry("invoiceShipmentLimit", MasterCategoryConstants.INVOICE_SHIPMENT_LIMIT),
            Map.entry("invoiceBreakBy", MasterCategoryConstants.INVOICE_BREAK_BY),
            Map.entry("triggerInvoice", MasterCategoryConstants.INVOICE_TRIGGER),
            Map.entry("eligibleForBilling", MasterCategoryConstants.ELIGIBLE_FOR_BILLING),
            Map.entry("businessType", MasterCategoryConstants.BUSINESS_TYPE),
            Map.entry("businessSegment", MasterCategoryConstants.BUSINESS_TYPE_AR),
            Map.entry("customType", MasterCategoryConstants.CUSTOMS_TYPE),
            Map.entry("invoiceFrequent", MasterCategoryConstants.INVOICE_FREQUENCY),
            Map.entry("billingCurrency", MasterCategoryConstants.CURRENCY),
            Map.entry("rateCurrency", MasterCategoryConstants.CURRENCY),
            Map.entry("splitEntry", MasterCategoryConstants.SPLIT_ENTRY),
            Map.entry("splitInvoice", MasterCategoryConstants.SPLIT_INVOICE),
            Map.entry("account", MasterCategoryConstants.ACCOUNT),
            Map.entry("marketStructure", MasterCategoryConstants.MARKET_STRUCTURE));

    public static final String FIELD_ID = "id";
    public static final String FIELD_CREATED_BY = "createdBy";
    public static final String FIELD_UPDATED_BY = "updatedBy";
    public static final String FIELD_CREATED_TIME = "createdTime";
    public static final String FIELD_UPDATED_TIME = "updatedTime";

    public static final String FIELD_RATES = "rates";
    public static final String FIELD_ITEMS = "items";

    public static final String JAVERS_TYPE_NAME = "typeName";
    public static final String JAVERS_CDO_ID = "cdoId";

    public static final Set<String> AUDIT_METADATA_FIELDS_TO_STRIP = Set.of(
            FIELD_ID,
            FIELD_CREATED_BY,
            FIELD_UPDATED_BY,
            FIELD_CREATED_TIME,
            FIELD_UPDATED_TIME);

    public static final Set<String> EMPTY_ARRAY_FIELDS_TO_OMIT = Set.of(FIELD_RATES, FIELD_ITEMS);

    private AccountAuditConstants() {
    }
}
