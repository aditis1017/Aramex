package com.customcharge.dashboard.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Minimal create payload for an AP account; master-data codes are validated in the service.
 */
@Getter
@Setter
public class AccountAddRequestDTO {

    private String accountNumber;
    private String accountName;
    /** Active country master code. */
    private String country;
    /** Active entity code (must belong to {@link #country}). */
    private String entity;
    private String rateCurrency;
    private String billingCurrency;
    private String marketStructure;
    private String status;
    private String accountType;
}

