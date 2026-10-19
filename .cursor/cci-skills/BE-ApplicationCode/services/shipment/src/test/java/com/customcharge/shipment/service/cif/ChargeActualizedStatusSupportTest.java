package com.customcharge.shipment.service.cif;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChargeActualizedStatusSupportTest {

    @Test
    void isActualized_shouldMatchNormalizedVariants() {
        assertThat(ChargeActualizedStatusSupport.isActualized("ACTUALIZED")).isTrue();
        assertThat(ChargeActualizedStatusSupport.isActualized("Actualized")).isTrue();
        assertThat(ChargeActualizedStatusSupport.isActualized("actualized")).isTrue();
        assertThat(ChargeActualizedStatusSupport.isActualized("ACTUALIZED ")).isTrue();
    }

    @Test
    void isActualized_shouldRejectOtherStatuses() {
        assertThat(ChargeActualizedStatusSupport.isActualized("DUE")).isFalse();
        assertThat(ChargeActualizedStatusSupport.isActualized("PENDING CALCULATION")).isFalse();
        assertThat(ChargeActualizedStatusSupport.isActualized(null)).isFalse();
    }
}
