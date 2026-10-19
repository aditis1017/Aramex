package com.customcharge.chargescalculator.service.batch;

import com.customcharge.chargescalculator.constants.BatchProcessingConstants;
import com.customcharge.chargescalculator.model.ChargeCalculationContext;
import com.customcharge.chargescalculator.service.calculation.ChargeCalculation;
import com.customcharge.chargescalculator.service.calculation.ShipmentTypeChargeCalculatorRouter;
import com.customcharge.chargescalculator.service.charge.ChargeLineSupport;
import com.customcharge.chargescalculator.util.RecordMapSupport;
import com.customcharge.persistencelayer.cache.CategoryCodesGlobalDataCacheService;
import com.customcharge.persistencelayer.constant.DomEntityConstants;
import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.exception.AbstractApiException;
import com.customcharge.persistencelayer.exception.ValidationException;
import com.customcharge.persistencelayer.recordtracking.RecordTrackingStepProbe;
import com.customcharge.persistencelayer.repository.ChargeRepository;
import com.customcharge.persistencelayer.repository.ShipmentRepository;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SingleRecordProcessorService {

    private static final Logger log = LoggerFactory.getLogger(SingleRecordProcessorService.class);

    private static final String AWB_KEY = "awb";
    private static final String ACTUAL_UPLOAD_COUNT_CODE = "ACTUAL_UPLOAD_COUNT";

    private final ShipmentLookupService shipmentLookupService;
    private final ShipmentRepository shipmentRepository;
    private final ChargeRepository chargeRepository;
    private final RecordProcessingLookups recordProcessingLookups;
    private final ShipmentTypeChargeCalculatorRouter shipmentTypeChargeCalculatorRouter;
    private final ObjectProvider<CategoryCodesGlobalDataCacheService> categoryCodesCacheService;
    private final ObjectProvider<RecordTrackingStepProbe> recordTrackingStepProbe;
    private final EntityManager entityManager;
    private final RowChargeWriteBuffer rowChargeWriteBuffer;

    public SingleRecordProcessorService(
            ShipmentLookupService shipmentLookupService,
            ShipmentRepository shipmentRepository,
            ChargeRepository chargeRepository,
            RecordProcessingLookups recordProcessingLookups,
            ShipmentTypeChargeCalculatorRouter shipmentTypeChargeCalculatorRouter,
            ObjectProvider<CategoryCodesGlobalDataCacheService> categoryCodesCacheService,
            ObjectProvider<RecordTrackingStepProbe> recordTrackingStepProbe,
            EntityManager entityManager,
            RowChargeWriteBuffer rowChargeWriteBuffer) {
        this.shipmentLookupService = shipmentLookupService;
        this.shipmentRepository = shipmentRepository;
        this.chargeRepository = chargeRepository;
        this.recordProcessingLookups = recordProcessingLookups;
        this.shipmentTypeChargeCalculatorRouter = shipmentTypeChargeCalculatorRouter;
        this.categoryCodesCacheService = categoryCodesCacheService;
        this.recordTrackingStepProbe = recordTrackingStepProbe;
        this.entityManager = entityManager;
        this.rowChargeWriteBuffer = rowChargeWriteBuffer;
    }

    /**
     * Processes one upload row inside a caller-managed transaction. Business failures must be caught
     * outside the row transaction so the transaction can roll back without {@code UnexpectedRollbackException}.
     */
    public void processRecordOrThrow(
            Map<String, String> recordData,
            String processFor,
            String rawUploadFileId,
            int chunkIndex,
            String entityCode) throws AbstractApiException {
        RecordTrackingStepProbe stepProbe = recordTrackingStepProbe.getIfAvailable();
        RecordTrackingStepProbe.PhaseTimer validationTimer = stepProbe != null ? stepProbe.validationStarted() : null;

        Map<String, String> normalized = RecordMapSupport.lowercaseKeys(recordData);
        String awb = normalized.get(AWB_KEY);
        if (awb == null || awb.isBlank()) {
            throw new ValidationException(BatchProcessingConstants.ERROR_INVALID_RECORD);
        }

        shipmentRepository.findByHawbNumber(awb.strip())
                .ifPresent(existing -> {
                    assertDdoNotConsumed(existing);
                    assertExemptedCategoryNotConsumed(existing);
                });
        Shipment shipment = shipmentLookupService.resolveByHawb(awb.strip());
        try {
            recordProcessingLookups.openForShipment(shipment.getId());
            rowChargeWriteBuffer.open();
            assertDdoNotConsumed(shipment);
            assertExemptedCategoryNotConsumed(shipment);
            assertEntityMatchesDestination(entityCode, shipment);

            if (BatchProcessingConstants.PROCESS_FOR_ESTIMATE.equalsIgnoreCase(processFor)) {
                assertNoActualizedCharges(shipment);
            } else if (BatchProcessingConstants.PROCESS_FOR_ACTUAL.equalsIgnoreCase(processFor)) {
                assertActualUploadAllowed(shipment);
                String paymentReference = normalized.get(BatchProcessingConstants.PAYMENT_REFERENCE);
                if (paymentReference == null || paymentReference.isBlank()) {
                    throw new ValidationException(BatchProcessingConstants.missingPaymentReferenceMessage());
                }
            }

            if (stepProbe != null) {
                stepProbe.validationCompleted(validationTimer);
            }

            RecordTrackingStepProbe.PhaseTimer calculationTimer = stepProbe != null ? stepProbe.calculationStarted() : null;
            List<ChargeCalculation> calculators =
                    shipmentTypeChargeCalculatorRouter.getCalculators(processFor, shipment.getShipmentType());
            ChargeCalculationContext context =
                    new ChargeCalculationContext(shipment, normalized, rawUploadFileId, processFor, chunkIndex);
            calculators.forEach(calculator -> calculator.calculate(context));
            if (stepProbe != null) {
                stepProbe.calculationCompleted(calculationTimer);
            }

            RecordTrackingStepProbe.PhaseTimer dbSaveTimer = stepProbe != null ? stepProbe.dbSaveStarted() : null;
            if (BatchProcessingConstants.PROCESS_FOR_ACTUAL.equalsIgnoreCase(processFor)) {
                rowChargeWriteBuffer.flush();
                if (!ChargeLineSupport.requiresZeroAmounts(shipment)) {
                    flushPendingPersistenceChanges();
                    markAllChargesActualized(shipment.getId());
                }
                incrementActualUploadCount(shipment);
            }
            if (stepProbe != null) {
                stepProbe.dbSaveCompleted(dbSaveTimer);
            }
        } finally {
            rowChargeWriteBuffer.close();
            recordProcessingLookups.closeScope();
        }
    }

    public Map<String, String> toFailedRecord(Map<String, String> recordData, AbstractApiException exception) {
        Map<String, String> copy = new java.util.HashMap<>(recordData);
        String errorMessage = exception.getMessage() != null
                ? exception.getMessage()
                : BatchProcessingConstants.ERROR_PROCESSING_FAILURE;
        copy.put(BatchProcessingConstants.ERROR_KEY, errorMessage);
        copy.put(BatchProcessingConstants.ERROR_CODE_KEY, exception.getErrorCode().getCode());
        return copy;
    }

    public void logRecordFailure(UUID fileId, String awb, int chunkIndex, AbstractApiException ex) {
        String awbKey = awb != null ? awb.strip() : null;
        if (log.isDebugEnabled()) {
            log.debug("Record processing failed awb={} fileId={} chunkIndex={}",
                    awbKey, fileId, chunkIndex, ex);
        } else {
            log.warn("Record processing failed awb={} fileId={} reason={}", awbKey, fileId, ex.getMessage());
        }
    }

    private void assertEntityMatchesDestination(String selectedEntityCode, Shipment shipment) {
        if (selectedEntityCode == null || selectedEntityCode.isBlank()) {
            return;
        }
        String selected = selectedEntityCode.strip();
        if (ChargeLineSupport.isDomesticProductGroup(shipment)) {
            String configuredDomEntity = resolveDomEntityCode(shipment);
            if (!configuredDomEntity.equalsIgnoreCase(selected)) {
                throw new ValidationException(
                        DomEntityConstants.formatDomEntityValidationMessage(configuredDomEntity));
            }
            return;
        }
        String destinationEntity = shipment.getHAWBDestinationEntity();
        String destination = destinationEntity == null ? "" : destinationEntity.strip();
        if (destination.isBlank() || !selected.equalsIgnoreCase(destination)) {
            throw new ValidationException(
                    BatchProcessingConstants.formatEntityDestinationMismatch(selected, destination));
        }
    }

    private String resolveDomEntityCode(Shipment shipment) {
        return DomEntityConstants.buildDomEntityValueCode(shipment.getConsigneeCountryCode())
                .map(valueCode -> {
                    CategoryCodesGlobalDataCacheService cache = categoryCodesCacheService.getIfAvailable();
                    if (cache == null) {
                        return DomEntityConstants.DEFAULT_DOM_ENTITY_CODE;
                    }
                    try {
                        return cache.getCategoryCodeByCode(valueCode).getName().strip();
                    } catch (Exception ex) {
                        return DomEntityConstants.DEFAULT_DOM_ENTITY_CODE;
                    }
                })
                .orElse(DomEntityConstants.DEFAULT_DOM_ENTITY_CODE);
    }

    private void assertNoActualizedCharges(Shipment shipment) {
        if (recordProcessingLookups.hasActualizedCharge(shipment.getId())) {
            throw new ValidationException(
                    BatchProcessingConstants.formatActualRecordExistsForAwb(shipment.getHawbNumber()));
        }
    }

    /**
     * Row processing uses {@code FlushMode.COMMIT}; bulk status update clears the persistence context.
     * Flush first so actual charge lines and header amounts are written before that clear.
     */
    private void flushPendingPersistenceChanges() {
        entityManager.flush();
    }

    private void markAllChargesActualized(UUID shipmentId) {
        chargeRepository.updateStatusByShipmentId(shipmentId, BatchProcessingConstants.STATUS_ACTUALIZED);
    }

    private void assertActualUploadAllowed(Shipment shipment) {
        int maxAllowed = resolveMaxActualUploadCount();
        int current = shipment.getActualUploadCount() == null ? 0 : shipment.getActualUploadCount();
        if (current >= maxAllowed) {
            throw new ValidationException(BatchProcessingConstants.formatActualUploadLimitExceeded(maxAllowed));
        }
    }

    private void assertDdoNotConsumed(Shipment shipment) {
        if (!BatchProcessingConstants.SHIPMENT_TYPE_DDO.equalsIgnoreCase(shipment.getShipmentType())) {
            return;
        }
        boolean hasEcd = recordProcessingLookups.findCharge(shipment.getId(), "ECD")
                .map(Charge::getId)
                .isPresent();
        boolean hasDtc = recordProcessingLookups.findCharge(shipment.getId(), "DTC")
                .map(Charge::getId)
                .isPresent();
        if (hasEcd && hasDtc) {
            throw new ValidationException(BatchProcessingConstants.ERROR_DDO_ALREADY_CONSUMED);
        }
    }

    private void assertExemptedCategoryNotConsumed(Shipment shipment) {
        if (!ChargeLineSupport.isExemptedCategoryType(shipment)) {
            return;
        }
        if (recordProcessingLookups.hasAnyCharges(shipment.getId())) {
            throw new ValidationException(BatchProcessingConstants.ERROR_EXEMPTED_ALREADY_CONSUMED);
        }
    }

    private int resolveMaxActualUploadCount() {
        CategoryCodesGlobalDataCacheService cache = categoryCodesCacheService.getIfAvailable();
        if (cache == null) {
            return BatchProcessingConstants.DEFAULT_MAX_ACTUAL_UPLOAD_COUNT;
        }
        try {
            return Integer.parseInt(cache.getCategoryCodeByCode(ACTUAL_UPLOAD_COUNT_CODE).getName().strip());
        } catch (Exception ex) {
            return BatchProcessingConstants.DEFAULT_MAX_ACTUAL_UPLOAD_COUNT;
        }
    }

    private void incrementActualUploadCount(Shipment shipment) {
        int current = shipment.getActualUploadCount() == null ? 0 : shipment.getActualUploadCount();
        shipment.setActualUploadCount(current + 1);
        shipmentRepository.save(shipment);
        log.debug("Incremented actualUploadCount for shipmentId={} hawb={} newCount={}",
                shipment.getId(), shipment.getHawbNumber(), shipment.getActualUploadCount());
    }
}
