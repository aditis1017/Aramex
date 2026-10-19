package com.customcharge.dashboard.service;

import com.customcharge.dashboard.constants.RateSheetMessageConstants;
import com.customcharge.dashboard.dto.RateSheetItemRateRequest;
import com.customcharge.persistencelayer.enums.PercentageOf;
import com.customcharge.persistencelayer.enums.RateType;
import com.customcharge.persistencelayer.exception.BadRequestException;
import com.customcharge.persistencelayer.exception.ValidationException;

import java.math.BigDecimal;
import java.util.List;

public final class RateSheetRateValidation {

    private static final BigDecimal MAX_PERCENTAGE = BigDecimal.valueOf(100);

    private RateSheetRateValidation() {
    }

    public static void validateRatesPresent(List<RateSheetItemRateRequest> rates) {
        if (rates == null || rates.isEmpty()) {
            throw new BadRequestException(RateSheetMessageConstants.AT_LEAST_ONE_RATE_CONFIGURATION_REQUIRED);
        }
    }

    public static void validateRate(String rateType, BigDecimal value, String percentageOf) {
        if (rateType == null || rateType.isBlank()) {
            throw new BadRequestException(RateSheetMessageConstants.RATE_TYPE_REQUIRED);
        }
        validateRate(rateType, value, percentageOf, null);
    }

    public static void validateRate(String rateType, BigDecimal value, String percentageOf, Integer rowNumber) {
        String prefix = rowNumber != null
                ? RateSheetMessageConstants.ROW_PREFIX + rowNumber + RateSheetMessageConstants.ROW_PREFIX_SEPARATOR
                : "";

        try {
            RateType parsedRateType = RateType.fromDisplayName(rateType);
            if (parsedRateType == RateType.PERCENTAGE) {
                if (percentageOf == null || percentageOf.isBlank()) {
                    throw error(prefix + RateSheetMessageConstants.PERCENTAGE_OF_REQUIRED_FOR_PERCENTAGE_RATE, rowNumber);
                }
                PercentageOf.fromDisplayName(percentageOf);
                if (value == null) {
                    throw error(prefix + RateSheetMessageConstants.VALUE_REQUIRED, rowNumber);
                }
                if (value.compareTo(MAX_PERCENTAGE) > 0) {
                    throw error(prefix + RateSheetMessageConstants.PERCENTAGE_CANNOT_EXCEED_100, rowNumber);
                }
            } else if (value == null) {
                throw error(prefix + RateSheetMessageConstants.VALUE_REQUIRED_FOR_FIXED, rowNumber);
            }
        } catch (IllegalArgumentException ex) {
            String message = ex.getMessage();
            if (message != null && message.startsWith(RateSheetMessageConstants.INVALID_PERCENTAGE_OF_PREFIX)) {
                throw error(prefix + RateSheetMessageConstants.INVALID_PERCENTAGE_OF_VALUE, rowNumber);
            }
            throw error(prefix + RateSheetMessageConstants.INVALID_RATE_TYPE_VALUE, rowNumber);
        }
    }

    private static RuntimeException error(String message, Integer rowNumber) {
        if (rowNumber != null) {
            return new ValidationException(message);
        }
        return new BadRequestException(message);
    }
}
