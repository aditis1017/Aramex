package com.customcharge.dashboard.dto;

import com.customcharge.persistencelayer.enums.AccountNature;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServiceChargeRequest {
    private String accountNumber;
    private AccountNature accountNature;
    /** Optional when a single account matches number + nature. */
    private String entityCode;

    /** When set, resolves the account row that matches this charge type (e.g. ECD, DTA). */
    private String chargeType;

    private String chargeName;
}