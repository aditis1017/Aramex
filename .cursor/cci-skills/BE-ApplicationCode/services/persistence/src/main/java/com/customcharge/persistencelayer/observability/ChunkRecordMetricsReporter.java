package com.customcharge.persistencelayer.observability;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Emits record outcome counters once per completed chunk. No per-row JVM state; Micrometer holds aggregates.
 */
public final class ChunkRecordMetricsReporter {

    private static final String FAILED_RECORD_ERROR_CODE_KEY = "errorCode";

    private ChunkRecordMetricsReporter() {
    }

    public static void recordChunkOutcomes(
            FileProcessingMetrics metrics,
            String processFor,
            String entityCode,
            int totalRecords,
            List<Map<String, String>> failedRecords,
            long chunkDurationNanos) {
        if (metrics == null || totalRecords <= 0) {
            return;
        }
        int failedCount = failedRecords != null ? failedRecords.size() : 0;
        int successCount = totalRecords - failedCount;
        metrics.recordRecordOutcomes(successCount, failedCount, processFor, entityCode);
        metrics.recordChunkAverageRecordProcessingDuration(chunkDurationNanos, totalRecords, processFor, entityCode);
        if (failedCount <= 0 || failedRecords == null) {
            return;
        }
        Map<String, Integer> failuresByCode = aggregateFailuresByErrorCode(failedRecords);
        for (Map.Entry<String, Integer> entry : failuresByCode.entrySet()) {
            metrics.recordRecordFailures(entry.getValue(), processFor, entry.getKey(), entityCode);
        }
    }

    private static Map<String, Integer> aggregateFailuresByErrorCode(List<Map<String, String>> failedRecords) {
        Map<String, Integer> counts = new HashMap<>();
        for (Map<String, String> failedRecord : failedRecords) {
            String errorCode = FileProcessingMetrics.errorCode(
                    failedRecord != null ? failedRecord.get(FAILED_RECORD_ERROR_CODE_KEY) : null);
            counts.merge(errorCode, 1, Integer::sum);
        }
        return counts;
    }
}
