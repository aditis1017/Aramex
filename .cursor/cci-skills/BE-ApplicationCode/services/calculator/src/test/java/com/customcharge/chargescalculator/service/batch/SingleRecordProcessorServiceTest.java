package com.customcharge.chargescalculator.service.batch;

import com.customcharge.chargescalculator.constants.BatchProcessingConstants;
import com.customcharge.chargescalculator.model.ChargeCalculationContext;
import com.customcharge.chargescalculator.service.calculation.ChargeCalculation;
import com.customcharge.chargescalculator.service.calculation.ShipmentTypeChargeCalculatorRouter;
import com.customcharge.chargescalculator.support.CalculatorTestDataFactory;
import com.customcharge.persistencelayer.cache.CategoryCodesGlobalDataCacheService;
import com.customcharge.persistencelayer.constant.DomEntityConstants;
import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.entities.CategoryValue;
import com.customcharge.persistencelayer.exception.AbstractApiException;
import com.customcharge.persistencelayer.exception.ErrorCode;
import com.customcharge.persistencelayer.exception.ValidationException;
import com.customcharge.persistencelayer.recordtracking.RecordTrackingStepProbe;
import com.customcharge.persistencelayer.repository.ChargeRepository;
import com.customcharge.persistencelayer.repository.ShipmentRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@ExtendWith(MockitoExtension.class)
class SingleRecordProcessorServiceTest {

    @Mock
    private ShipmentLookupService shipmentLookupService;
    @Mock
    private ShipmentRepository shipmentRepository;
    @Mock
    private ChargeRepository chargeRepository;
    @Mock
    private RecordProcessingLookups recordProcessingLookups;
    @Mock
    private ShipmentTypeChargeCalculatorRouter shipmentTypeChargeCalculatorRouter;
    @Mock
    private ObjectProvider<CategoryCodesGlobalDataCacheService> categoryCodesCacheService;
    @Mock
    private ObjectProvider<RecordTrackingStepProbe> recordTrackingStepProbe;
    @Mock
    private EntityManager entityManager;
    @Mock
    private RowChargeWriteBuffer rowChargeWriteBuffer;

    private SingleRecordProcessorService service;

    @BeforeEach
    void setUpMocks() {
        service = new SingleRecordProcessorService(
                shipmentLookupService,
                shipmentRepository,
                chargeRepository,
                recordProcessingLookups,
                shipmentTypeChargeCalculatorRouter,
                categoryCodesCacheService,
                recordTrackingStepProbe,
                entityManager,
                rowChargeWriteBuffer);
        lenient().when(recordProcessingLookups.openForShipment(any())).thenReturn(mock(RecordProcessingLookupCache.class));
        lenient().doNothing().when(recordProcessingLookups).closeScope();
        lenient().doNothing().when(rowChargeWriteBuffer).open();
        lenient().doNothing().when(rowChargeWriteBuffer).close();
        lenient().doNothing().when(rowChargeWriteBuffer).flush();
        lenient().when(recordProcessingLookups.hasActualizedCharge(any())).thenAnswer(invocation ->
                chargeRepository.existsByShipment_IdAndStatus(
                        invocation.getArgument(0), BatchProcessingConstants.STATUS_ACTUALIZED));
        lenient().when(recordProcessingLookups.findCharge(any(), any())).thenAnswer(invocation ->
                chargeRepository.findByShipment_IdAndChargeCode(invocation.getArgument(0), invocation.getArgument(1)));
        lenient().when(recordProcessingLookups.hasAnyCharges(any())).thenAnswer(invocation ->
                !chargeRepository.findByShipment_IdWithEntity(invocation.getArgument(0)).isEmpty());
        lenient().when(recordTrackingStepProbe.getIfAvailable()).thenReturn(null);
    }

    private void stubDomEntityFromCategoryCache(String consigneeCountryCode, String domEntityCode) {
        CategoryCodesGlobalDataCacheService cache = mock(CategoryCodesGlobalDataCacheService.class);
        CategoryValue domEntityValue = new CategoryValue();
        domEntityValue.setName(domEntityCode);
        String domEntityValueCode = DomEntityConstants.buildDomEntityValueCode(consigneeCountryCode).orElseThrow();
        lenient().when(categoryCodesCacheService.getIfAvailable()).thenReturn(cache);
        lenient().when(cache.getCategoryCodeByCode(domEntityValueCode))
                .thenReturn(domEntityValue);
    }

    @Test
    void processRecordOrThrow_shouldThrow_whenAwbMissing() {
        Map<String, String> record = Map.of("foo", "bar");
        String fileId = fileIdString();
        assertThatThrownBy(() -> service.processRecordOrThrow(
                record, BatchProcessingConstants.PROCESS_FOR_ESTIMATE, fileId, 0, null))
                .isInstanceOf(ValidationException.class);

        Map<String, String> failed = service.toFailedRecord(
                Map.of("foo", "bar"),
                new ValidationException(BatchProcessingConstants.ERROR_INVALID_RECORD));
        assertThat(failed)
                .containsEntry(BatchProcessingConstants.ERROR_CODE_KEY, ErrorCode.VALIDATION_FAILED.getCode())
                .containsKey(BatchProcessingConstants.ERROR_KEY);
    }

    @Test
    void processRecordOrThrow_shouldRunCalculators_forEstimate() {
        Shipment shipment = CalculatorTestDataFactory.shipment();
        ChargeCalculation calculator = mock(ChargeCalculation.class);
        when(shipmentLookupService.resolveByHawb("AWB-1001")).thenReturn(shipment);
        when(recordProcessingLookups.hasActualizedCharge(shipment.getId())).thenReturn(false);
        when(shipmentTypeChargeCalculatorRouter.getCalculators(
                BatchProcessingConstants.PROCESS_FOR_ESTIMATE, shipment.getShipmentType()))
                .thenReturn(List.of(calculator));

        service.processRecordOrThrow(
                CalculatorTestDataFactory.recordMap(),
                BatchProcessingConstants.PROCESS_FOR_ESTIMATE,
                        CalculatorTestDataFactory.fileId().toString(),
                        0,
                        null);

        ArgumentCaptor<ChargeCalculationContext> captor = ArgumentCaptor.forClass(ChargeCalculationContext.class);
        verify(calculator).calculate(captor.capture());
        assertThat(captor.getValue().getProcessFor()).isEqualTo(BatchProcessingConstants.PROCESS_FOR_ESTIMATE);
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void processRecordOrThrow_shouldRequireSaddad_forActual() {
        Shipment shipment = CalculatorTestDataFactory.shipment();
        when(shipmentLookupService.resolveByHawb("AWB-1001")).thenReturn(shipment);

        ValidationException ex = org.assertj.core.api.Assertions.catchThrowableOfType(
                ValidationException.class,
                () -> service.processRecordOrThrow(
                        CalculatorTestDataFactory.recordMap(),
                        BatchProcessingConstants.PROCESS_FOR_ACTUAL,
                        CalculatorTestDataFactory.fileId().toString(),
                        0,
                        null));

        assertThat(ex.getMessage()).contains(BatchProcessingConstants.paymentReferenceLabel());
    }

    @Test
    void processRecordOrThrow_shouldThrow_whenSecondCalculatorFails() {
        Shipment shipment = CalculatorTestDataFactory.shipment();
        ChargeCalculation first = mock(ChargeCalculation.class);
        ChargeCalculation second = mock(ChargeCalculation.class);
        when(shipmentLookupService.resolveByHawb("AWB-1001")).thenReturn(shipment);
        when(recordProcessingLookups.hasActualizedCharge(shipment.getId())).thenReturn(false);
        when(shipmentTypeChargeCalculatorRouter.getCalculators(
                BatchProcessingConstants.PROCESS_FOR_ESTIMATE, shipment.getShipmentType()))
                .thenReturn(List.of(first, second));
        doThrow(new ValidationException("ECS account missing"))
                .when(second)
                .calculate(any(ChargeCalculationContext.class));

        Map<String, String> record = CalculatorTestDataFactory.recordMap();
        String fileId = fileIdString();
        assertThatThrownBy(() -> service.processRecordOrThrow(
                record, BatchProcessingConstants.PROCESS_FOR_ESTIMATE, fileId, 0, null))
                .hasMessageContaining("ECS account missing");

        verify(first).calculate(any(ChargeCalculationContext.class));
        verify(second).calculate(any(ChargeCalculationContext.class));
    }

    @Test
    void processRecordOrThrow_shouldRejectEstimate_whenChargesActualized() {
        Shipment shipment = CalculatorTestDataFactory.shipment();
        when(shipmentLookupService.resolveByHawb("AWB-1001")).thenReturn(shipment);
        when(recordProcessingLookups.hasActualizedCharge(shipment.getId())).thenReturn(true);

        Map<String, String> record = CalculatorTestDataFactory.recordMap();
        String fileId = fileIdString();
        assertThatThrownBy(() -> service.processRecordOrThrow(
                record, BatchProcessingConstants.PROCESS_FOR_ESTIMATE, fileId, 0, null))
                .isInstanceOf(ValidationException.class)
                .hasMessage(BatchProcessingConstants.formatActualRecordExistsForAwb("AWB-1001"));

        verify(shipmentTypeChargeCalculatorRouter, never()).getCalculators(any(), any());
    }

    @Test
    void processRecordOrThrow_shouldIncrementActualUploadCount_whenActualSucceeds() {
        Shipment shipment = CalculatorTestDataFactory.shipment();
        ChargeCalculation calculator = mock(ChargeCalculation.class);
        when(shipmentLookupService.resolveByHawb("AWB-1001")).thenReturn(shipment);
        when(shipmentTypeChargeCalculatorRouter.getCalculators(
                BatchProcessingConstants.PROCESS_FOR_ACTUAL, shipment.getShipmentType()))
                .thenReturn(List.of(calculator));

        service.processRecordOrThrow(
                CalculatorTestDataFactory.actualRecordMap(),
                BatchProcessingConstants.PROCESS_FOR_ACTUAL,
                        CalculatorTestDataFactory.fileId().toString(),
                        0,
                        null);

        assertThat(shipment.getActualUploadCount()).isEqualTo(1);
        var order = inOrder(entityManager, chargeRepository);
        order.verify(entityManager).flush();
        order.verify(chargeRepository).updateStatusByShipmentId(
                shipment.getId(), BatchProcessingConstants.STATUS_ACTUALIZED);
        verify(shipmentRepository).save(shipment);
    }

    @Test
    void processRecordOrThrow_shouldNotActualizeCharges_whenDdoShipmentType() {
        Shipment shipment = CalculatorTestDataFactory.shipment();
        shipment.setShipmentType(BatchProcessingConstants.SHIPMENT_TYPE_DDO);
        ChargeCalculation calculator = mock(ChargeCalculation.class);
        when(shipmentLookupService.resolveByHawb("AWB-1001")).thenReturn(shipment);
        when(shipmentTypeChargeCalculatorRouter.getCalculators(
                BatchProcessingConstants.PROCESS_FOR_ACTUAL, shipment.getShipmentType()))
                .thenReturn(List.of(calculator));

        service.processRecordOrThrow(
                CalculatorTestDataFactory.actualRecordMap(),
                BatchProcessingConstants.PROCESS_FOR_ACTUAL,
                        CalculatorTestDataFactory.fileId().toString(),
                        0,
                        null);

        assertThat(shipment.getActualUploadCount()).isEqualTo(1);
        verify(chargeRepository, never()).updateStatusByShipmentId(any(), any());
        verify(shipmentRepository).save(shipment);
    }

    @Test
    void processRecordOrThrow_shouldRejectEstimate_whenExemptedCategoryAlreadyConsumed() {
        Shipment shipment = CalculatorTestDataFactory.shipment();
        shipment.setCategoryType(BatchProcessingConstants.CATEGORY_TYPE_EXEMPTED);
        shipment.setShipmentType(BatchProcessingConstants.SHIPMENT_TYPE_DDP);
        when(shipmentLookupService.resolveByHawb("AWB-1001")).thenReturn(shipment);
        when(recordProcessingLookups.hasAnyCharges(shipment.getId())).thenReturn(true);

        Map<String, String> record = CalculatorTestDataFactory.recordMap();
        String fileId = fileIdString();
        assertThatThrownBy(() -> service.processRecordOrThrow(
                record, BatchProcessingConstants.PROCESS_FOR_ESTIMATE, fileId, 0, null))
                .isInstanceOf(ValidationException.class)
                .hasMessage(BatchProcessingConstants.ERROR_EXEMPTED_ALREADY_CONSUMED);

        verify(shipmentTypeChargeCalculatorRouter, never()).getCalculators(any(), any());
    }

    @Test
    void processRecordOrThrow_shouldNotActualizeCharges_whenCategoryExempted() {
        Shipment shipment = CalculatorTestDataFactory.shipment();
        shipment.setCategoryType(BatchProcessingConstants.CATEGORY_TYPE_EXEMPTED);
        ChargeCalculation calculator = mock(ChargeCalculation.class);
        when(shipmentLookupService.resolveByHawb("AWB-1001")).thenReturn(shipment);
        when(recordProcessingLookups.hasAnyCharges(shipment.getId())).thenReturn(false);
        when(shipmentTypeChargeCalculatorRouter.getCalculators(
                BatchProcessingConstants.PROCESS_FOR_ACTUAL, shipment.getShipmentType()))
                .thenReturn(List.of(calculator));

        service.processRecordOrThrow(
                CalculatorTestDataFactory.actualRecordMap(),
                BatchProcessingConstants.PROCESS_FOR_ACTUAL,
                        CalculatorTestDataFactory.fileId().toString(),
                        0,
                        null);

        assertThat(shipment.getActualUploadCount()).isEqualTo(1);
        verify(chargeRepository, never()).updateStatusByShipmentId(any(), any());
        verify(shipmentRepository).save(shipment);
    }

    @Test
    void processRecordOrThrow_shouldRejectActual_whenUploadLimitExceeded() {
        Shipment shipment = CalculatorTestDataFactory.shipment();
        shipment.setActualUploadCount(BatchProcessingConstants.DEFAULT_MAX_ACTUAL_UPLOAD_COUNT);
        when(shipmentLookupService.resolveByHawb("AWB-1001")).thenReturn(shipment);

        Map<String, String> record = CalculatorTestDataFactory.actualRecordMap();
        String fileId = fileIdString();
        assertThatThrownBy(() -> service.processRecordOrThrow(
                record, BatchProcessingConstants.PROCESS_FOR_ACTUAL, fileId, 0, null))
                .hasMessageContaining(BatchProcessingConstants.formatActualUploadLimitExceeded(
                        BatchProcessingConstants.DEFAULT_MAX_ACTUAL_UPLOAD_COUNT));
    }

    @Test
    void processRecordOrThrow_shouldThrow_whenAwbBlank() {
        Map<String, String> record = Map.of("awb", "   ");
        String fileId = fileIdString();
        assertThatThrownBy(() -> service.processRecordOrThrow(
                record, BatchProcessingConstants.PROCESS_FOR_ESTIMATE, fileId, 0, null))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void toFailedRecord_shouldUseDefaultMessage_whenExceptionMessageNull() {
        AbstractApiException exception = mock(AbstractApiException.class, withSettings().lenient());
        when(exception.getMessage()).thenReturn(null);
        when(exception.getErrorCode()).thenReturn(ErrorCode.VALIDATION_FAILED);

        Map<String, String> failed = service.toFailedRecord(Map.of("awb", "AWB-1"), exception);

        assertThat(failed).containsEntry(
                BatchProcessingConstants.ERROR_KEY, BatchProcessingConstants.ERROR_PROCESSING_FAILURE);
    }

    @Test
    void logRecordFailure_shouldNotThrow() {
        assertThatCode(() -> service.logRecordFailure(
                CalculatorTestDataFactory.fileId(), " AWB-1 ", 2, new ValidationException("failed")))
                .doesNotThrowAnyException();
    }

    @Test
    void processRecordOrThrow_shouldReject_whenDdoAlreadyConsumed() {
        Shipment shipment = CalculatorTestDataFactory.shipment();
        shipment.setShipmentType(BatchProcessingConstants.SHIPMENT_TYPE_DDO);
        Charge ecd = CalculatorTestDataFactory.charge(UUID.randomUUID(), shipment, "ECD");
        Charge dtc = CalculatorTestDataFactory.charge(UUID.randomUUID(), shipment, "DTC");
        when(shipmentLookupService.resolveByHawb("AWB-1001")).thenReturn(shipment);
        when(recordProcessingLookups.findCharge(shipment.getId(), "ECD"))
                .thenReturn(Optional.of(ecd));
        when(recordProcessingLookups.findCharge(shipment.getId(), "DTC"))
                .thenReturn(Optional.of(dtc));

        Map<String, String> record = CalculatorTestDataFactory.recordMap();
        String fileId = fileIdString();
        assertThatThrownBy(() -> service.processRecordOrThrow(
                record, BatchProcessingConstants.PROCESS_FOR_ESTIMATE, fileId, 0, null))
                .hasMessage(BatchProcessingConstants.ERROR_DDO_ALREADY_CONSUMED);

        verify(shipmentTypeChargeCalculatorRouter, never()).getCalculators(any(), any());
    }

    @Test
    void processRecordOrThrow_shouldSkipExemptedCheck_whenCategoryNotExempted() {
        Shipment shipment = CalculatorTestDataFactory.shipment();
        ChargeCalculation calculator = mock(ChargeCalculation.class);
        when(shipmentLookupService.resolveByHawb("AWB-1001")).thenReturn(shipment);
        when(recordProcessingLookups.hasActualizedCharge(shipment.getId())).thenReturn(false);
        when(shipmentTypeChargeCalculatorRouter.getCalculators(
                BatchProcessingConstants.PROCESS_FOR_ESTIMATE, shipment.getShipmentType()))
                .thenReturn(List.of(calculator));

        service.processRecordOrThrow(
                CalculatorTestDataFactory.recordMap(),
                BatchProcessingConstants.PROCESS_FOR_ESTIMATE,
                        CalculatorTestDataFactory.fileId().toString(),
                        0,
                        null);

        verify(recordProcessingLookups, never()).hasAnyCharges(any());
        verify(calculator).calculate(any(ChargeCalculationContext.class));
    }

    @Test
    void processRecordOrThrow_shouldUseConfiguredMaxUploadCount_fromCache() {
        Shipment shipment = CalculatorTestDataFactory.shipment();
        shipment.setActualUploadCount(2);
        CategoryCodesGlobalDataCacheService cache = mock(CategoryCodesGlobalDataCacheService.class);
        CategoryValue uploadLimit = new CategoryValue();
        uploadLimit.setName("2");
        when(categoryCodesCacheService.getIfAvailable()).thenReturn(cache);
        when(cache.getCategoryCodeByCode("ACTUAL_UPLOAD_COUNT")).thenReturn(uploadLimit);
        when(shipmentLookupService.resolveByHawb("AWB-1001")).thenReturn(shipment);

        Map<String, String> record = CalculatorTestDataFactory.actualRecordMap();
        String fileId = fileIdString();
        assertThatThrownBy(() -> service.processRecordOrThrow(
                record, BatchProcessingConstants.PROCESS_FOR_ACTUAL, fileId, 0, null))
                .hasMessageContaining(BatchProcessingConstants.formatActualUploadLimitExceeded(2));
    }

    @Test
    void processRecordOrThrow_shouldFallbackToDefaultMaxUploadCount_whenCacheValueInvalid() {
        Shipment shipment = CalculatorTestDataFactory.shipment();
        shipment.setActualUploadCount(BatchProcessingConstants.DEFAULT_MAX_ACTUAL_UPLOAD_COUNT);
        CategoryCodesGlobalDataCacheService cache = mock(CategoryCodesGlobalDataCacheService.class);
        when(categoryCodesCacheService.getIfAvailable()).thenReturn(cache);
        when(cache.getCategoryCodeByCode("ACTUAL_UPLOAD_COUNT")).thenThrow(new RuntimeException("cache error"));
        when(shipmentLookupService.resolveByHawb("AWB-1001")).thenReturn(shipment);

        Map<String, String> record = CalculatorTestDataFactory.actualRecordMap();
        String fileId = fileIdString();
        assertThatThrownBy(() -> service.processRecordOrThrow(
                record, BatchProcessingConstants.PROCESS_FOR_ACTUAL, fileId, 0, null))
                .hasMessageContaining(BatchProcessingConstants.formatActualUploadLimitExceeded(
                        BatchProcessingConstants.DEFAULT_MAX_ACTUAL_UPLOAD_COUNT));
    }

    @Test
    void processRecordOrThrow_shouldIncrementFromNullActualUploadCount() {
        Shipment shipment = CalculatorTestDataFactory.shipment();
        shipment.setActualUploadCount(null);
        ChargeCalculation calculator = mock(ChargeCalculation.class);
        when(shipmentLookupService.resolveByHawb("AWB-1001")).thenReturn(shipment);
        when(shipmentTypeChargeCalculatorRouter.getCalculators(
                BatchProcessingConstants.PROCESS_FOR_ACTUAL, shipment.getShipmentType()))
                .thenReturn(List.of(calculator));

        service.processRecordOrThrow(
                CalculatorTestDataFactory.actualRecordMap(),
                BatchProcessingConstants.PROCESS_FOR_ACTUAL,
                        CalculatorTestDataFactory.fileId().toString(),
                        0,
                        null);

        assertThat(shipment.getActualUploadCount()).isEqualTo(1);
    }

    @Test
    void processRecordOrThrow_shouldReject_whenDomShipmentSelectedEntityIsNotRuh() {
        Shipment shipment = CalculatorTestDataFactory.shipment();
        shipment.setProductGroup(BatchProcessingConstants.PRODUCT_GROUP_DOM);
        shipment.setConsigneeCountryCode("SA");
        shipment.setHAWBDestinationEntity("DXB");
        stubDomEntityFromCategoryCache("SA", DomEntityConstants.DEFAULT_DOM_ENTITY_CODE);
        when(shipmentLookupService.resolveByHawb("AWB-1001")).thenReturn(shipment);

        assertThatThrownBy(() -> service.processRecordOrThrow(
                CalculatorTestDataFactory.recordMap(),
                BatchProcessingConstants.PROCESS_FOR_ESTIMATE,
                fileIdString(),
                0,
                "DXB"))
                .isInstanceOf(ValidationException.class)
                .hasMessage(DomEntityConstants.formatDomEntityValidationMessage(
                        DomEntityConstants.DEFAULT_DOM_ENTITY_CODE));

        verify(shipmentTypeChargeCalculatorRouter, never()).getCalculators(any(), any());
    }

    @Test
    void processRecordOrThrow_shouldProceed_whenDomShipmentSelectedEntityIsRuh() {
        Shipment shipment = CalculatorTestDataFactory.shipment();
        shipment.setProductGroup(BatchProcessingConstants.PRODUCT_GROUP_DOM);
        shipment.setConsigneeCountryCode("SA");
        shipment.setHAWBDestinationEntity("DXB");
        stubDomEntityFromCategoryCache("SA", DomEntityConstants.DEFAULT_DOM_ENTITY_CODE);
        ChargeCalculation calculator = mock(ChargeCalculation.class);
        when(shipmentLookupService.resolveByHawb("AWB-1001")).thenReturn(shipment);
        when(chargeRepository.existsByShipment_IdAndStatus(
                shipment.getId(), BatchProcessingConstants.STATUS_ACTUALIZED)).thenReturn(false);
        when(shipmentTypeChargeCalculatorRouter.getCalculators(
                BatchProcessingConstants.PROCESS_FOR_ESTIMATE, shipment.getShipmentType()))
                .thenReturn(List.of(calculator));

        service.processRecordOrThrow(
                CalculatorTestDataFactory.recordMap(),
                BatchProcessingConstants.PROCESS_FOR_ESTIMATE,
                CalculatorTestDataFactory.fileId().toString(),
                0,
                "RUH");

        verify(calculator).calculate(any(ChargeCalculationContext.class));
    }

    @Test
    void processRecordOrThrow_shouldReject_whenSelectedEntityDoesNotMatchDestination() {
        Shipment shipment = CalculatorTestDataFactory.shipment();
        shipment.setHAWBDestinationEntity("RUH");
        when(shipmentLookupService.resolveByHawb("AWB-1001")).thenReturn(shipment);

        Map<String, String> record = CalculatorTestDataFactory.recordMap();
        String fileId = fileIdString();
        assertThatThrownBy(() -> service.processRecordOrThrow(
                record, BatchProcessingConstants.PROCESS_FOR_ESTIMATE, fileId, 0, "DXB"))
                .isInstanceOf(ValidationException.class)
                .hasMessage(BatchProcessingConstants.formatEntityDestinationMismatch("DXB", "RUH"));

        verify(shipmentTypeChargeCalculatorRouter, never()).getCalculators(any(), any());
    }

    @Test
    void processRecordOrThrow_shouldProceed_whenSelectedEntityMatchesDestination() {
        Shipment shipment = CalculatorTestDataFactory.shipment();
        shipment.setHAWBDestinationEntity("DXB");
        ChargeCalculation calculator = mock(ChargeCalculation.class);
        when(shipmentLookupService.resolveByHawb("AWB-1001")).thenReturn(shipment);
        when(recordProcessingLookups.hasActualizedCharge(shipment.getId())).thenReturn(false);
        when(shipmentTypeChargeCalculatorRouter.getCalculators(
                BatchProcessingConstants.PROCESS_FOR_ESTIMATE, shipment.getShipmentType()))
                .thenReturn(List.of(calculator));

        service.processRecordOrThrow(
                CalculatorTestDataFactory.recordMap(),
                BatchProcessingConstants.PROCESS_FOR_ESTIMATE,
                CalculatorTestDataFactory.fileId().toString(),
                0,
                "dxb");

        verify(calculator).calculate(any(ChargeCalculationContext.class));
    }

    @Test
    void processRecordOrThrow_shouldThrow_whenCalculatorRouterFails() {
        Shipment shipment = CalculatorTestDataFactory.shipment();
        when(shipmentLookupService.resolveByHawb("AWB-1001")).thenReturn(shipment);
        when(recordProcessingLookups.hasActualizedCharge(shipment.getId())).thenReturn(false);
        when(shipmentTypeChargeCalculatorRouter.getCalculators(any(), eq(shipment.getShipmentType())))
                .thenThrow(new ValidationException("calc failed"));

        Map<String, String> record = CalculatorTestDataFactory.recordMap();
        String fileId = fileIdString();
        assertThatThrownBy(() -> service.processRecordOrThrow(
                record, BatchProcessingConstants.PROCESS_FOR_ESTIMATE, fileId, 0, null))
                .hasMessage("calc failed");
    }

    private static String fileIdString() {
        return CalculatorTestDataFactory.fileId().toString();
    }
}
