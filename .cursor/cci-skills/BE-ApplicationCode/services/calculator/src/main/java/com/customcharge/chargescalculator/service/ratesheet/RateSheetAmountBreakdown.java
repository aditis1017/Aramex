package com.customcharge.chargescalculator.service.ratesheet;

import java.math.BigDecimal;
import java.util.List;

public record RateSheetAmountBreakdown(BigDecimal fixedPart, List<PercentageRateSpec> percentageRates) {

    public static RateSheetAmountBreakdown zero() {
        return new RateSheetAmountBreakdown(BigDecimal.ZERO, List.of());
    }

    public BigDecimal total() {
        return safe(fixedPart);
    }

    private static BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
