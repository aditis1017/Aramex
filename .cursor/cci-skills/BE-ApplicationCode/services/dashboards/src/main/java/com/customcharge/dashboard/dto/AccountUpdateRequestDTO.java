package com.customcharge.dashboard.dto;

import com.customcharge.persistencelayer.enums.AccountNature;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class AccountUpdateRequestDTO {

    private UUID id;
    private AccountNature accountNature;
    private String accountNumber;
    private String entity;
    private String chargeType;
    private String accountName;
    private String accountStatus;
    /** AR-only (business type); omit for AP / entity-settings updates. */
    private String accountType;
    /** Active currency code from the {@code currency} table; validated in service before save. */
    private String rateCurrency;
    /** Active currency code from the {@code currency} table; validated in service before save. */
    private String billingCurrency;
    /** Master value code in {@code MARKET_STRUCTURE} category; validated in service before save. */
    private String marketStructure;
    private String splitEntry;
    private String invoiceAmountLimit;
    private String potentialDelay;
    private String account;
    private String invoiceShipmentLimit;
    private String invoiceBreakBy;
    private String eligibleForBilling;
    private String triggerInvoice;
    private String invoiceFrequent;
    private String invoiceLimit;
    private String sapCountryCode;
    private String entryLimit;
    private String defaultCashAccount;
    private String splitInvoice;
    /** Explicit B2B/B2C AR business type; omit to keep existing or derive on read from {@link #businessSegment}. */
    private String businessType;
    /** AR business segment (Corporate, Wholesale, E-Commerce, etc.). */
    private String businessSegment;
    private String customType;
    private String arAccountNumber;
    private String defaultAccountNumber;
}
