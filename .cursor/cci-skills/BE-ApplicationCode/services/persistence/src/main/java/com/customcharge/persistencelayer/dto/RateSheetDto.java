package com.customcharge.persistencelayer.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class RateSheetDto extends AbstractDto {
    private String rateType;
    private UUID apAccountId;
    private UUID arAccountId;
    private UUID entityId;
    private String chargeCode;
    private String countryCode;
    private UUID serviceCodeId;
    private BigDecimal fixedRate;
    private BigDecimal percentage;
    private String percentageOf;
    private Boolean sameAsCost;
    private LocalDate startDate;
    private LocalDate endDate;
}
