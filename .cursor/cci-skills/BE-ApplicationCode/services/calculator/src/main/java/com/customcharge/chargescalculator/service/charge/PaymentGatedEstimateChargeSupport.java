package com.customcharge.chargescalculator.service.charge;

import com.customcharge.chargescalculator.constants.BatchProcessingConstants;
import com.customcharge.chargescalculator.model.ChargeCalculationContext;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.exception.ValidationException;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public class PaymentGatedEstimateChargeSupport {

    private final ChargeLineSupport chargeLineSupport;
    private final EstimateChargeCreationService estimateChargeCreationService;
    private final ActualChargeCreationService actualChargeCreationService;

    public PaymentGatedEstimateChargeSupport(
            ChargeLineSupport chargeLineSupport,
            EstimateChargeCreationService estimateChargeCreationService,
            ActualChargeCreationService actualChargeCreationService) {
        this.chargeLineSupport = chargeLineSupport;
        this.estimateChargeCreationService = estimateChargeCreationService;
        this.actualChargeCreationService = actualChargeCreationService;
    }

    public void createEstimateIfPaymentType(
            ChargeCalculationContext ctx,
            String expectedPaymentType,
            String chargeCode,
            Function<Shipment, String> entityCodeResolver,
            String missingEntityCodeMessage) {
        if (!BatchProcessingConstants.PROCESS_FOR_ESTIMATE.equalsIgnoreCase(ctx.getProcessFor())) {
            return;
        }
        createIfPaymentType(
                ctx,
                expectedPaymentType,
                chargeCode,
                entityCodeResolver,
                missingEntityCodeMessage,
                true);
    }

    public void createActualIfPaymentType(
            ChargeCalculationContext ctx,
            String expectedPaymentType,
            String chargeCode,
            Function<Shipment, String> entityCodeResolver,
            String missingEntityCodeMessage) {
        if (!BatchProcessingConstants.PROCESS_FOR_ACTUAL.equalsIgnoreCase(ctx.getProcessFor())) {
            return;
        }
        createIfPaymentType(
                ctx,
                expectedPaymentType,
                chargeCode,
                entityCodeResolver,
                missingEntityCodeMessage,
                false);
    }

    private void createIfPaymentType(
            ChargeCalculationContext ctx,
            String expectedPaymentType,
            String chargeCode,
            Function<Shipment, String> entityCodeResolver,
            String missingEntityCodeMessage,
            boolean estimate) {
        Shipment shipment = ctx.getShipment();
        if (!chargeLineSupport.matchesPaymentType(shipment, expectedPaymentType)) {
            return;
        }
        String entityCode = entityCodeResolver.apply(shipment);
        if (entityCode == null || entityCode.isBlank()) {
            throw new ValidationException(missingEntityCodeMessage);
        }
        if (estimate) {
            estimateChargeCreationService.createOrUpdateEstimateCharge(ctx, chargeCode, entityCode);
        } else {
            actualChargeCreationService.createOrUpdateActualCharge(ctx, chargeCode, entityCode);
        }
    }
}
