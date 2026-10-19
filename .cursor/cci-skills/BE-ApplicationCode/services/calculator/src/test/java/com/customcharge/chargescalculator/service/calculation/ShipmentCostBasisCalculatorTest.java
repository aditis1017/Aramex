package com.customcharge.chargescalculator.service.calculation;

import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.enums.PercentageOf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ShipmentCostBasisCalculatorTest {

    private ShipmentCostBasisCalculator calculator;
    private Shipment shipment;

    @BeforeEach
    void setUp() {
        calculator = new ShipmentCostBasisCalculator();
        shipment = new Shipment();
        shipment.setCostOfGoods(100.0);
        shipment.setCustomsCurrency("SAR");
    }

    @Test
    void resolveBasis_shouldReturnCostOfGoods_forCostOfGoodsRule() {
        assertThat(calculator.resolveBasis(shipment, PercentageOf.COST_OF_GOODS))
                .isEqualByComparingTo("100.0000");
    }

    @Test
    void resolveBasis_shouldAddCifAndCostOfGoods_whenCifCurrencyMatchesCustoms() {
        shipment.setCifValue(50.0);
        shipment.setCifValueCurrency("SAR");

        assertThat(calculator.resolveBasis(shipment, PercentageOf.CIF_VALUE))
                .isEqualByComparingTo("150.0000");
    }

    @Test
    void resolveBasis_shouldAddCifWithoutFx_whenCifCurrencyDiffersFromCustoms() {
        shipment.setCifValue(10.0);
        shipment.setCifValueCurrency("USD");

        assertThat(calculator.resolveBasis(shipment, PercentageOf.CIF_VALUE))
                .isEqualByComparingTo("110.0000");
    }

    @Test
    void resolveBasis_shouldUseCostOfGoodsOnly_whenCifValueMissing() {
        shipment.setCifValue(null);

        assertThat(calculator.resolveBasis(shipment, PercentageOf.CIF_VALUE))
                .isEqualByComparingTo("100.0000");
    }

    @Test
    void resolveBasis_shouldReturnZero_whenShipmentNull() {
        assertThat(calculator.resolveBasis(null, PercentageOf.CIF_VALUE)).isEqualByComparingTo("0");
    }
}
