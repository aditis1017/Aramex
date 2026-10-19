package com.customcharge.persistencelayer.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ChargeLineDto extends AbstractDto {
    private UUID chargeId;
    private UUID serviceCodeId;
    private BigDecimal amount;
    private String taxIndicator;
}
