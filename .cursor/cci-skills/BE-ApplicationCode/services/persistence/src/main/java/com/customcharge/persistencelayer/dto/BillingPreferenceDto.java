package com.customcharge.persistencelayer.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class BillingPreferenceDto extends AbstractDto {
    private UUID accountId;
    private String chargeCode;
    private String eligibleForBilling;
    private String invoiceTrigger;
    private String splitInvoice;
    private Integer invoiceLimit;
    private String invoicingFrequency;
    private String invoiceGroupBy;
    private Integer potentialDelayDays;
}
