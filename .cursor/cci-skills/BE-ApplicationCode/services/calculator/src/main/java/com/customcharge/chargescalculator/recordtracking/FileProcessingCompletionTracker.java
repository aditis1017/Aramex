package com.customcharge.chargescalculator.recordtracking;

import com.customcharge.chargescalculator.dto.ProcessedChunkMessage;
import com.customcharge.persistencelayer.observability.CustomChargeMetricsProperties;
import com.customcharge.persistencelayer.observability.FileProcessingMetrics;
import com.customcharge.persistencelayer.recordtracking.RecordContext;
import com.customcharge.persistencelayer.recordtracking.RecordTrackingEvent;
import com.customcharge.persistencelayer.recordtracking.RecordTrackingGuard;
import com.customcharge.persistencelayer.recordtracking.RecordTrackingLogger;
import com.customcharge.persistencelayer.recordtracking.RecordTrackingMdc;
import com.customcharge.persistencelayer.recordtracking.RecordTrackingTimeline;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Emits {@link RecordTrackingEvent#FILE_BATCH_COMPLETED} once all calculator chunks finish,
 * with end-to-end wall-clock duration from orchestration start.
 */
@Component
@ConditionalOnProperty(prefix = "customcharge.record-tracking", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FileProcessingCompletionTracker {

    private final RecordTrackingLogger recordTrackingLogger;
    private final ObjectProvider<FileProcessingMetrics> fileProcessingMetrics;
    private final CustomChargeMetricsProperties metricsProperties;
    private final String serviceName;

    public FileProcessingCompletionTracker(
            RecordTrackingLogger recordTrackingLogger,
            ObjectProvider<FileProcessingMetrics> fileProcessingMetrics,
            CustomChargeMetricsProperties metricsProperties,
            @Value("${spring.application.name}") String serviceName) {
        this.recordTrackingLogger = recordTrackingLogger;
        this.fileProcessingMetrics = fileProcessingMetrics;
        this.metricsProperties = metricsProperties;
        this.serviceName = serviceName;
    }

    public void onFileFullyProcessed(ProcessedChunkMessage message) {
        RecordTrackingGuard.runSafely(() -> {
            try {
                applyFileContext(message);
                Long startedAtEpochMs = message.getFileProcessingStartedAtEpochMs();
                if (startedAtEpochMs != null && startedAtEpochMs > 0L) {
                    RecordTrackingTimeline.markTraceStartFromEpoch(startedAtEpochMs);
                } else {
                    RecordTrackingTimeline.markTraceStart();
                }
                long durationMs = startedAtEpochMs != null && startedAtEpochMs > 0L
                        ? RecordTrackingTimeline.durationSince(startedAtEpochMs)
                        : 0L;
                RecordContext context = RecordContext.fileCompletion(
                        message.getFileName(),
                        message.getFileType(),
                        message.getProcessFor(),
                        message.getRecordCount() > 0 ? message.getRecordCount() : null);
                recordTrackingLogger.logEvent(RecordTrackingEvent.FILE_BATCH_COMPLETED, context, durationMs);
                metricsIfEnabled(metrics -> {
                    metrics.recordFileCalculatorCompleted(message.getProcessFor(), FileProcessingMetrics.outcome(true));
                    if (durationMs > 0L) {
                        metrics.recordFileCalculatorEndToEndDuration(
                                TimeUnit.MILLISECONDS.toNanos(durationMs),
                                message.getProcessFor(),
                                message.getFileType(),
                                message.getEntityCode());
                    }
                });
            } finally {
                RecordTrackingTimeline.clearAll();
                RecordTrackingMdc.clear();
            }
        });
    }

    private void applyFileContext(ProcessedChunkMessage message) {
        RecordTrackingMdc.putContext(
                message.getTraceId(),
                message.getFileId(),
                message.getChunkIndex(),
                null,
                serviceName);
        RecordTrackingMdc.putFileName(message.getFileName());
        RecordTrackingMdc.putProcessFor(message.getProcessFor());
        RecordTrackingMdc.applyFileIdentity(message.getUploadedBy(), message.getEntityCode());
    }

    private void metricsIfEnabled(java.util.function.Consumer<FileProcessingMetrics> action) {
        if (!metricsProperties.getFileProcessing().isEnabled()) {
            return;
        }
        FileProcessingMetrics metrics = fileProcessingMetrics.getIfAvailable();
        if (metrics != null) {
            action.accept(metrics);
        }
    }
}
