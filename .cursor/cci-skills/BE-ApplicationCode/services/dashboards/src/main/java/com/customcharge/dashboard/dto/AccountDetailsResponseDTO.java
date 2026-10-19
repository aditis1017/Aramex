package com.customcharge.dashboard.dto;

import com.customcharge.persistencelayer.enums.AccountType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Single account-details payload; scalar fields are populated only for the requested
 * {@link com.customcharge.persistencelayer.enums.AccountNature}; others remain null.
 * {@link #dropdownValues} lists allowed option codes / enum names per field name.
 */
@Getter
@Setter
@NoArgsConstructor
public class AccountDetailsResponseDTO {
    @NotNull
    private UUID id;
    private String chargeName;
    private String accountNumber;
    private String accountName;
    private String country;
    private String entity;
    private String accountType;
    private String chargeType;
    private String rateCurrency;
    private String billingCurrency;
    private String marketStructure;
    /** Free-form or workflow status (AP). */
    private String status;
    private String arAccountNumber;
    private String invoiceFrequency;
    private String customsType;
    /** B2B/B2C business type; explicit on AR master or derived from {@link #businessSegment}. */
    private String businessType;
    /** AR business segment (Corporate, Wholesale, E-Commerce, etc.). */
    private String businessSegment;
    private String eligibleForBilling;
    private String invoiceTrigger;
    private String invoiceBreakBy;
    private String invoiceShipmentLimit;
    private String invoiceLimit;
    private String potentialDelay;
    private String splitEntry;
    private String splitInvoice;
    private String entryLimit;
    private String defaultCashAccount;
    /**
     * Entity-settings cash / SAP account reference (from {@code Account.defaultCashAccount}).
     */
    private String account;
    /** Not present on domain today; reserved for future use. */
    private String sapCompanyCode;
    private UUID rateSheetId;
    private String defaultAccountNumber;
    private Map<String, List<String>> dropdownValues;
}
