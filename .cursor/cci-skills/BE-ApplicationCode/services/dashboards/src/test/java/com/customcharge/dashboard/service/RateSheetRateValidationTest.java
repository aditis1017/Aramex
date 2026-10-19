package com.customcharge.dashboard.service;

import com.customcharge.persistencelayer.exception.BadRequestException;
import com.customcharge.persistencelayer.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateSheetRateValidationTest {

    @Test
    void validateRatesPresent_shouldRejectEmptyList() {
        assertThatThrownBy(() -> RateSheetRateValidation.validateRatesPresent(List.of()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void validateRate_shouldRequireValueForFixed() {
        assertThatThrownBy(() -> RateSheetRateValidation.validateRate("Fixed", null, null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void validateRate_shouldValidatePercentage() {
        RateSheetRateValidation.validateRate("Percentage", BigDecimal.TEN, "Cost of Goods");
    }

    @Test
    void validateRate_shouldUseValidationExceptionWithRowNumber() {
        assertThatThrownBy(() -> RateSheetRateValidation.validateRate("Percentage", null, "Cost of Goods", 2))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Row 2");
    }

    @Test
    void validateRate_shouldRejectPercentageAboveOneHundred() {
        assertThatThrownBy(() -> RateSheetRateValidation.validateRate(
                "Percentage", BigDecimal.valueOf(101), "Cost of Goods"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void validateRate_shouldRejectInvalidRateType() {
        assertThatThrownBy(() -> RateSheetRateValidation.validateRate("Unknown", BigDecimal.ONE, null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void validateRate_shouldRejectMissingPercentageBasis() {
        assertThatThrownBy(() -> RateSheetRateValidation.validateRate("Percentage", BigDecimal.TEN, null))
                .isInstanceOf(BadRequestException.class);
    }
}
