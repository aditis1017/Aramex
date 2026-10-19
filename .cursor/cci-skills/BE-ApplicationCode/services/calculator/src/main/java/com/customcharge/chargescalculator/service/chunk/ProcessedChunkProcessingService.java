package com.customcharge.chargescalculator.service.chunk;

import com.customcharge.chargescalculator.config.CustomChargeProperties;
import com.customcharge.chargescalculator.dto.ChunkProcessingResult;
import com.customcharge.chargescalculator.dto.ProcessedChunkMessage;
import com.customcharge.chargescalculator.service.batch.ChunkErrorPersistenceService;
import com.customcharge.chargescalculator.service.batch.ChunkProcessingCoordinator;
import com.customcharge.chargescalculator.service.batch.ChunkShipmentPrefetchScope;
import com.customcharge.chargescalculator.service.batch.ShipmentLookupService;
import com.customcharge.chargescalculator.service.batch.SingleRecordProcessorService;
import com.customcharge.chargescalculator.util.RecordMapSupport;
import com.customcharge.persistencelayer.RecordPayloadMetaKeys;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.exception.AbstractApiException;
import com.customcharge.persistencelayer.observability.CustomChargeMetricsProperties;
import com.customcharge.persistencelayer.observability.FileProcessingMetrics;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Processes all rows in a {@link ProcessedChunkMessage}: one transaction per row (parity with former
 * Spring Batch {@code chunk(1)}), then persists aggregated validation/business failures in a separate transaction.
 * Rows may run in parallel when {@code custom-charge.chunk.parallel-rows-enabled} is true.
 */
@Service
public class ProcessedChunkProcessingService {

    private static final Logger log = LoggerFactory.getLogger(ProcessedChunkProcessingService.class);
    private static final String AWB_KEY = "awb";

    private final SingleRecordProcessorService singleRecordProcessorService;
    private final ChunkErrorPersistenceService chunkErrorPersistenceService;
    private final ShipmentLookupService shipmentLookupService;
    private final EntityManagerFactory entityManagerFactory;
    private final TransactionTemplate rowTransactionTemplate;
    private final TransactionTemplate persistErrorsTransactionTemplate;
    private final Executor chunkRowExecutor;
    private final boolean parallelRowsEnabled;
    private final ObjectProvider<FileProcessingMetrics> fileProcessingMetrics;
    private final CustomChargeMetricsProperties metricsProperties;

    public ProcessedChunkProcessingService(
            PlatformTransactionManager transactionManager,
            EntityManagerFactory entityManagerFactory,
            SingleRecordProcessorService singleRecordProcessorService,
            ChunkErrorPersistenceService chunkErrorPersistenceService,
            ShipmentLookupService shipmentLookupService,
            @Qualifier("chunkRowExecutor") Executor chunkRowExecutor,
            CustomChargeProperties customChargeProperties,
            ObjectProvider<FileProcessingMetrics> fileProcessingMetrics,
            CustomChargeMetricsProperties metricsProperties) {
        this.singleRecordProcessorService = singleRecordProcessorService;
        this.chunkErrorPersistenceService = chunkErrorPersistenceService;
        this.shipmentLookupService = shipmentLookupService;
        this.entityManagerFactory = entityManagerFactory;
        this.rowTransactionTemplate = new TransactionTemplate(transactionManager, requiresNewDefinition());
        this.persistErrorsTransactionTemplate = new TransactionTemplate(transactionManager, requiresNewDefinition());
        this.chunkRowExecutor = chunkRowExecutor;
        this.parallelRowsEnabled = customChargeProperties.getChunk().isParallelRowsEnabled();
        this.fileProcessingMetrics = fileProcessingMetrics;
        this.metricsProperties = metricsProperties;
    }

    private static DefaultTransactionDefinition requiresNewDefinition() {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return def;
    }

    public ChunkProcessingResult processChunk(ProcessedChunkMessage message) {
        ChunkProcessingCoordinator.parseFileId(message.getFileId());
        List<Map<String, String>> records = message.getRecords() != null ? message.getRecords() : List.of();
        Map<String, Shipment> prefetchedShipments = shipmentLookupService.prefetchByHawbNumbers(extractAwbs(records));
        try {
            ChunkShipmentPrefetchScope.open(prefetchedShipments);
            List<Map<String, String>> failedRecords = parallelRowsEnabled
                    ? processRowsInParallel(message, records)
                    : processRowsSequentially(message, records);
            if (!failedRecords.isEmpty()) {
                persistErrorsTransactionTemplate.executeWithoutResult(status ->
                        chunkErrorPersistenceService.persistErrors(message, failedRecords));
            }
            return new ChunkProcessingResult(records.size(), failedRecords);
        } finally {
            ChunkShipmentPrefetchScope.close();
        }
    }

    private static List<String> extractAwbs(List<Map<String, String>> records) {
        List<String> awbs = new ArrayList<>(records.size());
        for (Map<String, String> recordData : records) {
            Map<String, String> normalized = RecordMapSupport.lowercaseKeys(recordData);
            String awb = normalized.get(AWB_KEY);
            if (awb != null && !awb.isBlank()) {
                awbs.add(awb);
            }
        }
        return awbs;
    }

    private List<Map<String, String>> processRowsSequentially(
            ProcessedChunkMessage message, List<Map<String, String>> records) {
        List<Map<String, String>> failedRecords = new ArrayList<>();
        for (Map<String, String> recordData : records) {
            Map<String, String> failed = processRow(message, recordData);
            if (failed != null) {
                failedRecords.add(failed);
            }
        }
        return failedRecords;
    }

    private List<Map<String, String>> processRowsInParallel(
            ProcessedChunkMessage message, List<Map<String, String>> records) {
        if (records.isEmpty()) {
            return List.of();
        }
        List<CompletableFuture<RowOutcome>> futures = new ArrayList<>(records.size());
        for (int index = 0; index < records.size(); index++) {
            final int rowIndex = index;
            final Map<String, String> recordData = records.get(index);
            futures.add(CompletableFuture.supplyAsync(
                    () -> new RowOutcome(rowIndex, processRow(message, recordData)),
                    chunkRowExecutor));
        }
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        return futures.stream()
                .map(CompletableFuture::join)
                .filter(outcome -> outcome.failedRecord() != null)
                .sorted(Comparator.comparingInt(RowOutcome::index))
                .map(RowOutcome::failedRecord)
                .toList();
    }

    private Map<String, String> processRow(ProcessedChunkMessage message, Map<String, String> recordData) {
        Map<String, String> normalized = RecordMapSupport.lowercaseKeys(recordData);
        String awb = normalized.get(AWB_KEY);
        String fileRowIndex = recordData.get(RecordPayloadMetaKeys.META_FILE_ROW_INDEX);
        long startedAtNanos = System.nanoTime();
        if (log.isDebugEnabled()) {
            log.debug(
                    "Record process started fileId={} chunkIndex={} awb={} fileRowIndex={}",
                    message.getFileId(),
                    message.getChunkIndex(),
                    awb,
                    fileRowIndex);
        }
        try {
            rowTransactionTemplate.executeWithoutResult(status -> {
                EntityManager entityManager =
                        EntityManagerFactoryUtils.getTransactionalEntityManager(entityManagerFactory);
                if (entityManager != null) {
                    entityManager.setFlushMode(FlushModeType.COMMIT);
                }
                singleRecordProcessorService.processRecordOrThrow(
                        recordData,
                        message.getProcessFor(),
                        message.getFileId(),
                        message.getChunkIndex(),
                        message.getEntityCode());
            });
            if (log.isDebugEnabled()) {
                log.debug(
                        "Record process completed fileId={} chunkIndex={} awb={} fileRowIndex={} durationMs={}",
                        message.getFileId(),
                        message.getChunkIndex(),
                        awb,
                        fileRowIndex,
                        (System.nanoTime() - startedAtNanos) / 1_000_000L);
            }
            recordRowProcessingDuration(startedAtNanos, message);
            return null;
        } catch (AbstractApiException ex) {
            if (log.isDebugEnabled()) {
                log.debug(
                        "Record process failed fileId={} chunkIndex={} awb={} fileRowIndex={} durationMs={} reason={}",
                        message.getFileId(),
                        message.getChunkIndex(),
                        awb,
                        fileRowIndex,
                        (System.nanoTime() - startedAtNanos) / 1_000_000L,
                        ex.getMessage());
            }
            recordRowProcessingDuration(startedAtNanos, message);
            return singleRecordProcessorService.toFailedRecord(recordData, ex);
        }
    }

    private void recordRowProcessingDuration(long startedAtNanos, ProcessedChunkMessage message) {
        if (!metricsProperties.getFileProcessing().isEnabled()) {
            return;
        }
        FileProcessingMetrics metrics = fileProcessingMetrics.getIfAvailable();
        if (metrics == null) {
            return;
        }
        metrics.recordRecordProcessingDurationNanos(
                System.nanoTime() - startedAtNanos,
                message.getProcessFor(),
                message.getEntityCode());
    }

    private record RowOutcome(int index, Map<String, String> failedRecord) {
    }
}
