package com.customcharge.chargescalculator.service.calculation;

import com.customcharge.chargescalculator.model.ChargeCalculationContext;

/**
 * Pluggable charge line calculator (ECS, EC3, etc.). Implementations are selected via {@link ShipmentTypeChargeCalculatorRouter}.
 */
public interface ChargeCalculation {
    String getChargeCode();
    void calculate(ChargeCalculationContext context);
}
