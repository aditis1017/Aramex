package com.customcharge.chargescalculator.service.batch;

import com.customcharge.chargescalculator.dto.ChunkProcessingResult;
import com.customcharge.chargescalculator.dto.ProcessedChunkMessage;
import com.customcharge.chargescalculator.recordtracking.RecordTrackingChunkAcceptanceTracker;
import com.customcharge.chargescalculator.service.chunk.ProcessedChunkProcessingService;
import com.customcharge.persistencelayer.observability.ChunkRecordMetricsReporter;
import com.customcharge.persistencelayer.observability.CustomChargeMetricsProperties;
import com.customcharge.persistencelayer.observability.FileProcessingMetrics;
import com.customcharge.persistencelayer.service.ChunkBeginResult;
import com.customcharge.persistencelayer.service.ProcessedChunkService;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ChunkProcessingCoordinator {

    private static final Logger log = LoggerFactory.getLogger(ChunkProcessingCoordinator.class);

    private final ProcessedChunkProcessingService processedChunkProcessingService;
    private final ProcessedChunkService processedChunkService;
    private final FileChunkCompletionService fileChunkCompletionService;
    private final ObjectProvider<RecordTrackingChunkAcceptanceTracker> recordTrackingChunkAcceptanceTracker;
    private final ObjectProvider<FileProcessingMetrics> fileProcessingMetrics;
    private final CustomChargeMetricsProperties metricsProperties;

    public ChunkProcessingCoordinator(
            ProcessedChunkProcessingService processedChunkProcessingService,
            ProcessedChunkService processedChunkService,
            FileChunkCompletionService fileChunkCompletionService,
            ObjectProvider<RecordTrackingChunkAcceptanceTracker> recordTrackingChunkAcceptanceTracker,
            ObjectProvider<FileProcessingMetrics> fileProcessingMetrics,
            CustomChargeMetricsProperties metricsProperties) {
        this.processedChunkProcessingService = processedChunkProcessingService;
        this.processedChunkService = processedChunkService;
        this.fileChunkCompletionService = fileChunkCompletionService;
        this.recordTrackingChunkAcceptanceTracker = recordTrackingChunkAcceptanceTracker;
        this.fileProcessingMetrics = fileProcessingMetrics;
        this.metricsProperties = metricsProperties;
    }

    public void process(ProcessedChunkMessage message) {
        UUID fileId = parseFileId(message.getFileId());
        int chunkIndex = message.getChunkIndex();

        ChunkBeginResult beginResult = processedChunkService.tryBeginChunk(fileId, chunkIndex);
        switch (beginResult) {
            case ALREADY_COMPLETED -> {
                log.debug("Skipping already completed chunk fileId={} chunkIndex={}", fileId, chunkIndex);
                fileChunkCompletionService.maybeTriggerErrorReport(message, fileId);
                return;
            }
            case IN_PROGRESS -> {
                log.warn(
                        "Deferring chunk until in-flight processing completes fileId={} chunkIndex={} traceId={}",
                        fileId,
                        chunkIndex,
                        message.getTraceId());
                throw new ChunkInProgressException(fileId, chunkIndex);
            }
            case ACCEPTED -> {
                // proceed
            }
        }

        RecordTrackingChunkAcceptanceTracker tracker = recordTrackingChunkAcceptanceTracker.getIfAvailable();
        if (tracker != null) {
            tracker.onChunkAccepted(message);
        }

        Timer.Sample chunkTimer = startChunkTimer();
        long chunkStartedAtNanos = System.nanoTime();
        try {
            if (log.isDebugEnabled()) {
                log.debug(
                        "Chunk process started fileId={} chunkIndex={} recordCount={} traceId={}",
                        fileId,
                        chunkIndex,
                        resolveRecordCount(message),
                        message.getTraceId());
            }
            ChunkProcessingResult result = processedChunkProcessingService.processChunk(message);
            processedChunkService.markChunkCompleted(fileId, chunkIndex);
            recordChunkRecordMetrics(message, result, chunkTimer, System.nanoTime() - chunkStartedAtNanos);
            if (log.isDebugEnabled()) {
                log.debug(
                        "Chunk process completed fileId={} chunkIndex={} traceId={}",
                        fileId,
                        chunkIndex,
                        message.getTraceId());
            }
        } catch (Exception ex) {
            processedChunkService.releaseChunk(fileId, chunkIndex);
            throw ex;
        }
        fileChunkCompletionService.maybeTriggerErrorReport(message, fileId);
    }

    public static UUID parseFileId(String rawFileId) {
        if (rawFileId == null || rawFileId.isBlank()) {
            throw new IllegalArgumentException("fileId is required");
        }
        try {
            return UUID.fromString(rawFileId.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid fileId: " + rawFileId, ex);
        }
    }

    private static int resolveRecordCount(ProcessedChunkMessage message) {
        if (message.getRecordCount() > 0) {
            return message.getRecordCount();
        }
        return message.getRecords() != null ? message.getRecords().size() : 0;
    }

    private Timer.Sample startChunkTimer() {
        if (!metricsProperties.getFileProcessing().isEnabled()) {
            return null;
        }
        FileProcessingMetrics metrics = fileProcessingMetrics.getIfAvailable();
        return metrics != null ? metrics.startTimer() : null;
    }

    private void recordChunkRecordMetrics(
            ProcessedChunkMessage message,
            ChunkProcessingResult result,
            Timer.Sample chunkTimer,
            long chunkDurationNanos) {
        if (!metricsProperties.getFileProcessing().isEnabled() || result == null) {
            return;
        }
        FileProcessingMetrics metrics = fileProcessingMetrics.getIfAvailable();
        if (metrics == null) {
            return;
        }
        String outcome = FileProcessingMetrics.outcome(result.failedCount() == 0);
        if (chunkTimer != null) {
            metrics.recordChunkProcessingDuration(chunkTimer, message.getProcessFor(), outcome);
        }
        metrics.recordChunkRecordsCount(result.totalRecords(), message.getProcessFor());
        if (result.failedCount() > 0) {
            metrics.recordChunkFailedRecordsCount(result.failedCount(), message.getProcessFor());
        }
        ChunkRecordMetricsReporter.recordChunkOutcomes(
                metrics,
                message.getProcessFor(),
                message.getEntityCode(),
                result.totalRecords(),
                result.failedRecords(),
                chunkDurationNanos);
    }
}
