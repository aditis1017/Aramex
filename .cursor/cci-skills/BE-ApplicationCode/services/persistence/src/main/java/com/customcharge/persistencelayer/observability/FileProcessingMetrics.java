package com.customcharge.persistencelayer.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.util.concurrent.TimeUnit;


public class FileProcessingMetrics {

    public static final String PREFIX = "customcharge.file.processing";

    private final MeterRegistry registry;

    public FileProcessingMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public Timer.Sample startTimer() {
        return Timer.start(registry);
    }

    public void recordFileReceived(String fileType, String processFor) {
        recordFileReceived(fileType, processFor, null, null);
    }

    public void recordFileReceived(String fileType, String processFor, String entityCode, String uploadedBy) {
        safe(() -> counter("files.received",
                "file_type", fileType,
                "process_for", processFor,
                "entity_code", entityCode,
                "uploaded_by", uploadedBy).increment());
    }

    public void recordFileOrchestrationDuration(long durationNanos, String fileType, String processFor, String outcome) {
        safe(() -> timer("files.orchestration.duration",
                "file_type", fileType,
                "process_for", processFor,
                "outcome", outcome)
                .record(durationNanos, TimeUnit.NANOSECONDS));
    }

    public void recordFileOrchestrationFailed(String fileType, String processFor) {
        safe(() -> counter("files.orchestration.failed",
                "file_type", fileType,
                "process_for", processFor).increment());
    }

    public void recordOrchestrationRowsTotal(int rowCount, String fileType) {
        safe(() -> distribution("files.orchestration.rows.total", "file_type", fileType).record(rowCount));
    }

    public void recordOrchestrationRowsDeduped(int droppedCount, String fileType) {
        if (droppedCount <= 0) {
            return;
        }
        safe(() -> counter("files.orchestration.rows.deduped", "file_type", fileType).increment(droppedCount));
    }

    public void recordFileOrchestrationChunksPublished(String fileType, String processFor) {
        recordFileOrchestrationChunksPublished(1, fileType, processFor);
    }

    public void recordFileOrchestrationChunksPublished(int chunkCount, String fileType, String processFor) {
        if (chunkCount <= 0) {
            return;
        }
        safe(() -> counter("files.orchestration.chunks.published",
                "file_type", fileType,
                "process_for", processFor).increment(chunkCount));
    }

    public void recordFileCalculatorCompleted(String processFor, String outcome) {
        safe(() -> counter("files.calculator.completed",
                "process_for", processFor,
                "outcome", outcome).increment());
    }

    public void recordFileCalculatorEndToEndDuration(long durationNanos, String processFor, String fileType) {
        recordFileCalculatorEndToEndDuration(durationNanos, processFor, fileType, null);
    }

    public void recordFileCalculatorEndToEndDuration(
            long durationNanos, String processFor, String fileType, String entityCode) {
        safe(() -> timer("files.calculator.end_to_end.duration",
                "process_for", processFor,
                "file_type", fileType,
                "entity_code", entityCode)
                .record(durationNanos, TimeUnit.NANOSECONDS));
    }

    public void recordChunkPublished(String processFor, String fileType) {
        safe(() -> counter("chunks.published",
                "process_for", processFor,
                "file_type", fileType).increment());
    }

    public void recordChunkPublishDuration(Timer.Sample sample, String outcome) {
        safe(() -> sample.stop(timer("chunks.publish.duration", "outcome", outcome)));
    }

    public void recordChunkReceived(String processFor) {
        safe(() -> counter("chunks.received", "process_for", processFor).increment());
    }

    public void recordChunkProcessingDuration(Timer.Sample sample, String processFor, String outcome) {
        safe(() -> sample.stop(timer("chunks.processing.duration",
                "process_for", processFor,
                "outcome", outcome)));
    }

    public void recordChunkFailed(String processFor) {
        safe(() -> counter("chunks.failed", "process_for", processFor).increment());
    }

    public void recordChunkSkippedDuplicate(String reason) {
        safe(() -> counter("chunks.skipped.duplicate", "reason", reason).increment());
    }

    public void recordChunkRecordsCount(int recordCount, String processFor) {
        safe(() -> distribution("chunks.records.count", "process_for", processFor).record(recordCount));
    }

    public void recordChunkFailedRecordsCount(int failedCount, String processFor) {
        safe(() -> distribution("chunks.failed_records.count", "process_for", processFor).record(failedCount));
    }

    public void recordRecordProcessed(String processFor, String outcome) {
        recordRecordProcessed(processFor, outcome, null);
    }

    public void recordRecordProcessed(String processFor, String outcome, String entityCode) {
        safe(() -> counter("records.processed",
                "process_for", processFor,
                "outcome", outcome,
                "entity_code", entityCode).increment());
    }

    public void recordRecordOutcomes(int successCount, int failedCount, String processFor, String entityCode) {
        if (successCount > 0) {
            safe(() -> counter("records.processed",
                    "process_for", processFor,
                    "outcome", outcome(true),
                    "entity_code", entityCode).increment(successCount));
        }
        if (failedCount > 0) {
            safe(() -> counter("records.processed",
                    "process_for", processFor,
                    "outcome", outcome(false),
                    "entity_code", entityCode).increment(failedCount));
        }
    }

    public void recordRecordFailures(int count, String processFor, String errorCode, String entityCode) {
        if (count <= 0) {
            return;
        }
        safe(() -> counter("records.failed",
                "process_for", processFor,
                "error_code", errorCode,
                "entity_code", entityCode).increment(count));
    }

    public void recordRecordProcessingDuration(Timer.Sample sample, String processFor) {
        recordRecordProcessingDuration(sample, processFor, null);
    }

    public void recordRecordProcessingDuration(Timer.Sample sample, String processFor, String entityCode) {
        safe(() -> sample.stop(timer("records.processing.duration",
                "process_for", processFor,
                "entity_code", entityCode)));
    }

    public void recordRecordProcessingDurationNanos(long durationNanos, String processFor, String entityCode) {
        if (durationNanos <= 0L) {
            return;
        }
        safe(() -> timer("records.processing.duration",
                "process_for", processFor,
                "entity_code", entityCode).record(durationNanos, TimeUnit.NANOSECONDS));
    }

    public void recordRecordPhaseDuration(long durationMs, String phase, String processFor) {
        if (durationMs < 0L || phase == null || phase.isBlank()) {
            return;
        }
        safe(() -> timer("records.processing.phase.duration",
                "phase", phase,
                "process_for", processFor).record(durationMs, TimeUnit.MILLISECONDS));
    }

    /**
     * Chunk wall time divided by row count (includes parallel row overlap). Not true per-row CPU/DB time.
     */
    public void recordChunkAverageRecordProcessingDuration(
            long chunkDurationNanos, int recordCount, String processFor, String entityCode) {
        if (recordCount <= 0 || chunkDurationNanos <= 0L) {
            return;
        }
        long perRecordNanos = chunkDurationNanos / recordCount;
        safe(() -> timer("chunks.processing.wall_per_record.duration",
                "process_for", processFor,
                "entity_code", entityCode).record(perRecordNanos, TimeUnit.NANOSECONDS));
    }

    public void recordRecordFailed(String processFor, String errorCode) {
        recordRecordFailed(processFor, errorCode, null);
    }

    public void recordRecordFailed(String processFor, String errorCode, String entityCode) {
        safe(() -> counter("records.failed",
                "process_for", processFor,
                "error_code", errorCode,
                "entity_code", entityCode).increment());
    }

    public void compensateRecordFailed(String processFor, String errorCode, String entityCode) {
        safe(() -> counter("records.failed",
                "process_for", processFor,
                "error_code", errorCode,
                "entity_code", entityCode).increment(-1));
    }

    public void compensateRecordProcessed(String processFor, String outcome, String entityCode) {
        safe(() -> counter("records.processed",
                "process_for", processFor,
                "outcome", outcome,
                "entity_code", entityCode).increment(-1));
    }

    public void recordRecordFailedUnexpected(String processFor) {
        safe(() -> counter("records.failed.unexpected", "process_for", processFor).increment());
    }

    public void recordSqsPublishFailed(String queue) {
        safe(() -> counter("sqs.publish.failed", "queue", queue).increment());
    }

    public void recordSqsPoisonMessage(String queue) {
        safe(() -> counter("sqs.messages.poison", "queue", queue).increment());
    }

    public void recordSqsListenerDuration(Timer.Sample sample, String outcome) {
        safe(() -> sample.stop(timer("sqs.listener.duration", "outcome", outcome)));
    }

    private Counter counter(String name, String... tags) {
        return Counter.builder(PREFIX + "." + name).tags(normalizeTags(tags)).register(registry);
    }

    private Timer timer(String name, String... tags) {
        return Timer.builder(PREFIX + "." + name).tags(normalizeTags(tags)).register(registry);
    }

    private DistributionSummary distribution(String name, String... tags) {
        return DistributionSummary.builder(PREFIX + "." + name).tags(normalizeTags(tags)).register(registry);
    }

    private String[] normalizeTags(String... tags) {
        if (tags == null || tags.length == 0) {
            return tags;
        }
        String[] normalized = new String[tags.length];
        for (int i = 0; i < tags.length; i++) {
            normalized[i] = i % 2 == 1 ? normalize(tags[i]) : tags[i];
        }
        return normalized;
    }

    public static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim();
    }

    public static String outcome(boolean success) {
        return success ? "success" : "failed";
    }

    public static String errorCode(String code) {
        return normalize(code);
    }

    private static void safe(Runnable action) {
        try {
            action.run();
        } catch (Exception ignored) {
            // Metrics must never affect business flow.
        }
    }
}
