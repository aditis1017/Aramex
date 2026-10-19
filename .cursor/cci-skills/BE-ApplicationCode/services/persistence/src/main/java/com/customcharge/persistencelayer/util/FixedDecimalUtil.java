package com.customcharge.persistencelayer.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@Slf4j
public class FixedDecimalUtil {

    public static final int DEFAULT_DECIMAL_PLACES = 2;

    private final EntityPrecisionCacheService entityPrecisionCacheService;

    public FixedDecimalUtil(EntityPrecisionCacheService entityPrecisionCacheService) {
        this.entityPrecisionCacheService = entityPrecisionCacheService;
    }

    public Double convertToFixedDecimal(Double inputVal, String entityCode) {
        if (inputVal == null) {
            return null;
        }
        if (inputVal.isNaN() || inputVal.isInfinite()) {
            throw new IllegalArgumentException("inputVal must be a finite number");
        }
        int decimalPlaces = resolveDecimalPlaces(entityCode);
        return BigDecimal.valueOf(inputVal)
                .setScale(decimalPlaces, RoundingMode.HALF_UP)
                .doubleValue();
    }

    int resolveDecimalPlaces(String entityCode) {
        try {
            return entityPrecisionCacheService.resolveDecimalPlaces(entityCode);
        } catch (RuntimeException ex) {
            log.warn(
                    "Failed to resolve decimal places for entity {}; using default {}",
                    entityCode,
                    DEFAULT_DECIMAL_PLACES,
                    ex);
            return DEFAULT_DECIMAL_PLACES;
        }
    }
}
