package com.customcharge.dashboard.mapper;

import com.customcharge.dashboard.constants.ChargeTypeConstants;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EntityChargeGridRowMapperTest {

    @Test
    void resolveChargeGroup_shouldClassifyChargeCodes() {
        assertThat(EntityChargeGridRowMapper.resolveChargeGroup("DTC"))
                .isEqualTo(ChargeTypeConstants.CHARGE_GROUP_REVENUE_AR);
        assertThat(EntityChargeGridRowMapper.resolveChargeGroup("ECD"))
                .isEqualTo(ChargeTypeConstants.CHARGE_GROUP_COST_AP);
        assertThat(EntityChargeGridRowMapper.resolveChargeGroup("UNKNOWN")).isNull();
    }

    @Test
    void formatLocation_shouldCombineEntityAndCountry() {
        assertThat(EntityChargeGridRowMapper.formatLocation("DXB", "AE")).isEqualTo("DXB/AE");
        assertThat(EntityChargeGridRowMapper.formatLocation("DXB", null)).isEqualTo("DXB");
        assertThat(EntityChargeGridRowMapper.formatLocation(null, "AE")).isEqualTo("AE");
    }
}
