package com.customcharge.persistencelayer.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FixedDecimalUtilTest {

    @Mock
    private EntityPrecisionCacheService entityPrecisionCacheService;

    private FixedDecimalUtil fixedDecimalUtil;

    @BeforeEach
    void setUp() {
        fixedDecimalUtil = new FixedDecimalUtil(entityPrecisionCacheService);
    }

    @Test
    void convertToFixedDecimal_shouldUseDefaultTwoPlaces_whenConfigMissing() {
        when(entityPrecisionCacheService.resolveDecimalPlaces("DXB"))
                .thenReturn(FixedDecimalUtil.DEFAULT_DECIMAL_PLACES);

        assertThat(fixedDecimalUtil.convertToFixedDecimal(10.556, "DXB")).isEqualTo(10.56);
    }

    @Test
    void convertToFixedDecimal_shouldUseConfiguredDecimalPlaces() {
        when(entityPrecisionCacheService.resolveDecimalPlaces("RUH")).thenReturn(3);

        assertThat(fixedDecimalUtil.convertToFixedDecimal(1.23456, "RUH")).isEqualTo(1.235);
    }

    @Test
    void convertToFixedDecimal_shouldReturnNull_whenInputNull() {
        assertThat(fixedDecimalUtil.convertToFixedDecimal(null, "DXB")).isNull();
    }

    @Test
    void convertToFixedDecimal_shouldRejectNonFiniteValues() {
        assertThatThrownBy(() -> fixedDecimalUtil.convertToFixedDecimal(Double.NaN, "DXB"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void convertToFixedDecimal_shouldUseDefault_whenEntityCodeBlank() {
        when(entityPrecisionCacheService.resolveDecimalPlaces(null))
                .thenReturn(FixedDecimalUtil.DEFAULT_DECIMAL_PLACES);
        when(entityPrecisionCacheService.resolveDecimalPlaces("  "))
                .thenReturn(FixedDecimalUtil.DEFAULT_DECIMAL_PLACES);

        assertThat(fixedDecimalUtil.convertToFixedDecimal(1.239, null)).isEqualTo(1.24);
        assertThat(fixedDecimalUtil.convertToFixedDecimal(1.239, "  ")).isEqualTo(1.24);
    }

    @Test
    void convertToFixedDecimal_shouldUseDefault_whenResolverThrowsRuntimeException() {
        when(entityPrecisionCacheService.resolveDecimalPlaces("DXB"))
                .thenThrow(new RuntimeException("redis error"));

        assertThat(fixedDecimalUtil.convertToFixedDecimal(3.456, "DXB")).isEqualTo(3.46);
    }

    @Test
    void convertToFixedDecimal_shouldUseFourDecimalPlaces() {
        when(entityPrecisionCacheService.resolveDecimalPlaces("RUH")).thenReturn(4);

        assertThat(fixedDecimalUtil.convertToFixedDecimal(1.23456, "RUH")).isEqualTo(1.2346);
    }
}
