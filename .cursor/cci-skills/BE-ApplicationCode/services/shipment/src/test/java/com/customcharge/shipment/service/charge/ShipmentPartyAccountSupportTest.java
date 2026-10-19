package com.customcharge.shipment.service.charge;

import com.customcharge.persistencelayer.entities.Shipment;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ShipmentPartyAccountSupportTest {

    @Test
    void resolvePartyAccountNumber_shouldUseConsigneeForDtaAndDtc() {
        Shipment shipment = new Shipment();
        shipment.setConsigneeNumber(" CONS-1 ");
        shipment.setShipperNumber("SHIP-1");
        shipment.setThirdPartyNumber("TP-1");

        assertThat(ShipmentPartyAccountSupport.resolvePartyAccountNumber(shipment, "DTA"))
                .contains("CONS-1");
        assertThat(ShipmentPartyAccountSupport.resolvePartyAccountNumber(shipment, "DTC"))
                .contains("CONS-1");
    }

    @Test
    void resolvePartyAccountNumber_shouldUseShipperForDts() {
        Shipment shipment = new Shipment();
        shipment.setConsigneeNumber("CONS-1");
        shipment.setShipperNumber(" SHIP-1 ");

        assertThat(ShipmentPartyAccountSupport.resolvePartyAccountNumber(shipment, "DTS"))
                .contains("SHIP-1");
    }

    @Test
    void resolvePartyAccountNumber_shouldUseThirdPartyForDt3() {
        Shipment shipment = new Shipment();
        shipment.setConsigneeNumber("CONS-1");
        shipment.setShipperNumber("SHIP-1");
        shipment.setThirdPartyNumber(" TP-1 ");

        assertThat(ShipmentPartyAccountSupport.resolvePartyAccountNumber(shipment, "DT3"))
                .contains("TP-1");
    }

    @Test
    void resolvePartyAccountNumber_shouldUseShipperForDomDduDtc() {
        Shipment shipment = new Shipment();
        shipment.setProductGroup("DOM");
        shipment.setShipmentType("DDU");
        shipment.setConsigneeNumber("CONS-1");
        shipment.setShipperNumber(" SHIP-1 ");

        assertThat(ShipmentPartyAccountSupport.resolvePartyAccountNumber(shipment, "DTC"))
                .contains("SHIP-1");
    }

    @Test
    void resolvePartyAccountNumber_shouldReturnEmptyWhenPartyMissing() {
        Shipment shipment = new Shipment();

        assertThat(ShipmentPartyAccountSupport.resolvePartyAccountNumber(shipment, "DTS")).isEmpty();
        assertThat(ShipmentPartyAccountSupport.resolvePartyAccountNumber(null, "DTC")).isEmpty();
        assertThat(ShipmentPartyAccountSupport.resolvePartyAccountNumber(shipment, " ")).isEmpty();
    }
}
