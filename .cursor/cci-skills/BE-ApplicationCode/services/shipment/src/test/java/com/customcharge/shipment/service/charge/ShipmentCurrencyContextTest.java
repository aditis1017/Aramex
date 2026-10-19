package com.customcharge.shipment.service.charge;

import com.customcharge.persistencelayer.entities.Country;
import com.customcharge.persistencelayer.entities.Currency;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.repository.CurrencyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShipmentCurrencyContextTest {

    @Mock
    private CurrencyRepository currencyRepository;

    private Shipment shipment;

    @BeforeEach
    void setUp() {
        shipment = new Shipment();
        shipment.setCustomsCurrency("INR");
        shipment.setConsigneeCountryCode("SA");

        Country saudi = new Country();
        saudi.setCode("SA");
        Currency sar = new Currency();
        sar.setCurrencyCode("SAR");
        sar.setCountry(saudi);
        lenient().when(currencyRepository.findByCountry_CodeIgnoreCaseAndActiveTrue(anyString()))
                .thenReturn(Optional.of(sar));
    }

    @Test
    void rateSheetBasisCurrency_shouldUseDestinationCurrency_forEcdAndDtc() {
        ShipmentCurrencyContext context = new ShipmentCurrencyContext(shipment, currencyRepository);

        assertThat(context.rateSheetBasisCurrency("ECD")).isEqualTo("SAR");
        assertThat(context.rateSheetFromCurrency("ECD")).isEqualTo("SAR");
        assertThat(context.rateSheetBasisCurrency("DTC")).isEqualTo("SAR");
        assertThat(context.rateSheetFromCurrency("DTC")).isEqualTo("SAR");
    }

    @Test
    void chargeTargetCurrency_shouldUseDestinationCurrency_forEcdAndDtc() {
        ShipmentCurrencyContext context = new ShipmentCurrencyContext(shipment, currencyRepository);

        assertThat(context.chargeTargetCurrency("ECD")).isEqualTo("SAR");
        assertThat(context.chargeTargetCurrency("DTC")).isEqualTo("SAR");
    }

    @Test
    void rateSheetBasisCurrency_shouldUseDestination_whenCustomsMissing() {
        shipment.setCustomsCurrency(null);
        ShipmentCurrencyContext context = new ShipmentCurrencyContext(shipment, currencyRepository);

        assertThat(context.rateSheetBasisCurrency("ECD")).isEqualTo("SAR");
    }

    @Test
    void chargeTargetCurrency_shouldUseUsd_forDta() {
        ShipmentCurrencyContext context = new ShipmentCurrencyContext(shipment, currencyRepository);

        assertThat(context.chargeTargetCurrency("DTA")).isEqualTo("USD");
    }

    @Test
    void cifTargetCurrency_shouldUseShipperCurrency_forEcs() {
        shipment.setShipperCountryCode("SA");
        ShipmentCurrencyContext context = new ShipmentCurrencyContext(shipment, currencyRepository);

        assertThat(context.cifTargetCurrency("ECS")).isEqualTo("SAR");
    }

    @Test
    void customsCurrency_shouldNormalizeValue() {
        ShipmentCurrencyContext context = new ShipmentCurrencyContext(shipment, currencyRepository);

        assertThat(context.customsCurrency()).isEqualTo("INR");
    }

    @Test
    void shipperCurrency_shouldResolveFromCountry() {
        shipment.setShipperCountryCode("AE");
        ShipmentCurrencyContext context = new ShipmentCurrencyContext(shipment, currencyRepository);

        assertThat(context.shipperCurrency()).isEqualTo("SAR");
    }

    @Test
    void thirdPartyCurrency_shouldResolveFromCountry() {
        shipment.setThirdPartyCountry("AE");
        ShipmentCurrencyContext context = new ShipmentCurrencyContext(shipment, currencyRepository);

        assertThat(context.thirdPartyCurrency()).isEqualTo("SAR");
    }

    @Test
    void chargeTargetCurrency_shouldUseShipperCurrency_forDts() {
        shipment.setShipperCountryCode("SA");
        ShipmentCurrencyContext context = new ShipmentCurrencyContext(shipment, currencyRepository);

        assertThat(context.chargeTargetCurrency("DTS")).isEqualTo("SAR");
    }

    @Test
    void rateSheetBasisCurrency_shouldUseShipperCurrency_forEcs() {
        shipment.setShipperCountryCode("SA");
        ShipmentCurrencyContext context = new ShipmentCurrencyContext(shipment, currencyRepository);

        assertThat(context.rateSheetBasisCurrency("ECS")).isEqualTo("SAR");
    }

    @Test
    void cifTargetCurrency_shouldUseThirdPartyCurrency_forDt3() {
        shipment.setThirdPartyCountry("SA");
        ShipmentCurrencyContext context = new ShipmentCurrencyContext(shipment, currencyRepository);

        assertThat(context.cifTargetCurrency("DT3")).isEqualTo("SAR");
    }

    @Test
    void rateSheetBasisCurrency_shouldUseUsd_forEc3() {
        shipment.setThirdPartyCountry("SA");
        ShipmentCurrencyContext context = new ShipmentCurrencyContext(shipment, currencyRepository);

        assertThat(context.rateSheetBasisCurrency("EC3")).isEqualTo("USD");
        assertThat(context.chargeTargetCurrency("EC3")).isEqualTo("USD");
    }

    @Test
    void chargeTargetCurrency_shouldUseThirdPartyCurrency_forDt3() {
        shipment.setThirdPartyCountry("SA");
        ShipmentCurrencyContext context = new ShipmentCurrencyContext(shipment, currencyRepository);

        assertThat(context.chargeTargetCurrency("DT3")).isEqualTo("SAR");
    }

    @Test
    void cifTargetCurrency_shouldUseUsd_forEc3() {
        shipment.setShipperCountryCode("SA");
        ShipmentCurrencyContext context = new ShipmentCurrencyContext(shipment, currencyRepository);

        assertThat(context.cifTargetCurrency("EC3")).isEqualTo("USD");
    }
}
