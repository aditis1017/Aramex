package com.customcharge.shipment.service.cif;

import com.customcharge.persistencelayer.dto.CurrencyConversionResult;
import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.repository.CurrencyRepository;
import com.customcharge.persistencelayer.util.ExchangeRateService;
import com.customcharge.shipment.config.ShipmentProperties;
import com.customcharge.shipment.service.charge.ShipmentCurrencyContext;
import com.customcharge.shipment.service.charge.ShipmentCurrencyContextFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChargeCifValueSupportTest {

    @Mock
    private ObjectProvider<ExchangeRateService> exchangeRateServiceProvider;
    @Mock
    private ExchangeRateService exchangeRateService;
    @Mock
    private ShipmentCurrencyContextFactory currencyContextFactory;
    @Mock
    private CurrencyRepository currencyRepository;
    @Mock
    private ShipmentProperties shipmentProperties;

    @InjectMocks
    private ChargeCifValueSupport support;

    @BeforeEach
    void setUp() {
        lenient().when(shipmentProperties.getMonetaryScale()).thenReturn(4);
        lenient().when(exchangeRateServiceProvider.getIfAvailable()).thenReturn(exchangeRateService);
        lenient().when(currencyContextFactory.forShipment(org.mockito.ArgumentMatchers.any(Shipment.class)))
                .thenAnswer(invocation -> new ShipmentCurrencyContext(invocation.getArgument(0), currencyRepository));
    }

    @Test
    void applySh013CogSnapshot_shouldSetZero_whenCostOfGoodsMissing() {
        Shipment shipment = shipmentWithCountries("AE", "GB", null);
        shipment.setCostOfGoods(null);
        shipment.setCustomsCurrency("INR");
        Charge charge = charge("ECS", "USD");

        support.applySh013CogSnapshot(shipment, charge);

        assertThat(charge.getCifValue()).isEqualTo(0.0);
        assertThat(charge.getCifCurrencyRate()).isNull();
        verify(exchangeRateService, never()).convertAmount(org.mockito.ArgumentMatchers.anyDouble(), eq("INR"), eq("GBP"));
    }

    @Test
    void applySh013CogSnapshot_shouldSetZero_whenCustomsCurrencyMissing() {
        Shipment shipment = shipmentWithCountries("AE", "GB", null);
        shipment.setCostOfGoods(100.0);
        shipment.setCustomsCurrency(null);
        Charge charge = charge("ECS", "USD");

        support.applySh013CogSnapshot(shipment, charge);

        assertThat(charge.getCifValue()).isEqualTo(0.0);
        assertThat(charge.getCifCurrencyRate()).isNull();
    }

    @Test
    void applySh013CogSnapshot_shouldConvertCostOfGoodsIntoCifCurrency_notChargeCurrency() {
        Shipment shipment = shipmentWithCountries("AE", "GB", null);
        shipment.setCostOfGoods(150.0);
        shipment.setCustomsCurrency("INR");
        Charge charge = charge("ECS", "USD");
        stubCurrency("GB", "GBP");
        when(exchangeRateService.convertAmount(150.0, "INR", "GBP"))
                .thenReturn(new CurrencyConversionResult(1.8, 0.012));

        support.applySh013CogSnapshot(shipment, charge);

        assertThat(charge.getCifValue()).isEqualTo(1.8);
        assertThat(charge.getCifCurrency()).isEqualTo("GBP");
        assertThat(charge.getCifCurrencyRate()).isEqualTo(0.012);
        assertThat(charge.getCurrency()).isEqualTo("USD");
    }

    @Test
    void applySh013CogSnapshot_shouldUseDestinationCurrency_forDtaCharge() {
        Shipment shipment = shipmentWithCountries("AE", "GB", null);
        shipment.setCostOfGoods(50.0);
        shipment.setCustomsCurrency("AED");
        Charge charge = charge("DTA", "USD");
        stubCurrency("AE", "AED");

        support.applySh013CogSnapshot(shipment, charge);

        assertThat(charge.getCifValue()).isEqualTo(50.0);
        assertThat(charge.getCifCurrency()).isEqualTo("AED");
        assertThat(charge.getCifCurrencyRate()).isEqualTo(1.0);
        verify(exchangeRateService, never()).convertAmount(org.mockito.ArgumentMatchers.anyDouble(), eq("AED"), eq("AED"));
    }

    @Test
    void applySh013CogSnapshot_shouldUseUnitRate_whenCurrenciesMatch() {
        Shipment shipment = shipmentWithCountries("AE", "US", null);
        shipment.setCostOfGoods(50.0);
        shipment.setCustomsCurrency("USD");
        Charge charge = charge("ECS", "USD");
        stubCurrency("US", "USD");

        support.applySh013CogSnapshot(shipment, charge);

        assertThat(charge.getCifValue()).isEqualTo(50.0);
        assertThat(charge.getCifCurrency()).isEqualTo("USD");
        assertThat(charge.getCifCurrencyRate()).isEqualTo(1.0);
        verify(exchangeRateService, never()).convertAmount(org.mockito.ArgumentMatchers.anyDouble(), eq("USD"), eq("USD"));
    }

    @Test
    void addSh267CifContribution_shouldZeroShippingChargesAndUseCostOfGoodsOnly_whenCifAmountZero() {
        Shipment shipment = shipmentWithCountries("AE", "GB", null);
        shipment.setCostOfGoods(100.0);
        shipment.setCustomsCurrency("TND");
        shipment.setCifValue(0.0);
        shipment.setCifValueCurrency("TND");
        Charge charge = charge("ECS", "USD");
        charge.setShippingCharges(25.0);
        charge.setCifValue(125.0);
        charge.setCifCurrency("GBP");
        stubCurrency("GB", "GBP");
        when(exchangeRateService.convertAmount(100.0, "TND", "GBP"))
                .thenReturn(new CurrencyConversionResult(30.0, 0.3));

        support.addSh267CifContribution(shipment, charge, 0.0, "TND");

        assertThat(charge.getShippingCharges()).isEqualTo(0.0);
        assertThat(charge.getCifValue()).isEqualTo(30.0);
        assertThat(charge.getCifCurrency()).isEqualTo("GBP");
        assertThat(charge.getCifCurrencyRate()).isEqualTo(0.3);
    }

    @Test
    void addSh267CifContribution_shouldSumShipmentCogsAndCif_thenConvertToChargeCifCurrency() {
        Shipment shipment = shipmentWithCountries("AE", "GB", null);
        shipment.setCostOfGoods(100.0);
        shipment.setCustomsCurrency("TND");
        shipment.setCifValue(40.0);
        shipment.setCifValueCurrency("TND");
        Charge charge = charge("ECS", "USD");
        charge.setCifValue(10.0);
        charge.setCifCurrency("GBP");
        charge.setCifCurrencyRate(0.5);
        stubCurrency("GB", "GBP");
        when(exchangeRateService.convertAmount(140.0, "TND", "GBP"))
                .thenReturn(new CurrencyConversionResult(42.0, 0.3));
        when(exchangeRateService.convertAmount(40.0, "TND", "GBP"))
                .thenReturn(new CurrencyConversionResult(12.0, 0.3));

        support.addSh267CifContribution(shipment, charge, 40.0, "TND");

        assertThat(charge.getCifValue()).isEqualTo(42.0);
        assertThat(charge.getCifCurrency()).isEqualTo("GBP");
        assertThat(charge.getCifCurrencyRate()).isEqualTo(0.3);
        assertThat(charge.getShippingCharges()).isEqualTo(12.0);
        assertThat(charge.getCurrency()).isEqualTo("USD");
        verify(exchangeRateService, never()).convertAmount(org.mockito.ArgumentMatchers.anyDouble(), eq("USD"), eq("GBP"));
    }

    @Test
    void addSh267CifContribution_shouldConvertShipmentCogsOnly_whenShipmentCifMissing() {
        Shipment shipment = shipmentWithCountries("AE", "GB", null);
        shipment.setCostOfGoods(50.0);
        shipment.setCustomsCurrency("TND");
        shipment.setCifValue(null);
        shipment.setCifValueCurrency("TND");
        Charge charge = charge("ECS", "USD");
        charge.setCifValue(12.5);
        charge.setCifCurrency("GBP");
        stubCurrency("GB", "GBP");
        when(exchangeRateService.convertAmount(50.0, "TND", "GBP"))
                .thenReturn(new CurrencyConversionResult(15.0, 0.3));
        when(exchangeRateService.convertAmount(40.0, "TND", "GBP"))
                .thenReturn(new CurrencyConversionResult(12.0, 0.3));

        support.addSh267CifContribution(shipment, charge, 40.0, "TND");

        assertThat(charge.getCifValue()).isEqualTo(15.0);
        assertThat(charge.getCifCurrency()).isEqualTo("GBP");
        assertThat(charge.getCifCurrencyRate()).isEqualTo(0.3);
        assertThat(charge.getShippingCharges()).isEqualTo(12.0);
    }

    @Test
    void addSh267CifContribution_shouldAlignCifToCustomsCurrency_beforeSumming() {
        Shipment shipment = shipmentWithCountries("AE", "GB", null);
        shipment.setCostOfGoods(100.0);
        shipment.setCustomsCurrency("TND");
        shipment.setCifValue(10.0);
        shipment.setCifValueCurrency("USD");
        Charge charge = charge("ECS", "USD");
        stubCurrency("GB", "GBP");
        when(exchangeRateService.convertAmount(10.0, "USD", "TND"))
                .thenReturn(new CurrencyConversionResult(31.5, 3.15));
        when(exchangeRateService.convertAmount(131.5, "TND", "GBP"))
                .thenReturn(new CurrencyConversionResult(39.45, 0.3));
        when(exchangeRateService.convertAmount(10.0, "USD", "GBP"))
                .thenReturn(new CurrencyConversionResult(7.9, 0.79));

        support.addSh267CifContribution(shipment, charge, 10.0, "USD");

        assertThat(charge.getCifValue()).isEqualTo(39.45);
        assertThat(charge.getCifCurrency()).isEqualTo("GBP");
        assertThat(charge.getCifCurrencyRate()).isEqualTo(0.3);
        assertThat(charge.getShippingCharges()).isEqualTo(7.9);
    }

    @Test
    void applySh013CogSnapshot_shouldNoOp_whenInputsMissing() {
        support.applySh013CogSnapshot(null, new Charge());
        support.applySh013CogSnapshot(new Shipment(), null);
    }

    @Test
    void applySh267ShippingChargesSnapshot_shouldStoreConvertedShippingCharges() {
        Shipment shipment = shipmentWithCountries("AE", "US", null);
        Charge charge = charge("ECS", "USD");
        stubCurrency("US", "USD");
        when(exchangeRateService.convertAmount(20.0, "TND", "USD"))
                .thenReturn(new CurrencyConversionResult(6.5, 0.325));

        support.applySh267ShippingChargesSnapshot(shipment, charge, 20.0, "TND");

        assertThat(charge.getShippingCharges()).isEqualTo(6.5);
        assertThat(charge.getCifCurrency()).isEqualTo("USD");
    }

    @Test
    void applySh267ShippingChargesSnapshot_shouldZero_whenAmountMissing() {
        Shipment shipment = shipmentWithCountries("AE", "US", null);
        Charge charge = charge("ECS", "USD");
        stubCurrency("US", "USD");

        support.applySh267ShippingChargesSnapshot(shipment, charge, 0.0, "TND");

        assertThat(charge.getShippingCharges()).isEqualTo(0.0);
    }

    @Test
    void addSh267CifContribution_shouldNoOp_whenInputsMissing() {
        support.addSh267CifContribution(null, new Charge(), 10.0, "TND");
        support.addSh267CifContribution(new Shipment(), null, 10.0, "TND");
    }

    @Test
    void addSh267CifContribution_shouldZeroCif_whenCustomsCurrencyMissing() {
        Shipment shipment = shipmentWithCountries("AE", "US", null);
        shipment.setCostOfGoods(100.0);
        shipment.setCustomsCurrency(null);
        Charge charge = charge("ECS", "USD");
        stubCurrency("US", "USD");

        support.addSh267CifContribution(shipment, charge, 10.0, "USD");

        assertThat(charge.getCifValue()).isEqualTo(0.0);
        assertThat(charge.getCifCurrencyRate()).isNull();
    }

    @Test
    void applySh013CogSnapshot_shouldReturnZeroConversion_whenExchangeServiceUnavailable() {
        Shipment shipment = shipmentWithCountries("AE", "US", null);
        shipment.setCostOfGoods(100.0);
        shipment.setCustomsCurrency("INR");
        Charge charge = charge("ECS", "USD");
        stubCurrency("US", "USD");
        when(exchangeRateServiceProvider.getIfAvailable()).thenReturn(null);

        support.applySh013CogSnapshot(shipment, charge);

        assertThat(charge.getCifValue()).isEqualTo(0.0);
    }

    private static Shipment shipmentWithCountries(String consignee, String shipper, String thirdParty) {
        Shipment shipment = new Shipment();
        shipment.setConsigneeCountryCode(consignee);
        shipment.setShipperCountryCode(shipper);
        shipment.setThirdPartyCountry(thirdParty);
        return shipment;
    }

    private static Charge charge(String chargeCode, String currency) {
        Charge charge = new Charge();
        charge.setChargeCode(chargeCode);
        charge.setCurrency(currency);
        return charge;
    }

    private void stubCurrency(String countryCode, String currencyCode) {
        com.customcharge.persistencelayer.entities.Currency currency =
                new com.customcharge.persistencelayer.entities.Currency();
        currency.setCurrencyCode(currencyCode);
        when(currencyRepository.findByCountry_CodeIgnoreCaseAndActiveTrue(countryCode)).thenReturn(java.util.Optional.of(currency));
    }
}
