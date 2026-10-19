package com.customcharge.shipment.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ShipmentEnumsTest {

    @Test
    void chargeStatus_shouldExposeAllValues() {
        assertThat(ChargeStatus.values()).containsExactly(
                ChargeStatus.PENDING_CALCULATION,
                ChargeStatus.DUE,
                ChargeStatus.SETTLED,
                ChargeStatus.NOT_DUE,
                ChargeStatus.NON_BILLABLE);
    }

    @Test
    void deliveryType_shouldExposeConfiguredNames() {
        assertThat(DeliveryType.DDU.name()).isEqualTo("DDU");
        assertThat(DeliveryType.DDP.name()).isEqualTo("DDP");
        assertThat(DeliveryType.DDO.name()).isEqualTo("DDO");
    }

    @Test
    void piCode_shouldExposeNameAndDescription() {
        assertThat(PICode.SH013.getName()).isEqualTo("SH013");
        assertThat(PICode.SH013.getDescription()).isEqualTo("Create charges");
        assertThat(PICode.SH267.getName()).isEqualTo("SH267");
        assertThat(PICode.SH041.getName()).isEqualTo("SH041");
    }

    @Test
    void productType_shouldExposeNames() {
        assertThat(ProductType.EXP.name()).isEqualTo("EXP");
        assertThat(ProductType.DOM.name()).isEqualTo("DOM");
    }
}
