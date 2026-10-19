package com.customcharge.dashboard.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EntityChargeStatusMapperTest {

    @Test
    void toDisplay_shouldMapKnownStatuses() {
        assertThat(EntityChargeStatusMapper.toDisplay("DUE")).isEqualTo("Due");
        assertThat(EntityChargeStatusMapper.toDisplay("pending_calculation")).isEqualTo("Pending Calculation");
    }

    @Test
    void toDisplay_shouldHumanizeUnknownStatus() {
        assertThat(EntityChargeStatusMapper.toDisplay("CUSTOM_STATUS")).isEqualTo("Custom Status");
    }

    @Test
    void toDisplay_shouldReturnNullForBlank() {
        assertThat(EntityChargeStatusMapper.toDisplay(null)).isNull();
        assertThat(EntityChargeStatusMapper.toDisplay("  ")).isNull();
    }

    @Test
    void toDatabase_shouldMapDisplayLabels() {
        assertThat(EntityChargeStatusMapper.toDatabase("Due")).isEqualTo("DUE");
        assertThat(EntityChargeStatusMapper.toDatabase("non billable")).isEqualTo("NON BILLABLE");
    }

    @Test
    void toDatabase_shouldNormalizeUnknownValues() {
        assertThat(EntityChargeStatusMapper.toDatabase("Custom Label")).isEqualTo("CUSTOM LABEL");
    }

    @Test
    void toDatabase_shouldReturnNullForBlank() {
        assertThat(EntityChargeStatusMapper.toDatabase(null)).isNull();
        assertThat(EntityChargeStatusMapper.toDatabase("  ")).isNull();
    }

    @Test
    void toDatabase_shouldMapExemptedDisplayLabel() {
        assertThat(EntityChargeStatusMapper.toDatabase("exempted")).isEqualTo("Exempted");
    }
}
