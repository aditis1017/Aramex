package com.customcharge.chargescalculator.service.charge;

import com.customcharge.chargescalculator.constants.BatchProcessingConstants;
import com.customcharge.chargescalculator.model.ChargeCalculationContext;
import com.customcharge.chargescalculator.support.CalculatorTestDataFactory;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentGatedEstimateChargeSupportTest {

    @Mock
    private ChargeLineSupport chargeLineSupport;
    @Mock
    private EstimateChargeCreationService estimateChargeCreationService;
    @Mock
    private ActualChargeCreationService actualChargeCreationService;

    @InjectMocks
    private PaymentGatedEstimateChargeSupport support;

    @Test
    void createEstimateIfPaymentType_shouldNoOp_whenProcessForIsActual() {
        ChargeCalculationContext context = CalculatorTestDataFactory.chargeContext(
                BatchProcessingConstants.PROCESS_FOR_ACTUAL,
                CalculatorTestDataFactory.actualRecordMap());

        support.createEstimateIfPaymentType(
                context,
                BatchProcessingConstants.PAYMENT_TYPE_PREPAID,
                BatchProcessingConstants.CHARGE_CODE_ECS,
                Shipment::getOriginEntity,
                "missing");

        verify(estimateChargeCreationService, never()).createOrUpdateEstimateCharge(
                context,
                BatchProcessingConstants.CHARGE_CODE_ECS,
                "DXB");
    }

    @Test
    void createEstimateIfPaymentType_shouldCreateCharge_whenPaymentMatches() {
        ChargeCalculationContext context = CalculatorTestDataFactory.chargeContext();
        when(chargeLineSupport.matchesPaymentType(any(Shipment.class), eq(BatchProcessingConstants.PAYMENT_TYPE_PREPAID)))
                .thenReturn(true);
        when(chargeLineSupport.resolveOriginEntityCode(any(Shipment.class))).thenReturn("RUH");

        support.createEstimateIfPaymentType(
                context,
                BatchProcessingConstants.PAYMENT_TYPE_PREPAID,
                BatchProcessingConstants.CHARGE_CODE_ECS,
                chargeLineSupport::resolveOriginEntityCode,
                "missing origin");

        verify(estimateChargeCreationService).createOrUpdateEstimateCharge(
                context,
                BatchProcessingConstants.CHARGE_CODE_ECS,
                "RUH");
    }

    @Test
    void createEstimateIfPaymentType_shouldSkip_whenPaymentDoesNotMatch() {
        ChargeCalculationContext context = CalculatorTestDataFactory.chargeContext();
        when(chargeLineSupport.matchesPaymentType(any(Shipment.class), eq(BatchProcessingConstants.PAYMENT_TYPE_PREPAID)))
                .thenReturn(false);

        support.createEstimateIfPaymentType(
                context,
                BatchProcessingConstants.PAYMENT_TYPE_PREPAID,
                BatchProcessingConstants.CHARGE_CODE_ECS,
                chargeLineSupport::resolveOriginEntityCode,
                "missing origin");

        verify(estimateChargeCreationService, never()).createOrUpdateEstimateCharge(
                context,
                BatchProcessingConstants.CHARGE_CODE_ECS,
                "RUH");
    }

    @Test
    void createEstimateIfPaymentType_shouldThrow_whenEntityCodeMissing() {
        ChargeCalculationContext context = CalculatorTestDataFactory.chargeContext();
        when(chargeLineSupport.matchesPaymentType(any(Shipment.class), eq(BatchProcessingConstants.PAYMENT_TYPE_PREPAID)))
                .thenReturn(true);
        when(chargeLineSupport.resolveOriginEntityCode(any(Shipment.class))).thenReturn(" ");

        assertThatThrownBy(() -> support.createEstimateIfPaymentType(
                context,
                BatchProcessingConstants.PAYMENT_TYPE_PREPAID,
                BatchProcessingConstants.CHARGE_CODE_ECS,
                chargeLineSupport::resolveOriginEntityCode,
                "missing origin"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("missing origin");
    }

    @Test
    void createActualIfPaymentType_shouldCreateActualCharge_whenProcessForIsActual() {
        ChargeCalculationContext context = CalculatorTestDataFactory.chargeContext(
                BatchProcessingConstants.PROCESS_FOR_ACTUAL,
                CalculatorTestDataFactory.actualRecordMap());
        when(chargeLineSupport.matchesPaymentType(any(Shipment.class), eq(BatchProcessingConstants.PAYMENT_TYPE_PREPAID)))
                .thenReturn(true);
        when(chargeLineSupport.resolveOriginEntityCode(any(Shipment.class))).thenReturn("RUH");

        support.createActualIfPaymentType(
                context,
                BatchProcessingConstants.PAYMENT_TYPE_PREPAID,
                BatchProcessingConstants.CHARGE_CODE_DTS,
                chargeLineSupport::resolveOriginEntityCode,
                "missing origin");

        verify(actualChargeCreationService).createOrUpdateActualCharge(
                context,
                BatchProcessingConstants.CHARGE_CODE_DTS,
                "RUH");
    }
}
