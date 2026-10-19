package com.customcharge.chargescalculator.service.calculation;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class TaxValueAmountCalculator {

    private static final int AMOUNT_SCALE = 4;

    private TaxValueAmountCalculator() {
    }

    /**
     * {@code calculatedAmount = coreData + (coreData * taxValuePercent / 100)}
     */
    public static BigDecimal apply(BigDecimal coreData, BigDecimal taxValuePercent) {
        BigDecimal core = coreData == null ? BigDecimal.ZERO : coreData;
        if (core.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal taxPercent = taxValuePercent == null ? BigDecimal.ZERO : taxValuePercent;
        BigDecimal taxComponent = core.multiply(taxPercent)
                .divide(BigDecimal.valueOf(100), AMOUNT_SCALE, RoundingMode.HALF_UP);
        return core.add(taxComponent).setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
    }
}
