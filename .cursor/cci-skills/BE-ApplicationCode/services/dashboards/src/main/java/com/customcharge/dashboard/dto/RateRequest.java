package com.customcharge.dashboard.dto;

import com.customcharge.persistencelayer.enums.PercentageOf;
import com.customcharge.persistencelayer.enums.RateType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class RateRequest {

    @NotNull
    private RateType rateType;

    private PercentageOf percentageOf;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal value;

    private String currency;
}
