package com.customcharge.shipment.service.charge.processing.calculation;

import com.customcharge.persistencelayer.dto.CurrencyConversionResult;
import com.customcharge.persistencelayer.entities.Country;
import com.customcharge.persistencelayer.entities.Currency;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.enums.PercentageOf;
import com.customcharge.persistencelayer.repository.CurrencyRepository;
import com.customcharge.persistencelayer.util.ExchangeRateService;
import com.customcharge.shipment.config.ShipmentProperties;
import com.customcharge.shipment.service.charge.ShipmentCurrencyContext;
import com.customcharge.shipment.service.charge.ShipmentCurrencyContextFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShipmentCostBasisCalculatorTest {

    @Mock
    private ShipmentCurrencyContextFactory currencyContextFactory;
    @Mock
    private ObjectProvider<ExchangeRateService> exchangeRateServiceProvider;
    @Mock
    private ExchangeRateService exchangeRateService;
    @Mock
    private CurrencyRepository currencyRepository;

    private ShipmentCostBasisCalculator calculator;

    @BeforeEach
    void setUp() {
        ShipmentProperties shipmentProperties = new ShipmentProperties();
        calculator = new ShipmentCostBasisCalculator(shipmentProperties, currencyContextFactory, exchangeRateServiceProvider);
        lenient().when(currencyContextFactory.forShipment(any())).thenAnswer(invocation ->
                new ShipmentCurrencyContext(invocation.getArgument(0), currencyRepository));
        lenient().when(exchangeRateServiceProvider.getIfAvailable()).thenReturn(exchangeRateService);

        Country saudi = new Country();
        saudi.setCode("SA");
        Currency sar = new Currency();
        sar.setCurrencyCode("SAR");
        sar.setCountry(saudi);
        lenient().when(currencyRepository.findByCountry_CodeIgnoreCaseAndActiveTrue("SA"))
                .thenReturn(Optional.of(sar));
    }

    @Test
    void resolveBasis_shouldConvertCostOfGoodsToDestinationCurrency_forEcd() {
        Shipment shipment = new Shipment();
        shipment.setCostOfGoods(100.0);
        shipment.setCustomsCurrency("GBP");
        shipment.setConsigneeCountryCode("SA");
        when(exchangeRateService.convertAmount(100.0, "GBP", "SAR"))
                .thenReturn(new CurrencyConversionResult(470.0, 4.7));

        ShipmentCostBasisCalculator.ResolvedBasis basis =
                calculator.resolveBasisWithRate(shipment, PercentageOf.COST_OF_GOODS, "ECD");

        assertThat(basis.amount()).isEqualByComparingTo("470.0000");
        assertThat(basis.exchangeRate()).isEqualTo(4.7);
        assertThat(calculator.resolveBasis(shipment, PercentageOf.COST_OF_GOODS, "ECD"))
                .isEqualByComparingTo("470.0000");
    }

    @Test
    void resolveBasis_shouldUseUnitExchangeRate_whenCustomsMatchesDestination() {
        Shipment shipment = new Shipment();
        shipment.setCostOfGoods(100.0);
        shipment.setCustomsCurrency("SAR");
        shipment.setConsigneeCountryCode("SA");

        ShipmentCostBasisCalculator.ResolvedBasis basis =
                calculator.resolveBasisWithRate(shipment, PercentageOf.COST_OF_GOODS, "ECD");

        assertThat(basis.amount()).isEqualByComparingTo("100.0000");
        assertThat(basis.exchangeRate()).isEqualTo(1.0);
    }

    @Test
    void resolveBasis_shouldUseCostOfGoodsForCifWhenCifNotPositive() {
        Shipment shipment = new Shipment();
        shipment.setCostOfGoods(100.0);
        shipment.setCollectAmount(50.0);
        shipment.setCifValue(0.0);
        shipment.setCifValueCurrency("TND");
        shipment.setCustomsCurrency("TND");

        assertThat(calculator.resolveBasis(shipment, PercentageOf.COST_OF_GOODS, "ECD"))
                .isEqualByComparingTo("100.0000");
        assertThat(calculator.resolveBasis(shipment, PercentageOf.CIF_VALUE, "ECD"))
                .isEqualByComparingTo("100.0000");
    }

    @Test
    void resolveBasis_shouldAddCifToCostOfGoodsWhenCifPositive() {
        Shipment shipment = new Shipment();
        shipment.setCostOfGoods(100.0);
        shipment.setCifValue(25.0);
        shipment.setCifValueCurrency("TND");
        shipment.setCustomsCurrency("TND");

        assertThat(calculator.resolveBasis(shipment, PercentageOf.CIF_VALUE, "ECD"))
                .isEqualByComparingTo("125.0000");
    }

    @Test
    void resolveBasis_shouldReturnZeroWhenInputMissing() {
        assertThat(calculator.resolveBasis(null, PercentageOf.COST_OF_GOODS, "ECD"))
                .isEqualByComparingTo(BigDecimal.ZERO);
    }
}
