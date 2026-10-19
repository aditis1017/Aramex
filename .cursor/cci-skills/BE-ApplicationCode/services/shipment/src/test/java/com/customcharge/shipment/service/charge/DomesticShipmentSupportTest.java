package com.customcharge.shipment.service.charge;

import com.customcharge.persistencelayer.constant.DomEntityConstants;
import com.customcharge.persistencelayer.entities.Shipment;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DomesticShipmentSupportTest {

    @Test
    void isDomesticProductGroup_shouldMatchDomCaseInsensitively() {
        assertThat(DomesticShipmentSupport.isDomesticProductGroup("DOM")).isTrue();
        assertThat(DomesticShipmentSupport.isDomesticProductGroup(" dom ")).isTrue();
        assertThat(DomesticShipmentSupport.isDomesticProductGroup("EXP")).isFalse();
        assertThat(DomesticShipmentSupport.isDomesticProductGroup(null)).isFalse();
    }

    @Test
    void usesRuhDduChargeBilling_shouldApplyForDomDduEcdAndDtc() {
        Shipment shipment = domesticDduShipment();

        assertThat(DomesticShipmentSupport.usesRuhDduChargeBilling(shipment, "ECD")).isTrue();
        assertThat(DomesticShipmentSupport.usesRuhDduChargeBilling(shipment, "DTC")).isTrue();
        assertThat(DomesticShipmentSupport.resolveBillingEntityCode(shipment, "ECD"))
                .isEqualTo(DomEntityConstants.DEFAULT_DOM_ENTITY_CODE);
    }

    @Test
    void resolveDomEntityCode_shouldDefaultWhenConsigneeCountryMissing() {
        assertThat(DomesticShipmentSupport.resolveDomEntityCode(null))
                .isEqualTo(DomEntityConstants.DEFAULT_DOM_ENTITY_CODE);
    }

    @Test
    void usesRuhDduChargeBilling_shouldApplyForDomDdo() {
        Shipment shipment = domesticDduShipment();
        shipment.setShipmentType("DDO");

        assertThat(DomesticShipmentSupport.usesRuhDduChargeBilling(shipment, "DTC")).isTrue();
    }

    @Test
    void usesRuhDduChargeBilling_shouldNotApplyForExpOrDdp() {
        Shipment shipment = domesticDduShipment();
        shipment.setProductGroup("EXP");
        assertThat(DomesticShipmentSupport.usesRuhDduChargeBilling(shipment, "DTC")).isFalse();

        shipment.setProductGroup("DOM");
        shipment.setShipmentType("DDP");
        assertThat(DomesticShipmentSupport.usesRuhDduChargeBilling(shipment, "DTC")).isFalse();
    }

    @Test
    void resolveDtcPartyAccountNumberForAccountType_shouldUseShipperForDomDdu() {
        Shipment shipment = domesticDduShipment();
        shipment.setShipperNumber("SHIP-1");
        shipment.setConsigneeNumber("CONS-1");

        assertThat(DomesticShipmentSupport.resolveDtcPartyAccountNumberForAccountType(shipment))
                .isEqualTo("SHIP-1");
    }

    @Test
    void resolveDtcPartyAccountNumberForAccountType_shouldUseConsigneeForNonDom() {
        Shipment shipment = domesticDduShipment();
        shipment.setProductGroup("EXP");
        shipment.setShipperNumber("SHIP-1");
        shipment.setConsigneeNumber("CONS-1");

        assertThat(DomesticShipmentSupport.resolveDtcPartyAccountNumberForAccountType(shipment))
                .isEqualTo("CONS-1");
    }

    private static Shipment domesticDduShipment() {
        Shipment shipment = new Shipment();
        shipment.setProductGroup("DOM");
        shipment.setShipmentType("DDU");
        shipment.setHAWBDestinationEntity("JED");
        return shipment;
    }
}
