package com.customcharge.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntityChargeGridRowDTO {

    private String entity;
    private String trackingNumber;
    private String accountNumber;
    private String accountName;
    private String origin;
    private String destination;
    private String thirdParty;
    private String chargeGroup;
    private String chargeCode;
    private String currency;
    private BigDecimal totalOutstanding;
    private BigDecimal totalBalance;
    private String status;
}
