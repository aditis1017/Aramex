package com.customcharge.chargescalculator.service.calculation.impl;

import com.customcharge.chargescalculator.constants.BatchProcessingConstants;
import com.customcharge.chargescalculator.constants.ChargeCreationConstants;
import com.customcharge.chargescalculator.model.ChargeCalculationContext;
import com.customcharge.chargescalculator.service.charge.ChargeLineSupport;
import com.customcharge.chargescalculator.service.charge.PaymentGatedEstimateChargeSupport;
import com.customcharge.chargescalculator.support.CalculatorTestDataFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Function;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DtsChargeCalculationTest {

    @Mock
    private PaymentGatedEstimateChargeSupport paymentGatedEstimateChargeSupport;
    @Mock
    private ChargeLineSupport chargeLineSupport;

    @InjectMocks
    private DtsChargeCalculation calculation;

    @Test
    void calculate_shouldUseEstimatePath() {
        ChargeCalculationContext context = CalculatorTestDataFactory.chargeContext();

        calculation.calculate(context);

        verify(paymentGatedEstimateChargeSupport).createEstimateIfPaymentType(
                eq(context),
                eq(BatchProcessingConstants.PAYMENT_TYPE_PREPAID),
                eq(BatchProcessingConstants.CHARGE_CODE_DTS),
                any(Function.class),
                eq(ChargeCreationConstants.MISSING_ORIGIN_ENTITY_CODE));
    }

    @Test
    void calculate_shouldUseActualPath_forActualProcess() {
        ChargeCalculationContext context = CalculatorTestDataFactory.chargeContext(
                BatchProcessingConstants.PROCESS_FOR_ACTUAL,
                CalculatorTestDataFactory.actualRecordMap());

        calculation.calculate(context);

        verify(paymentGatedEstimateChargeSupport).createActualIfPaymentType(
                eq(context),
                eq(BatchProcessingConstants.PAYMENT_TYPE_PREPAID),
                eq(BatchProcessingConstants.CHARGE_CODE_DTS),
                any(Function.class),
                eq(ChargeCreationConstants.MISSING_ORIGIN_ENTITY_CODE));
    }
}
