package com.customcharge.chargescalculator.service.calculation.impl;

import com.customcharge.chargescalculator.constants.BatchProcessingConstants;
import com.customcharge.chargescalculator.model.ChargeCalculationContext;
import com.customcharge.chargescalculator.service.calculation.ChargeCalculation;
import com.customcharge.chargescalculator.service.charge.ActualChargeCreationService;
import com.customcharge.chargescalculator.service.charge.EstimateChargeCreationService;
import org.springframework.stereotype.Component;

@Component
public class EcdChargeCalculation implements ChargeCalculation {

    private final EstimateChargeCreationService estimateChargeCreationService;
    private final ActualChargeCreationService actualChargeCreationService;

    public EcdChargeCalculation(
            EstimateChargeCreationService estimateChargeCreationService,
            ActualChargeCreationService actualChargeCreationService) {
        this.estimateChargeCreationService = estimateChargeCreationService;
        this.actualChargeCreationService = actualChargeCreationService;
    }

    @Override
    public String getChargeCode() {
        return "ECD";
    }

    @Override
    public void calculate(ChargeCalculationContext context) {
        if (BatchProcessingConstants.PROCESS_FOR_ACTUAL.equalsIgnoreCase(context.getProcessFor())) {
            actualChargeCreationService.createOrUpdateActualCharge(context, getChargeCode());
        } else {
            estimateChargeCreationService.createOrUpdateEstimateCharge(context, getChargeCode());
        }
    }
}
