package com.customcharge.fileorchestration.recordtracking;

import com.customcharge.fileorchestration.pipeline.FileProcessingRequest;
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
 * File-level record tracking for the custom orchestration pipeline (replaces Spring Batch job listener).
 */
@Component
@ConditionalOnProperty(prefix = "customcharge.record-tracking", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FileOrchestrationPipelineTracker {

    private final RecordTrackingLogger recordTrackingLogger;
    private final ObjectProvider<FileProcessingMetrics> fileProcessingMetrics;
    private final CustomChargeMetricsProperties metricsProperties;
    private final String serviceName;

    public FileOrchestrationPipelineTracker(
            RecordTrackingLogger recordTrackingLogger,
            ObjectProvider<FileProcessingMetrics> fileProcessingMetrics,
            CustomChargeMetricsProperties metricsProperties,
            @Value("${spring.application.name}") String serviceName) {
        this.recordTrackingLogger = recordTrackingLogger;
        this.fileProcessingMetrics = fileProcessingMetrics;
        this.metricsProperties = metricsProperties;
        this.serviceName = serviceName;
    }

    public void onFileReceived(FileProcessingRequest request) {
        RecordTrackingGuard.runSafely(() -> {
            applyFileContext(request);
            RecordTrackingTimeline.markTraceStart();
            recordTrackingLogger.logEvent(
                    RecordTrackingEvent.FILE_RECEIVED,
                    RecordContext.fileMetadata(request.fileName(), request.fileType(), request.processFor()));
            metricsIfEnabled(metrics -> metrics.recordFileReceived(
                    request.fileType(),
                    request.processFor(),
                    request.entityCode(),
                    request.uploadedBy()));
        });
    }

    public void onOrchestrationFinished(
            FileProcessingRequest request,
            long durationMs,
            int totalRows,
            int droppedRows,
            int publishedChunks) {
        RecordTrackingGuard.runSafely(() -> {
            try {
                applyFileContext(request);
                metricsIfEnabled(metrics -> {
                    metrics.recordFileOrchestrationDuration(
                            TimeUnit.MILLISECONDS.toNanos(durationMs),
                            request.fileType(),
                            request.processFor(),
                            FileProcessingMetrics.outcome(true));
                    metrics.recordOrchestrationRowsTotal(totalRows, request.fileType());
                    metrics.recordOrchestrationRowsDeduped(droppedRows, request.fileType());
                    metrics.recordFileOrchestrationChunksPublished(
                            publishedChunks, request.fileType(), request.processFor());
                });
            } finally {
                RecordTrackingTimeline.clearAll();
                RecordTrackingMdc.clear();
            }
        });
    }

    public void onFileFailed(FileProcessingRequest request, Throwable failure, long durationMs) {
        RecordTrackingGuard.runSafely(() -> {
            try {
                applyFileContext(request);
                RecordContext context = RecordContext.fileMetadata(
                        request.fileName(), request.fileType(), request.processFor());
                String message = failure != null && failure.getMessage() != null && !failure.getMessage().isBlank()
                        ? failure.getMessage()
                        : "File orchestration failed";
                recordTrackingLogger.logFailure(
                        RecordTrackingEvent.FILE_BATCH_FAILED,
                        context,
                        new IllegalStateException(message, failure),
                        durationMs);
                metricsIfEnabled(metrics -> {
                    metrics.recordFileOrchestrationFailed(request.fileType(), request.processFor());
                    metrics.recordFileOrchestrationDuration(
                            TimeUnit.MILLISECONDS.toNanos(durationMs),
                            request.fileType(),
                            request.processFor(),
                            FileProcessingMetrics.outcome(false));
                });
            } finally {
                RecordTrackingTimeline.clearAll();
                RecordTrackingMdc.clear();
            }
        });
    }

    private void applyFileContext(FileProcessingRequest request) {
        RecordTrackingMdc.putContext(request.traceId(), request.fileId(), null, null, serviceName);
        RecordTrackingMdc.putFileName(request.fileName());
        RecordTrackingMdc.putProcessFor(request.processFor());
        RecordTrackingMdc.applyFileIdentity(request.uploadedBy(), request.entityCode());
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
