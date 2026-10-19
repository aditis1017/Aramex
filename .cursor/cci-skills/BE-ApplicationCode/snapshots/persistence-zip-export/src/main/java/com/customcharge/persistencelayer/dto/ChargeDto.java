package com.customcharge.persistencelayer.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ChargeDto extends AbstractDto {
    private UUID shipmentId;
    private String chargeCode;
    private UUID entityId;
    private String status;
    private BigDecimal estimateAmount;
    private BigDecimal actualAmount;
    private BigDecimal outstandingAmount;
    private String currency;
    private UUID arAccountId;
    private UUID apAccountId;
    private String externalDocumentNumber;
}
