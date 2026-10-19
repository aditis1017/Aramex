package com.customcharge.chargescalculator.service.charge;

import com.customcharge.chargescalculator.constants.BatchProcessingConstants;
import com.customcharge.chargescalculator.constants.ChargeCreationConstants;
import com.customcharge.chargescalculator.model.ChargeCalculationContext;
import com.customcharge.chargescalculator.service.batch.RecordProcessingLookups;
import com.customcharge.chargescalculator.service.batch.RowChargeWriteBuffer;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.ChargeLineItem;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.exception.ValidationException;
import com.customcharge.persistencelayer.repository.ChargeLineItemRepository;
import com.customcharge.persistencelayer.repository.ChargeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ActualChargeCreationService {

    private static final Map<String, String> ACTUAL_MIRROR_TO_COST_CHARGE = Map.of(
            BatchProcessingConstants.CHARGE_CODE_DTC, BatchProcessingConstants.CHARGE_CODE_ECD,
            BatchProcessingConstants.CHARGE_CODE_DTA, BatchProcessingConstants.CHARGE_CODE_ECD,
            BatchProcessingConstants.CHARGE_CODE_DTS, BatchProcessingConstants.CHARGE_CODE_ECS,
            BatchProcessingConstants.CHARGE_CODE_DT3, BatchProcessingConstants.CHARGE_CODE_EC3);

    private final ChargeLineSupport chargeLineSupport;
    private final ChargeRepository chargeRepository;
    private final ChargeLineItemRepository chargeLineItemRepository;
    private final RecordProcessingLookups recordProcessingLookups;
    private final DtaContextSupport dtaContextSupport;
    private final ActualChargeAmountPropagationService actualChargeAmountPropagationService;
    private final RowChargeWriteBuffer rowChargeWriteBuffer;

    public ActualChargeCreationService(
            ChargeLineSupport chargeLineSupport,
            ChargeRepository chargeRepository,
            ChargeLineItemRepository chargeLineItemRepository,
            RecordProcessingLookups recordProcessingLookups,
            DtaContextSupport dtaContextSupport,
            ActualChargeAmountPropagationService actualChargeAmountPropagationService,
            RowChargeWriteBuffer rowChargeWriteBuffer) {
        this.chargeLineSupport = chargeLineSupport;
        this.chargeRepository = chargeRepository;
        this.chargeLineItemRepository = chargeLineItemRepository;
        this.recordProcessingLookups = recordProcessingLookups;
        this.dtaContextSupport = dtaContextSupport;
        this.actualChargeAmountPropagationService = actualChargeAmountPropagationService;
        this.rowChargeWriteBuffer = rowChargeWriteBuffer;
    }

    @Transactional
    public void createOrUpdateActualCharge(ChargeCalculationContext ctx, String chargeCode) {
        createOrUpdateActualCharge(
                ctx,
                chargeCode,
                chargeLineSupport.resolveDomDestinationChargeEntityCode(ctx.getShipment(), chargeCode));
    }

    @Transactional
    public void createOrUpdateActualCharge(
            ChargeCalculationContext ctx,
            String chargeCode,
            String billingEntityCode) {
        createOrUpdateActualChargeFromFile(ctx, chargeCode, billingEntityCode);
    }

    private void createOrUpdateActualChargeFromFile(
            ChargeCalculationContext ctx,
            String chargeCode,
            String billingEntityCode) {
        Map<String, String> source = ctx.getSourceRecord();
        String paymentReference = ChargeLineSupport.sourceValue(source, BatchProcessingConstants.PAYMENT_REFERENCE);
        Shipment shipment = ctx.getShipment();
        boolean zeroAmounts = ChargeLineSupport.requiresZeroAmounts(shipment);
        if (!zeroAmounts && paymentReference.isBlank()) {
            throw new ValidationException(BatchProcessingConstants.missingPaymentReferenceMessage());
        }
        String entityCode = billingEntityCode == null ? "" : billingEntityCode.strip();
        if (entityCode.isBlank()) {
            throw new ValidationException(ChargeCreationConstants.MISSING_ENTITY_CODE);
        }

        List<Account> accounts = BatchProcessingConstants.CHARGE_CODE_DTC.equalsIgnoreCase(chargeCode)
                ? chargeLineSupport.resolveAccountsForNewDtcLine(shipment, entityCode)
                : chargeLineSupport.resolveAccounts(shipment, entityCode, chargeCode);
        if (accounts.isEmpty()) {
            throw new ValidationException(ChargeCreationConstants.NO_ACCOUNTS_FOR_CHARGE + ": " + chargeCode);
        }

        com.customcharge.persistencelayer.entities.Entity billingEntity = recordProcessingLookups.findEntity(entityCode)
                .orElseThrow(() -> new ValidationException(
                        ChargeCreationConstants.ENTITY_MASTER_NOT_FOUND + ": " + entityCode));

        UUID fileReferenceId = ChargeLineSupport.parseOptionalFileReference(ctx.getRawUploadFileId());

        Charge charge = prepareCharge(shipment, chargeCode, billingEntity, entityCode);
        assertPaymentReferenceNotDuplicate(charge, paymentReference, chargeCode);
        assertDeclarationNumberNotDuplicate(source);

        DtaContextSupport.DtaLineContext dta =
                dtaContextSupport.forCharge(ctx, shipment, chargeCode, entityCode, chargeLineSupport);
        List<ChargeLineItem> newLines = chargeLineSupport.buildLineItemsForCharge(
                charge,
                accounts,
                source,
                chargeCode,
                entityCode,
                fileReferenceId,
                paymentReference,
                dta,
                true,
                ChargeLineSupport.exchangeRateKeyPrefix(ctx));
        rowChargeWriteBuffer.enqueueLineSaves(newLines);
        dta.persist(ctx);

        finalizeActualCharge(charge, zeroAmounts, newLines);

        if (BatchProcessingConstants.CHARGE_CODE_ECD.equalsIgnoreCase(chargeCode)) {
            actualChargeAmountPropagationService.propagateAfterEcdActualUpload(ctx);
        }
    }

    private Charge prepareCharge(
            Shipment shipment,
            String chargeCode,
            com.customcharge.persistencelayer.entities.Entity billingEntity,
            String entityCode) {
        Charge charge = recordProcessingLookups.findCharge(shipment.getId(), chargeCode)
                .orElse(null);
        if (charge == null) {
            charge = new Charge();
            charge.setShipment(shipment);
            charge.setChargeCode(chargeCode);
            charge.setEntity(billingEntity);
            charge = rowChargeWriteBuffer.persistNewCharge(charge);
            recordProcessingLookups.registerCharge(charge);
        } else {
            charge = chargeRepository.findByIdForUpdate(charge.getId())
                    .orElseThrow(() -> new ValidationException("Charge not found after save"));
            charge.setEntity(billingEntity);
        }
        chargeLineSupport.applyChargeCurrency(charge, chargeCode, entityCode, shipment, true);
        return charge;
    }

    private void assertPaymentReferenceNotDuplicate(Charge charge, String paymentReference, String chargeCode) {
        String normalizedPaymentReference = paymentReference == null ? "" : paymentReference.strip();
        if (normalizedPaymentReference.isEmpty()) {
            return;
        }
        boolean duplicate = recordProcessingLookups.findLineItems(charge.getId()).stream()
                .anyMatch(line -> line != null
                        && line.getPaymentReference() != null
                        && line.getPaymentReference().strip().equalsIgnoreCase(normalizedPaymentReference));
        if (duplicate) {
            throw new ValidationException(
                    BatchProcessingConstants.ERROR_ACTUAL_PAYMENT_REFERENCE_ALREADY_EXISTS
                            + ": " + chargeCode
                            + ", " + BatchProcessingConstants.paymentReferenceLabel() + ": " + paymentReference);
        }
    }

    private void assertDeclarationNumberNotDuplicate(Map<String, String> source) {
        String declarationNumber = ChargeLineSupport.sourceValue(
                source, BatchProcessingConstants.SOURCE_KEY_DECLARATION_NUMBER);
        if (declarationNumber == null || declarationNumber.isBlank()) {
            return;
        }
        if (chargeLineItemRepository.existsByDeclarationNumberNormalized(declarationNumber)) {
            throw new ValidationException(BatchProcessingConstants.ERROR_ACTUAL_DECLARATION_NUMBER_ALREADY_EXISTS);
        }
    }

    private void finalizeActualCharge(Charge charge, boolean zeroAmounts, List<ChargeLineItem> newLines) {
        List<ChargeLineItem> allLines = new ArrayList<>(recordProcessingLookups.findLineItems(charge.getId()));
        allLines.addAll(newLines);
        recordProcessingLookups.cacheLineItems(charge.getId(), allLines);
        if (zeroAmounts) {
            charge.setEstimateAmount(BigDecimal.ZERO);
            charge.setActualAmount(BigDecimal.ZERO);
            charge.setOutstandingAmount(BigDecimal.ZERO);
        } else {
            chargeLineSupport.applyChargeHeaderAmounts(charge, allLines);
        }
        rowChargeWriteBuffer.enqueueChargeUpdate(charge);
    }
}
