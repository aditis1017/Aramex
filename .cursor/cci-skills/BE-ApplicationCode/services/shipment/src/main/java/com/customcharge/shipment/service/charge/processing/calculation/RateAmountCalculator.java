package com.customcharge.shipment.service.charge.processing.calculation;

import com.customcharge.persistencelayer.entities.RateSheetItem;
import com.customcharge.persistencelayer.entities.RateSheetItemRate;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.enums.RateType;
import com.customcharge.shipment.constants.ShipmentChargeTypeSupport;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class RateAmountCalculator {

    private static final int AMOUNT_SCALE = 4;
    private static final double UNIT_EXCHANGE_RATE = 1.0;

    ShipmentCostBasisCalculator shipmentCostBasisCalculator;

    public record CalculatedRateAmount(BigDecimal amount, Double basisExchangeRate) {
    }

    public BigDecimal calculate(RateSheetItem item, Shipment shipment, String chargeCode) {
        return calculateWithBasisRate(item, shipment, chargeCode).amount();
    }

    public CalculatedRateAmount calculateWithBasisRate(RateSheetItem item, Shipment shipment, String chargeCode) {
        Objects.requireNonNull(item, "rateSheetItem is required");
        if (item.getRates() == null || item.getRates().isEmpty()) {
            return new CalculatedRateAmount(BigDecimal.ZERO, null);
        }

        BigDecimal total = BigDecimal.ZERO;
        Double basisExchangeRate = null;
        for (RateSheetItemRate rate : item.getRates()) {
            RateCalculation rateCalculation = calculateRateWithBasisRate(rate, shipment, chargeCode);
            total = total.add(rateCalculation.amount());
            if (rateCalculation.basisExchangeRate() != null) {
                basisExchangeRate = rateCalculation.basisExchangeRate();
            }
        }
        if (basisExchangeRate == null
                && ShipmentChargeTypeSupport.usesDestinationCountryCurrency(
                        ShipmentChargeTypeSupport.normalize(chargeCode))) {
            basisExchangeRate = UNIT_EXCHANGE_RATE;
        }
        return new CalculatedRateAmount(total, basisExchangeRate);
    }

    public BigDecimal calculate(RateSheetItemRate rate, Shipment shipment, String chargeCode) {
        return calculateRateWithBasisRate(rate, shipment, chargeCode).amount();
    }

    private RateCalculation calculateRateWithBasisRate(
            RateSheetItemRate rate, Shipment shipment, String chargeCode) {
        Objects.requireNonNull(rate, "rateSheetItemRate is required");
        BigDecimal rateValue = Optional.ofNullable(rate.getValue()).orElse(BigDecimal.ZERO);

        if (rate.getRateType() == RateType.FIXED) {
            return new RateCalculation(
                    rateValue.setScale(AMOUNT_SCALE, RoundingMode.HALF_UP),
                    fixedRateExchangeRate(chargeCode));
        }
        if (rate.getRateType() == RateType.PERCENTAGE) {
            ShipmentCostBasisCalculator.ResolvedBasis basis = shipmentCostBasisCalculator.resolveBasisWithRate(
                    shipment, rate.getPercentageOf(), chargeCode);
            BigDecimal amount = basis.amount().multiply(rateValue)
                    .divide(BigDecimal.valueOf(100), AMOUNT_SCALE, RoundingMode.HALF_UP);
            return new RateCalculation(amount, basis.exchangeRate());
        }
        return new RateCalculation(BigDecimal.ZERO, null);
    }

    private static Double fixedRateExchangeRate(String chargeCode) {
        if (ShipmentChargeTypeSupport.usesDestinationCountryCurrency(
                ShipmentChargeTypeSupport.normalize(chargeCode))) {
            return UNIT_EXCHANGE_RATE;
        }
        return null;
    }

    private record RateCalculation(BigDecimal amount, Double basisExchangeRate) {
    }
}
