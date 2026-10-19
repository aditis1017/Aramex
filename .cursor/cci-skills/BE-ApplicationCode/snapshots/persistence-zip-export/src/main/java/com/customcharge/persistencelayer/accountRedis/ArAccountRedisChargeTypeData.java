package com.customcharge.persistencelayer.accountRedis;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Per charge-type account configuration stored in {@link ArAccountRedisPayload#charges}.
 * {@link ArAccountRedisChargeTypeData#items} is retained for JSON shape compatibility; it is not
 * populated. Service-charge tax is resolved from the database via the service-charges fetch API.
 */
@Getter
@Setter
@NoArgsConstructor
public class ArAccountRedisChargeTypeData {

    private UUID accountId;
    private UUID rateSheetId;

    /** Per charge-type override; may be blank when an account config row exists. */
    private String arAccountNo;

    private String rateCurrency;
    private String billingCurrency;
    private String invoiceFrequency;
    private String potentialDelay;
    private String invoiceAmountLimit;
    private String invoiceShipmentLimit;
    private String invoiceBreakBy;
    private String businessType;
    private String customType;
    private String eligibleForBilling;
    private String invoiceTrigger;
    private String amountCode;

    /** Entity-settings fields (when nature is ENTITY_SETTINGS). */
    private String splitEntry;
    private String splitInvoice;
    private String invoiceLimit;
    private String sapCountryCode;
    private String entryLimit;
    private String defaultCashAccount;
    /** Entity-settings DTA default AR account number. */
    private String defaultArAccountNumber;
    private String account;
    private String defaultAccountNumber;

    /** True when service charges and rate sheet items were fully built for this charge type. */
    private Boolean chargeDataSynced;

    private List<ArAccountRedisServiceCharge> serviceCharges = new ArrayList<>();

    private List<ArAccountRedisRateSheetItem> rateSheetItems = new ArrayList<>();

    private List<ArAccountRedisChargeEntry> items = new ArrayList<>();
}
