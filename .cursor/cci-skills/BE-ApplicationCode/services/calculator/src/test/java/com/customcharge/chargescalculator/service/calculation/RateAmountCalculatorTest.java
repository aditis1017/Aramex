package com.customcharge.chargescalculator.service.calculation;

import com.customcharge.persistencelayer.entities.RateSheetItem;
import com.customcharge.persistencelayer.entities.RateSheetItemRate;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.enums.PercentageOf;
import com.customcharge.persistencelayer.enums.RateType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateAmountCalculatorTest {

    @Mock
    private ShipmentCostBasisCalculator shipmentCostBasisCalculator;

    private RateAmountCalculator calculator;
    private Shipment shipment;

    @BeforeEach
    void setUp() {
        calculator = new RateAmountCalculator(shipmentCostBasisCalculator);
        shipment = new Shipment();
    }

    @Test
    void calculate_shouldApplyPercentageOnCifBasis() {
        RateSheetItemRate rate = percentageRate(PercentageOf.CIF_VALUE, BigDecimal.valueOf(10));
        when(shipmentCostBasisCalculator.resolveBasis(shipment, PercentageOf.CIF_VALUE))
                .thenReturn(BigDecimal.valueOf(200));

        assertThat(calculator.calculate(rate, shipment)).isEqualByComparingTo("20.0000");
    }

    @Test
    void calculateBreakdown_shouldSplitFixedAndPercentageRates() {
        RateSheetItem item = new RateSheetItem();
        RateSheetItemRate fixed = new RateSheetItemRate();
        fixed.setRateType(RateType.FIXED);
        fixed.setValue(BigDecimal.valueOf(5));
        RateSheetItemRate percentage = percentageRate(PercentageOf.COST_OF_GOODS, BigDecimal.valueOf(10));
        item.setRates(List.of(fixed, percentage));

        var breakdown = calculator.calculateBreakdown(item, shipment);

        assertThat(breakdown.fixedPart()).isEqualByComparingTo("5.0000");
        assertThat(breakdown.percentageRates()).hasSize(1);
        assertThat(breakdown.percentageRates().get(0).ratePercent()).isEqualByComparingTo("10");
        assertThat(breakdown.percentageRates().get(0).percentageOf()).isEqualTo(PercentageOf.COST_OF_GOODS);
        assertThat(breakdown.total()).isEqualByComparingTo("5.0000");
    }

    private static RateSheetItemRate percentageRate(PercentageOf percentageOf, BigDecimal value) {
        RateSheetItemRate rate = new RateSheetItemRate();
        rate.setRateType(RateType.PERCENTAGE);
        rate.setPercentageOf(percentageOf);
        rate.setValue(value);
        return rate;
    }
}
