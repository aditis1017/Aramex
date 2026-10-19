package com.customcharge.shipment.service.charge;

import com.customcharge.persistencelayer.dto.CurrencyConversionResult;
import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.ChargeLineItem;
import com.customcharge.persistencelayer.entities.Country;
import com.customcharge.persistencelayer.entities.Currency;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.repository.CurrencyRepository;
import com.customcharge.persistencelayer.util.ExchangeRateService;
import com.customcharge.persistencelayer.util.FixedDecimalUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShipmentChargeAmountSupportTest {

    @Mock
    private FixedDecimalUtil fixedDecimalUtil;
    @Mock
    private ObjectProvider<ExchangeRateService> exchangeRateServiceProvider;
    @Mock
    private ExchangeRateService exchangeRateService;
    @Mock
    private ShipmentCurrencyContextFactory currencyContextFactory;
    @Mock
    private CurrencyRepository currencyRepository;

    private ShipmentChargeAmountSupport support;
    private Shipment shipment;
    private Charge charge;

    @BeforeEach
    void setUp() {
        support = new ShipmentChargeAmountSupport(
                fixedDecimalUtil, exchangeRateServiceProvider, currencyContextFactory);

        shipment = new Shipment();
        shipment.setHawbNumber("HAWB-1");
        shipment.setCustomsCurrency("INR");
        shipment.setConsigneeCountryCode("SA");
        shipment.setHAWBDestinationEntity("RUH");

        charge = new Charge();
        charge.setChargeCode("ECD");

        Country saudi = new Country();
        saudi.setCode("SA");
        Currency sar = new Currency();
        sar.setCurrencyCode("SAR");
        sar.setCountry(saudi);

        lenient().when(currencyContextFactory.forShipment(shipment))
                .thenReturn(new ShipmentCurrencyContext(shipment, currencyRepository));
        lenient().when(currencyRepository.findByCountry_CodeIgnoreCaseAndActiveTrue("SA"))
                .thenReturn(Optional.of(sar));
        lenient().when(exchangeRateServiceProvider.getIfAvailable()).thenReturn(exchangeRateService);
        lenient().when(fixedDecimalUtil.convertToFixedDecimal(anyDouble(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void convertAndFormatWithRate_shouldNotConvert_whenBasisAndTargetAreDestination_forEcd() {
        ConvertedChargeAmount result = support.convertAndFormatWithRate(BigDecimal.TEN, shipment, charge);

        assertThat(result.amount()).isEqualByComparingTo("10");
        assertThat(result.exchangeRate()).isEqualTo(1.0);
        verify(exchangeRateService, never()).convertAmount(anyDouble(), anyString(), anyString());
    }

    @Test
    void convertAndFormatWithRate_shouldNotConvert_whenCustomsMatchesDestination() {
        shipment.setCustomsCurrency("SAR");

        ConvertedChargeAmount result = support.convertAndFormatWithRate(BigDecimal.TEN, shipment, charge);

        assertThat(result.amount()).isEqualByComparingTo("10");
        assertThat(result.exchangeRate()).isEqualTo(1.0);
    }

    @Test
    void convertAndFormatWithRate_shouldNotConvert_whenBasisAndTargetAreDestination_forDtc() {
        charge.setChargeCode("DTC");

        ConvertedChargeAmount result = support.convertAndFormatWithRate(BigDecimal.TEN, shipment, charge);

        assertThat(result.amount()).isEqualByComparingTo("10");
        assertThat(result.exchangeRate()).isEqualTo(1.0);
        verify(exchangeRateService, never()).convertAmount(anyDouble(), anyString(), anyString());
    }

    @Test
    void formatAmount_shouldReturnZero_whenAmountNull() {
        assertThat(support.formatAmount(null, "RUH")).isEqualByComparingTo("0");
    }

    @Test
    void formatAmount_shouldUseChargeCodeEntity() {
        shipment.setOriginEntity("JED");
        support.formatAmount(BigDecimal.TEN, shipment, "ECS");
        verify(fixedDecimalUtil).convertToFixedDecimal(10.0, "JED");
    }

    @Test
    void convertAndFormatWithRate_shouldConvert_whenCurrenciesDiffer() {
        shipment.setShipperCountryCode("SA");
        charge.setChargeCode("ECS");
        when(exchangeRateService.convertAmount(10.0, "SAR", "USD"))
                .thenReturn(new CurrencyConversionResult(2.67, 0.267));

        ConvertedChargeAmount result = support.convertAndFormatWithRate(BigDecimal.TEN, shipment, charge);

        assertThat(result.amount()).isEqualByComparingTo("2.67");
        assertThat(result.exchangeRate()).isEqualTo(0.267);
    }

    @Test
    void convertAndFormatWithRate_shouldReturnUnchanged_whenExchangeServiceUnavailable() {
        shipment.setShipperCountryCode("SA");
        charge.setChargeCode("ECS");
        when(exchangeRateServiceProvider.getIfAvailable()).thenReturn(null);

        ConvertedChargeAmount result = support.convertAndFormatWithRate(BigDecimal.TEN, shipment, charge);

        assertThat(result.amount()).isEqualByComparingTo("10");
        assertThat(result.exchangeRate()).isNull();
    }

    @Test
    void formatAmount_shouldUseEntityCodeDirectly() {
        when(fixedDecimalUtil.convertToFixedDecimal(12.5, "RUH")).thenReturn(12.5);

        assertThat(support.formatAmount(BigDecimal.valueOf(12.5), "RUH")).isEqualByComparingTo("12.5");
    }

    @Test
    void convertAndFormat_shouldReturnFormattedAmount() {
        BigDecimal result = support.convertAndFormat(BigDecimal.TEN, shipment, charge);

        assertThat(result).isEqualByComparingTo("10");
    }

    @Test
    void applyChargeHeaderAmounts_shouldPopulateChargeTotals() {
        ChargeLineItem line = new ChargeLineItem();
        line.setServiceCode("DUTY");
        line.setAmount(BigDecimal.TEN);
        line.setPreTaxableAmount(BigDecimal.TEN);

        support.applyChargeHeaderAmounts(shipment, charge, List.of(line));

        assertThat(charge.getEstimateAmount()).isNotNull();
    }
}
