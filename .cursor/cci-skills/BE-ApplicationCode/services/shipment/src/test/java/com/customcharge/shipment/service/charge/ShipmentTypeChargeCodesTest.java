package com.customcharge.shipment.service.charge;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ShipmentTypeChargeCodesTest {

    @Test
    void codesForShipment_shouldReturnDduCharges() {
        assertThat(ShipmentTypeChargeCodes.codesForShipment("DDU", null))
                .containsExactly("ECD", "DTC");
        assertThat(ShipmentTypeChargeCodes.codesForShipment("ddo", null))
                .containsExactly("ECD", "DTC");
    }

    @Test
    void codesForShipment_shouldReturnDdpChargesByPaymentType() {
        assertThat(ShipmentTypeChargeCodes.codesForShipment("DDP", "P"))
                .containsExactly("ECS", "ECD", "DTS", "DTA");
        assertThat(ShipmentTypeChargeCodes.codesForShipment("DDP", "3"))
                .containsExactly("EC3", "ECD", "DT3", "DTA");
    }

    @Test
    void codesForShipment_shouldReturnEmptyForUnknown() {
        assertThat(ShipmentTypeChargeCodes.codesForShipment(null, null)).isEmpty();
        assertThat(ShipmentTypeChargeCodes.codesForShipment("UNKNOWN", null)).isEmpty();
        assertThat(ShipmentTypeChargeCodes.codesForShipment("DDP", "X")).isEmpty();
    }
}
