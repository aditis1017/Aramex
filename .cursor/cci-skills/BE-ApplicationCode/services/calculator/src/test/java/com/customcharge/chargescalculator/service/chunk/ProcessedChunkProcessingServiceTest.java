package com.customcharge.chargescalculator.service.chunk;

import com.customcharge.chargescalculator.config.CustomChargeProperties;
import com.customcharge.chargescalculator.constants.BatchProcessingConstants;
import com.customcharge.chargescalculator.dto.ProcessedChunkMessage;
import com.customcharge.chargescalculator.service.batch.ChunkErrorPersistenceService;
import com.customcharge.chargescalculator.service.batch.ShipmentLookupService;
import com.customcharge.chargescalculator.service.batch.SingleRecordProcessorService;
import com.customcharge.chargescalculator.support.CalculatorTestDataFactory;
import com.customcharge.persistencelayer.exception.AbstractApiException;
import com.customcharge.persistencelayer.exception.ValidationException;
import com.customcharge.persistencelayer.observability.CustomChargeMetricsProperties;
import com.customcharge.persistencelayer.observability.FileProcessingMetrics;
import org.springframework.beans.factory.ObjectProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import jakarta.persistence.EntityManagerFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessedChunkProcessingServiceTest {

    @Mock
    private SingleRecordProcessorService singleRecordProcessorService;
    @Mock
    private ChunkErrorPersistenceService chunkErrorPersistenceService;
    @Mock
    private ShipmentLookupService shipmentLookupService;
    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private EntityManagerFactory entityManagerFactory;

    private ProcessedChunkProcessingService service;

    @BeforeEach
    void setUp() {
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(shipmentLookupService.prefetchByHawbNumbers(any())).thenReturn(Map.of());
        CustomChargeProperties properties = new CustomChargeProperties();
        properties.getChunk().setParallelRowsEnabled(false);
        ObjectProvider<FileProcessingMetrics> fileProcessingMetrics = org.mockito.Mockito.mock(ObjectProvider.class);
        CustomChargeMetricsProperties metricsProperties = new CustomChargeMetricsProperties();
        metricsProperties.getFileProcessing().setEnabled(false);
        service = new ProcessedChunkProcessingService(
                transactionManager,
                entityManagerFactory,
                singleRecordProcessorService,
                chunkErrorPersistenceService,
                shipmentLookupService,
                Runnable::run,
                properties,
                fileProcessingMetrics,
                metricsProperties);
    }

    @Test
    void processChunk_shouldPersistFailedRecords_whenProcessorThrows() {
        ProcessedChunkMessage message = CalculatorTestDataFactory.chunkMessage();
        Map<String, String> record = CalculatorTestDataFactory.recordMap();
        ValidationException ex = new ValidationException("negative amount");
        Map<String, String> failed = new HashMap<>(record);
        failed.put(BatchProcessingConstants.ERROR_KEY, "negative amount");

        doThrow(ex)
                .when(singleRecordProcessorService)
                .processRecordOrThrow(
                        eq(record),
                        eq(message.getProcessFor()),
                        eq(message.getFileId()),
                        eq(message.getChunkIndex()),
                        eq(message.getEntityCode()));
        when(singleRecordProcessorService.toFailedRecord(eq(record), any(AbstractApiException.class)))
                .thenReturn(failed);

        service.processChunk(message);

        ArgumentCaptor<List<Map<String, String>>> captor = ArgumentCaptor.forClass(List.class);
        verify(chunkErrorPersistenceService).persistErrors(eq(message), captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().getFirst()).containsEntry(BatchProcessingConstants.ERROR_KEY, "negative amount");
    }

    @Test
    void processChunk_shouldPrefetchShipmentsForChunkAwbs() {
        ProcessedChunkMessage message = CalculatorTestDataFactory.chunkMessage();

        service.processChunk(message);

        verify(shipmentLookupService).prefetchByHawbNumbers(any());
    }

    @Test
    void processChunk_shouldNotPersist_whenAllRecordsSucceed() {
        ProcessedChunkMessage message = CalculatorTestDataFactory.chunkMessage();

        service.processChunk(message);

        verify(chunkErrorPersistenceService, never()).persistErrors(any(), any());
    }

    @Test
    void processChunk_shouldPreserveFailureOrder_whenParallelRowsEnabled() {
        CustomChargeProperties properties = new CustomChargeProperties();
        properties.getChunk().setParallelRowsEnabled(true);
        Executor directExecutor = Runnable::run;
        ObjectProvider<FileProcessingMetrics> fileProcessingMetrics = org.mockito.Mockito.mock(ObjectProvider.class);
        CustomChargeMetricsProperties metricsProperties = new CustomChargeMetricsProperties();
        metricsProperties.getFileProcessing().setEnabled(false);
        ProcessedChunkProcessingService parallelService = new ProcessedChunkProcessingService(
                transactionManager,
                entityManagerFactory,
                singleRecordProcessorService,
                chunkErrorPersistenceService,
                shipmentLookupService,
                directExecutor,
                properties,
                fileProcessingMetrics,
                metricsProperties);

        ProcessedChunkMessage message = CalculatorTestDataFactory.chunkMessage();
        Map<String, String> firstRecord = CalculatorTestDataFactory.recordMap();
        Map<String, String> secondRecord = new HashMap<>(firstRecord);
        secondRecord.put("awb", "AWB-1002");
        message.setRecords(List.of(firstRecord, secondRecord));

        ValidationException firstEx = new ValidationException("first failure");
        ValidationException secondEx = new ValidationException("second failure");
        Map<String, String> firstFailed = new HashMap<>(firstRecord);
        firstFailed.put(BatchProcessingConstants.ERROR_KEY, "first failure");
        Map<String, String> secondFailed = new HashMap<>(secondRecord);
        secondFailed.put(BatchProcessingConstants.ERROR_KEY, "second failure");

        doThrow(firstEx)
                .when(singleRecordProcessorService)
                .processRecordOrThrow(
                        eq(firstRecord),
                        eq(message.getProcessFor()),
                        eq(message.getFileId()),
                        eq(message.getChunkIndex()),
                        eq(message.getEntityCode()));
        doThrow(secondEx)
                .when(singleRecordProcessorService)
                .processRecordOrThrow(
                        eq(secondRecord),
                        eq(message.getProcessFor()),
                        eq(message.getFileId()),
                        eq(message.getChunkIndex()),
                        eq(message.getEntityCode()));
        when(singleRecordProcessorService.toFailedRecord(eq(firstRecord), eq(firstEx))).thenReturn(firstFailed);
        when(singleRecordProcessorService.toFailedRecord(eq(secondRecord), eq(secondEx))).thenReturn(secondFailed);

        parallelService.processChunk(message);

        ArgumentCaptor<List<Map<String, String>>> captor = ArgumentCaptor.forClass(List.class);
        verify(chunkErrorPersistenceService).persistErrors(eq(message), captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue().get(0)).containsEntry(BatchProcessingConstants.ERROR_KEY, "first failure");
        assertThat(captor.getValue().get(1)).containsEntry(BatchProcessingConstants.ERROR_KEY, "second failure");
    }
}
