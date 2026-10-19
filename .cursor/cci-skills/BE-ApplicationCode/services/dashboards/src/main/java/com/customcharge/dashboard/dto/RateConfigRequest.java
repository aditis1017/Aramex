package com.customcharge.dashboard.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class RateConfigRequest {

    @NotNull
    private UUID rateSheetItemId;

    /** When set, updates an existing nested rate; otherwise appends a new rate to the item. */
    private UUID rateId;

    @NotNull
    private String rateType;

    private String percentageOf;

    @NotNull
    private BigDecimal value;

    private String currency;
}
