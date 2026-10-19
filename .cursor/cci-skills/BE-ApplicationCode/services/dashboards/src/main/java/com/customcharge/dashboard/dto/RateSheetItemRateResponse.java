package com.customcharge.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RateSheetItemRateResponse {

    private UUID id;
    private String rateType;
    private String percentageOf;
    private BigDecimal value;
}
