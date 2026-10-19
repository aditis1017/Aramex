package com.customcharge.chargescalculator.service.ratesheet;

import com.customcharge.persistencelayer.enums.PercentageOf;

import java.math.BigDecimal;

public record PercentageRateSpec(BigDecimal ratePercent, PercentageOf percentageOf) {
}
