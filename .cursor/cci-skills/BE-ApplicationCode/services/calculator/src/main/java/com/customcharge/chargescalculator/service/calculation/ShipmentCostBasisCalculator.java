package com.customcharge.chargescalculator.service.calculation;

import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.enums.PercentageOf;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Component
public class ShipmentCostBasisCalculator {

    private static final int MONETARY_SCALE = 4;

    public BigDecimal resolveBasis(Shipment shipment, PercentageOf percentageOf) {
        if (shipment == null || percentageOf == null) {
            return BigDecimal.ZERO;
        }
        return switch (percentageOf) {
            case COST_OF_GOODS -> toBigDecimal(shipment.getCostOfGoods());
            case CIF_VALUE -> cifFinalValueInCustomsCurrency(shipment);
        };
    }

    private BigDecimal cifFinalValueInCustomsCurrency(Shipment shipment) {
        BigDecimal costOfGoods = toBigDecimal(shipment.getCostOfGoods());
        BigDecimal cifValue = toBigDecimal(shipment.getCifValue());
        if (cifValue.compareTo(BigDecimal.ZERO) <= 0) {
            return costOfGoods.setScale(MONETARY_SCALE, RoundingMode.HALF_UP);
        }
        return costOfGoods.add(cifValue).setScale(MONETARY_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal toBigDecimal(Double value) {
        return Optional.ofNullable(value)
                .map(v -> BigDecimal.valueOf(v).setScale(MONETARY_SCALE, RoundingMode.HALF_UP))
                .orElse(BigDecimal.ZERO);
    }
}
