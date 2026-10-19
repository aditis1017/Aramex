package com.customcharge.dashboard.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class RateSheetItemRateRequest {

    private UUID id;

    @NotNull
    private String rateType;

    private String percentageOf;

    @NotNull
    private BigDecimal value;
}
