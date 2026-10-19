package com.customcharge.dashboard.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RateSheetItemKeyUtilTest {

    @Test
    void isCatchAll_shouldReturnTrueOnlyWhenAllDimensionsAreAll() {
        assertThat(RateSheetItemKeyUtil.isCatchAll("ALL", "ALL", "ALL")).isTrue();
        assertThat(RateSheetItemKeyUtil.isCatchAll("all", "ALL", "all")).isTrue();
        assertThat(RateSheetItemKeyUtil.isCatchAll("DDU", "ALL", "ALL")).isFalse();
        assertThat(RateSheetItemKeyUtil.isCatchAll("ALL", "B2B", "LV")).isFalse();
    }

    @Test
    void itemKey_shouldNormalizeParts() {
        assertThat(RateSheetItemKeyUtil.itemKey(" express ", " ddu ", " b2b ", " lv "))
                .isEqualTo("EXPRESS::DDU::B2B::LV");
    }

    @Test
    void configurationsOverlap_shouldDetectPartialWildcardOverlap() {
        assertThat(RateSheetItemKeyUtil.configurationsOverlap(
                        "ALL", "ALL", "LV", "ALL", "B2C", "LV"))
                .isTrue();
        assertThat(RateSheetItemKeyUtil.configurationsOverlap(
                        "DDU", "ALL", "LV", "DDU", "B2C", "LV"))
                .isTrue();
        assertThat(RateSheetItemKeyUtil.configurationsOverlap(
                        "DDU", "B2B", "LV", "DDP", "B2B", "LV"))
                .isFalse();
        assertThat(RateSheetItemKeyUtil.configurationsOverlap(
                        "ALL", "ALL", "ALL", "DDU", "B2B", "LV"))
                .isTrue();
    }

    @Test
    void itemKey_shouldHandleNullValues() {
        assertThat(RateSheetItemKeyUtil.itemKey(null, null, null, null)).isEqualTo("::::::");
    }
}
