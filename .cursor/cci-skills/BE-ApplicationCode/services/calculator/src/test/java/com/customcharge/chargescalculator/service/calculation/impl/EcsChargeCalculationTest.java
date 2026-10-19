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
class EcsChargeCalculationTest {

    @Mock
    private PaymentGatedEstimateChargeSupport paymentGatedEstimateChargeSupport;
    @Mock
    private ChargeLineSupport chargeLineSupport;

    @InjectMocks
    private EcsChargeCalculation calculation;

    @Test
    void calculate_shouldDelegateToPaymentGatedSupport() {
        ChargeCalculationContext context = CalculatorTestDataFactory.chargeContext();

        calculation.calculate(context);

        verify(paymentGatedEstimateChargeSupport).createEstimateIfPaymentType(
                eq(context),
                eq(BatchProcessingConstants.PAYMENT_TYPE_PREPAID),
                eq(BatchProcessingConstants.CHARGE_CODE_ECS),
                any(Function.class),
                eq(ChargeCreationConstants.MISSING_ORIGIN_ENTITY_CODE));
    }
}
