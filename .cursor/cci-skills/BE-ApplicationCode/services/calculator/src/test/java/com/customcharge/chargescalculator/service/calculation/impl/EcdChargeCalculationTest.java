package com.customcharge.chargescalculator.service.calculation.impl;

import com.customcharge.chargescalculator.constants.BatchProcessingConstants;
import com.customcharge.chargescalculator.model.ChargeCalculationContext;
import com.customcharge.chargescalculator.service.charge.ActualChargeCreationService;
import com.customcharge.chargescalculator.service.charge.EstimateChargeCreationService;
import com.customcharge.chargescalculator.support.CalculatorTestDataFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EcdChargeCalculationTest {

    @Mock
    private EstimateChargeCreationService estimateChargeCreationService;
    @Mock
    private ActualChargeCreationService actualChargeCreationService;

    @InjectMocks
    private EcdChargeCalculation calculation;

    @Test
    void getChargeCode_shouldReturnEcd() {
        assertThat(calculation.getChargeCode()).isEqualTo("ECD");
    }

    @Test
    void calculate_shouldDelegateToEstimateService() {
        ChargeCalculationContext context = CalculatorTestDataFactory.chargeContext();

        calculation.calculate(context);

        verify(estimateChargeCreationService).createOrUpdateEstimateCharge(context, "ECD");
        verify(actualChargeCreationService, never()).createOrUpdateActualCharge(context, "ECD");
    }

    @Test
    void calculate_shouldDelegateToActualService_forActualProcess() {
        ChargeCalculationContext context = CalculatorTestDataFactory.chargeContext(
                BatchProcessingConstants.PROCESS_FOR_ACTUAL,
                CalculatorTestDataFactory.actualRecordMap());

        calculation.calculate(context);

        verify(actualChargeCreationService).createOrUpdateActualCharge(context, "ECD");
    }
}
