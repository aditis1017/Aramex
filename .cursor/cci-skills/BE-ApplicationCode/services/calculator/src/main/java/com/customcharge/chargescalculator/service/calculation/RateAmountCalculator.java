package com.customcharge.chargescalculator.service.calculation;

import com.customcharge.chargescalculator.constants.ChargeCreationConstants;
import com.customcharge.chargescalculator.service.ratesheet.PercentageRateSpec;
import com.customcharge.chargescalculator.service.ratesheet.RateSheetAmountBreakdown;
import com.customcharge.persistencelayer.entities.RateSheetItem;
import com.customcharge.persistencelayer.entities.RateSheetItemRate;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.enums.RateType;
import com.customcharge.persistencelayer.exception.ValidationException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class RateAmountCalculator {

    private static final int AMOUNT_SCALE = 4;

    ShipmentCostBasisCalculator shipmentCostBasisCalculator;

    public BigDecimal calculate(RateSheetItem item, Shipment shipment) {
        return calculateBreakdown(item, shipment).total();
    }

    public RateSheetAmountBreakdown calculateBreakdown(RateSheetItem item, Shipment shipment) {
        Objects.requireNonNull(item, "rateSheetItem is required");
        if (item.getRates() == null || item.getRates().isEmpty()) {
            return RateSheetAmountBreakdown.zero();
        }
        BigDecimal fixedTotal = BigDecimal.ZERO;
        List<PercentageRateSpec> percentageRates = new ArrayList<>();
        for (RateSheetItemRate rate : item.getRates()) {
            if (rate == null) {
                throw new ValidationException(ChargeCreationConstants.INVALID_RATE_SHEET_RATE);
            }
            if (rate.getRateType() == RateType.FIXED) {
                fixedTotal = fixedTotal.add(calculate(rate, shipment));
            } else if (rate.getRateType() == RateType.PERCENTAGE) {
                BigDecimal rateValue = Optional.ofNullable(rate.getValue()).orElse(BigDecimal.ZERO);
                percentageRates.add(new PercentageRateSpec(rateValue, rate.getPercentageOf()));
            }
        }
        return new RateSheetAmountBreakdown(fixedTotal, percentageRates);
    }

    public BigDecimal calculate(RateSheetItemRate rate, Shipment shipment) {
        Objects.requireNonNull(rate, "rateSheetItemRate is required");
        BigDecimal rateValue = Optional.ofNullable(rate.getValue()).orElse(BigDecimal.ZERO);

        if (rate.getRateType() == RateType.FIXED) {
            return rateValue.setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
        }
        if (rate.getRateType() == RateType.PERCENTAGE) {
            BigDecimal basis = shipmentCostBasisCalculator.resolveBasis(shipment, rate.getPercentageOf());
            return basis.multiply(rateValue)
                    .divide(BigDecimal.valueOf(100), AMOUNT_SCALE, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }
}
