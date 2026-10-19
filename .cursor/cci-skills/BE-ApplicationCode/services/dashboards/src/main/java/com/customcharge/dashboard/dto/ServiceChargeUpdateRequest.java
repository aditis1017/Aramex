package com.customcharge.dashboard.dto;

import com.customcharge.persistencelayer.enums.AccountNature;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ServiceChargeUpdateRequest {

    /** Null → create account-level {@code ServiceCharge} linked to an existing master service code. */
    private UUID id;

    /**
     * When present, resolves the account by id for upsert.
     * Redis is refreshed after save using the resolved account.
     */
    private UUID accountId;

    /**
     * Required when {@code accountId} is null — {@link AccountNature#AR} or {@link AccountNature#ENTITY_SETTINGS}.
     */
    private AccountNature accountNature;

    /**
     * Required when {@code accountId} is null and {@link #accountNature} is {@link AccountNature#AR}.
     */
    private String accountNumber;

    /** Required when {@code id} is null — same as {@code /fetch} entityCode. */
    private String entityCode;

    /** Required when {@code id} is null — same as {@code /fetch} chargeType. */
    private String chargeType;

    /** Required when {@code id} is null — master service code (e.g. DUTY, VAT, REVENUE). */
    private String serviceCode;

    private Boolean active;

    private String defaultAccountNumber;

    private Boolean isBillable;

    private Double taxValue;
}
