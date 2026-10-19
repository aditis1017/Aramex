package com.customcharge.shipment.constants;

import com.customcharge.persistencelayer.entities.Shipment;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ShipmentChargeTypeSupportTest {

    @Test
    void normalize_shouldUppercaseAndTrim() {
        assertThat(ShipmentChargeTypeSupport.normalize(" dtc ")).isEqualTo("DTC");
        assertThat(ShipmentChargeTypeSupport.normalize(null)).isEmpty();
    }

    @Test
    void chargeTypeFlags_shouldClassifyCodes() {
        assertThat(ShipmentChargeTypeSupport.usesEntitySettingsAccountOnly("ECD")).isTrue();
        assertThat(ShipmentChargeTypeSupport.isRevenueCharge("DTC")).isTrue();
        assertThat(ShipmentChargeTypeSupport.usesOriginEntity("ECS")).isTrue();
        assertThat(ShipmentChargeTypeSupport.usesOriginEntity("EC3")).isFalse();
        assertThat(ShipmentChargeTypeSupport.usesOriginEntity("DT3")).isFalse();
        assertThat(ShipmentChargeTypeSupport.usesThirdPartyEntity("EC3")).isTrue();
        assertThat(ShipmentChargeTypeSupport.usesThirdPartyEntity("DT3")).isTrue();
        assertThat(ShipmentChargeTypeSupport.usesShipperCountryCurrency("DT3")).isFalse();
        assertThat(ShipmentChargeTypeSupport.usesThirdPartyCountryCurrency("DT3")).isTrue();
        assertThat(ShipmentChargeTypeSupport.usesUsdCurrency("EC3")).isTrue();
        assertThat(ShipmentChargeTypeSupport.usesOriginEntity("DTC")).isFalse();
    }

    @Test
    void processingOrder_shouldPreferCostBeforeRevenue() {
        assertThat(ShipmentChargeTypeSupport.processingOrder("ECD"))
                .isLessThan(ShipmentChargeTypeSupport.processingOrder("DTC"));
    }

    @Test
    void resolveBillingEntityCode_shouldUseOriginOrDestination() {
        Shipment shipment = new Shipment();
        shipment.setOriginEntity("DXB");
        shipment.setThirdPartyEntity("JED");
        shipment.setHAWBDestinationEntity("RUH");

        assertThat(ShipmentChargeTypeSupport.resolveBillingEntityCode(shipment, "ECS")).isEqualTo("DXB");
        assertThat(ShipmentChargeTypeSupport.resolveBillingEntityCode(shipment, "EC3")).isEqualTo("JED");
        assertThat(ShipmentChargeTypeSupport.resolveBillingEntityCode(shipment, "DT3")).isEqualTo("JED");
        assertThat(ShipmentChargeTypeSupport.resolveBillingEntityCode(shipment, "DTC")).isEqualTo("RUH");
    }

    @Test
    void resolveBillingEntityCode_shouldUseRuhForDomDduEcdAndDtc() {
        Shipment shipment = new Shipment();
        shipment.setProductGroup("DOM");
        shipment.setShipmentType("DDU");
        shipment.setHAWBDestinationEntity("JED");

        assertThat(ShipmentChargeTypeSupport.resolveBillingEntityCode(shipment, "ECD")).isEqualTo("RUH");
        assertThat(ShipmentChargeTypeSupport.resolveBillingEntityCode(shipment, "DTC")).isEqualTo("RUH");
    }

    @Test
    void resolveMasterChargeTypes_shouldReturnSingleType() {
        assertThat(ShipmentChargeTypeSupport.resolveMasterChargeTypes("DTA"))
                .containsExactly("DTA");
        assertThat(ShipmentChargeTypeSupport.resolveMasterChargeTypes("DTC"))
                .containsExactly("DTC");
        assertThat(ShipmentChargeTypeSupport.resolveMasterChargeTypes("DT3"))
                .containsExactly("DT3");
        assertThat(ShipmentChargeTypeSupport.resolveMasterChargeTypes("DTS"))
                .containsExactly("DTS");
        assertThat(ShipmentChargeTypeSupport.resolveMasterChargeTypes("ECS"))
                .containsExactly("ECS");
        assertThat(ShipmentChargeTypeSupport.resolveMasterChargeTypes(null)).containsExactly("");
    }

    @Test
    void resolveBillingEntityCode_shouldReturnEmpty_whenShipmentNull() {
        assertThat(ShipmentChargeTypeSupport.resolveBillingEntityCode(null, "DTC")).isEmpty();
    }

    @Test
    void processingOrder_shouldReturnDefaultForUnknownCode() {
        assertThat(ShipmentChargeTypeSupport.processingOrder("UNKNOWN")).isEqualTo(2);
    }

    @Test
    void copySourceChargeCode_shouldMapRevenueToCostCharges() {
        assertThat(ShipmentChargeTypeSupport.copySourceChargeCode("DTC")).contains("ECD");
        assertThat(ShipmentChargeTypeSupport.copySourceChargeCode("DTA")).contains("ECD");
        assertThat(ShipmentChargeTypeSupport.copySourceChargeCode("DTS")).contains("ECS");
        assertThat(ShipmentChargeTypeSupport.copySourceChargeCode("DT3")).contains("EC3");
        assertThat(ShipmentChargeTypeSupport.copySourceChargeCode("ECD")).isEmpty();
    }

    @Test
    void currencyFlags_shouldClassifyCifCurrencySources() {
        assertThat(ShipmentChargeTypeSupport.usesCifDestinationCountryCurrency("ECD")).isTrue();
        assertThat(ShipmentChargeTypeSupport.usesCifShipperCountryCurrency("ECS")).isTrue();
        assertThat(ShipmentChargeTypeSupport.usesCifThirdPartyCountryCurrency("DT3")).isTrue();
        assertThat(ShipmentChargeTypeSupport.usesCifShipperCountryCurrency("EC3")).isFalse();
    }
}
