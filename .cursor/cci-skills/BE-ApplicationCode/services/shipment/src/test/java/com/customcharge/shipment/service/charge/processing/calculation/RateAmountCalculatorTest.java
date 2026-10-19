package com.customcharge.shipment.service.charge.processing.calculation;

import com.customcharge.persistencelayer.entities.RateSheetItem;
import com.customcharge.persistencelayer.entities.RateSheetItemRate;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.enums.PercentageOf;
import com.customcharge.persistencelayer.enums.RateType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateAmountCalculatorTest {

    private static final String CHARGE_CODE = "ECD";

    @Mock
    private ShipmentCostBasisCalculator shipmentCostBasisCalculator;

    @InjectMocks
    private RateAmountCalculator rateAmountCalculator;

    @Test
    void calculateFixedRate_shouldReturnScaledAmountAndUnitExchangeRate() {
        RateSheetItem item = new RateSheetItem();
        RateSheetItemRate rate = new RateSheetItemRate();
        rate.setRateType(RateType.FIXED);
        rate.setValue(BigDecimal.valueOf(12.34567));
        item.setRates(List.of(rate));

        RateAmountCalculator.CalculatedRateAmount result =
                rateAmountCalculator.calculateWithBasisRate(item, new Shipment(), CHARGE_CODE);

        assertThat(result.amount()).isEqualByComparingTo("12.3457");
        assertThat(result.basisExchangeRate()).isEqualTo(1.0);
    }

    @Test
    void calculatePercentageRate_shouldUseBasis() {
        Shipment shipment = new Shipment();
        RateSheetItemRate rate = new RateSheetItemRate();
        rate.setRateType(RateType.PERCENTAGE);
        rate.setValue(BigDecimal.TEN);
        rate.setPercentageOf(PercentageOf.COST_OF_GOODS);

        when(shipmentCostBasisCalculator.resolveBasisWithRate(shipment, PercentageOf.COST_OF_GOODS, CHARGE_CODE))
                .thenReturn(new ShipmentCostBasisCalculator.ResolvedBasis(BigDecimal.valueOf(200), 4.7));

        RateAmountCalculator.CalculatedRateAmount calculated =
                rateAmountCalculator.calculateWithBasisRate(wrapRate(rate), shipment, CHARGE_CODE);

        assertThat(calculated.amount()).isEqualByComparingTo("20.0000");
        assertThat(calculated.basisExchangeRate()).isEqualTo(4.7);
        assertThat(rateAmountCalculator.calculate(rate, shipment, CHARGE_CODE))
                .isEqualByComparingTo("20.0000");
    }

    private static RateSheetItem wrapRate(RateSheetItemRate rate) {
        RateSheetItem item = new RateSheetItem();
        item.setRates(List.of(rate));
        return item;
    }

    @Test
    void calculateItem_shouldSumAllRates() {
        Shipment shipment = new Shipment();
        RateSheetItem item = new RateSheetItem();

        RateSheetItemRate fixed = new RateSheetItemRate();
        fixed.setRateType(RateType.FIXED);
        fixed.setValue(BigDecimal.ONE);

        RateSheetItemRate percentage = new RateSheetItemRate();
        percentage.setRateType(RateType.PERCENTAGE);
        percentage.setValue(BigDecimal.TEN);
        percentage.setPercentageOf(PercentageOf.CIF_VALUE);

        item.setRates(List.of(fixed, percentage));
        when(shipmentCostBasisCalculator.resolveBasisWithRate(shipment, PercentageOf.CIF_VALUE, CHARGE_CODE))
                .thenReturn(new ShipmentCostBasisCalculator.ResolvedBasis(BigDecimal.valueOf(100), 2.5));

        RateAmountCalculator.CalculatedRateAmount result =
                rateAmountCalculator.calculateWithBasisRate(item, shipment, CHARGE_CODE);

        assertThat(result.amount()).isEqualByComparingTo("11.0000");
        assertThat(result.basisExchangeRate()).isEqualTo(2.5);
    }

    @Test
    void calculateItem_shouldReturnZeroWhenRatesMissing() {
        RateSheetItem item = new RateSheetItem();
        item.setRates(List.of());

        assertThat(rateAmountCalculator.calculate(item, new Shipment(), CHARGE_CODE))
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void calculate_shouldRejectNullInputs() {
        assertThatThrownBy(() -> rateAmountCalculator.calculate((RateSheetItem) null, new Shipment(), CHARGE_CODE))
                .isInstanceOf(NullPointerException.class);
    }
}
