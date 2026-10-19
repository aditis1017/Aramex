package com.customcharge.chargescalculator.service.calculation.impl;

import com.customcharge.chargescalculator.constants.BatchProcessingConstants;
import com.customcharge.chargescalculator.constants.ChargeCreationConstants;
import com.customcharge.chargescalculator.model.ChargeCalculationContext;
import com.customcharge.chargescalculator.service.calculation.ChargeCalculation;
import com.customcharge.chargescalculator.service.charge.ChargeLineSupport;
import com.customcharge.chargescalculator.service.charge.PaymentGatedEstimateChargeSupport;
import org.springframework.stereotype.Component;

@Component
public class Dt3ChargeCalculation implements ChargeCalculation {

    private final PaymentGatedEstimateChargeSupport paymentGatedEstimateChargeSupport;
    private final ChargeLineSupport chargeLineSupport;

    public Dt3ChargeCalculation(
            PaymentGatedEstimateChargeSupport paymentGatedEstimateChargeSupport,
            ChargeLineSupport chargeLineSupport) {
        this.paymentGatedEstimateChargeSupport = paymentGatedEstimateChargeSupport;
        this.chargeLineSupport = chargeLineSupport;
    }

    @Override
    public String getChargeCode() {
        return BatchProcessingConstants.CHARGE_CODE_DT3;
    }

    @Override
    public void calculate(ChargeCalculationContext context) {
        if (BatchProcessingConstants.PROCESS_FOR_ACTUAL.equalsIgnoreCase(context.getProcessFor())) {
            paymentGatedEstimateChargeSupport.createActualIfPaymentType(
                    context,
                    BatchProcessingConstants.PAYMENT_TYPE_THIRD_PARTY,
                    getChargeCode(),
                    chargeLineSupport::resolveThirdPartyEntityCode,
                    ChargeCreationConstants.MISSING_THIRD_PARTY_ENTITY_CODE);
        } else {
            paymentGatedEstimateChargeSupport.createEstimateIfPaymentType(
                    context,
                    BatchProcessingConstants.PAYMENT_TYPE_THIRD_PARTY,
                    getChargeCode(),
                    chargeLineSupport::resolveThirdPartyEntityCode,
                    ChargeCreationConstants.MISSING_THIRD_PARTY_ENTITY_CODE);
        }
    }
}
