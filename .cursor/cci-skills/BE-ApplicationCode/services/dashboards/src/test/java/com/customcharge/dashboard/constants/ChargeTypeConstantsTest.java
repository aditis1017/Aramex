package com.customcharge.dashboard.constants;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChargeTypeConstantsTest {

    @Test
    void revenueChargeLinkedToEcd_shouldUseDtcForDduShipmentTypes() {
        assertThat(ChargeTypeConstants.revenueChargeLinkedToEcd("DDU")).isEqualTo(ChargeTypeConstants.DTC);
        assertThat(ChargeTypeConstants.revenueChargeLinkedToEcd("DDO")).isEqualTo(ChargeTypeConstants.DTC);
    }

    @Test
    void revenueChargeLinkedToEcd_shouldUseDtaOtherwise() {
        assertThat(ChargeTypeConstants.revenueChargeLinkedToEcd("DDP")).isEqualTo(ChargeTypeConstants.DTA);
        assertThat(ChargeTypeConstants.revenueChargeLinkedToEcd(null)).isEqualTo(ChargeTypeConstants.DTA);
    }

    @Test
    void resolveMasterChargeTypes_shouldReturnSingleType() {
        assertThat(ChargeTypeConstants.resolveMasterChargeTypes("DTA")).containsExactly("DTA");
        assertThat(ChargeTypeConstants.resolveMasterChargeTypes("DTC")).containsExactly("DTC");
        assertThat(ChargeTypeConstants.resolveMasterChargeTypes("ECS")).containsExactly("ECS");
    }

    @Test
    void chargeGroupChecks_shouldClassifyCodes() {
        assertThat(ChargeTypeConstants.isCostAp("ecd")).isTrue();
        assertThat(ChargeTypeConstants.isRevenueAr("DTC")).isTrue();
        assertThat(ChargeTypeConstants.isEcsOrEc3("EC3")).isTrue();
    }

    @Test
    void requiresPreConvertedAmountWhenMirroringActuals_shouldApplyOnlyToFxRevenueTypes() {
        assertThat(ChargeTypeConstants.requiresPreConvertedAmountWhenMirroringActuals("DTA")).isTrue();
        assertThat(ChargeTypeConstants.requiresPreConvertedAmountWhenMirroringActuals("DTS")).isTrue();
        assertThat(ChargeTypeConstants.requiresPreConvertedAmountWhenMirroringActuals("DT3")).isTrue();
        assertThat(ChargeTypeConstants.requiresPreConvertedAmountWhenMirroringActuals("DTC")).isFalse();
    }

    @Test
    void pairedRevenueChargeType_shouldResolveCostPairs() {
        assertThat(ChargeTypeConstants.pairedRevenueChargeType("ECD", "DDP"))
                .contains(ChargeTypeConstants.DTA);
        assertThat(ChargeTypeConstants.pairedRevenueChargeType("ECD", "DDU"))
                .contains(ChargeTypeConstants.DTC);
        assertThat(ChargeTypeConstants.pairedRevenueChargeType("ECS", "DDP"))
                .contains(ChargeTypeConstants.DTS);
        assertThat(ChargeTypeConstants.pairedRevenueChargeType("EC3", "DDP"))
                .contains(ChargeTypeConstants.DT3);
        assertThat(ChargeTypeConstants.pairedRevenueChargeType("DTA", "DDP")).isEmpty();
    }

    @Test
    void normalize_shouldReturnEmptyForBlank() {
        assertThat(ChargeTypeConstants.normalize(null)).isEmpty();
        assertThat(ChargeTypeConstants.normalize("  ")).isEmpty();
        assertThat(ChargeTypeConstants.normalize(" dts ")).isEqualTo("DTS");
    }
}
