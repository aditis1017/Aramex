package com.customcharge.persistencelayer.recordtracking;

import org.slf4j.MDC;

/**
 * Timeline helpers for record-tracking elapsed-time fields.
 */
public final class RecordTrackingTimeline {

    public static final String TRACE_STARTED_AT = "traceStartedAt";
    public static final String CHUNK_STARTED_AT = "chunkStartedAt";
    public static final String RECORD_STARTED_AT = "recordStartedAt";

    private RecordTrackingTimeline() {
    }

    public static void markTraceStart() {
        if (MDC.get(TRACE_STARTED_AT) == null) {
            MDC.put(TRACE_STARTED_AT, String.valueOf(System.currentTimeMillis()));
        }
    }

    public static void markTraceStartFromEpoch(long startedAtEpochMs) {
        MDC.put(TRACE_STARTED_AT, String.valueOf(startedAtEpochMs));
    }

    public static void markChunkStart() {
        MDC.put(CHUNK_STARTED_AT, String.valueOf(System.currentTimeMillis()));
    }

    public static void markRecordStart() {
        MDC.put(RECORD_STARTED_AT, String.valueOf(System.currentTimeMillis()));
    }

    public static Long elapsedSinceTraceMs() {
        return elapsedSince(MDC.get(TRACE_STARTED_AT));
    }

    public static Long elapsedSinceChunkMs() {
        return elapsedSince(MDC.get(CHUNK_STARTED_AT));
    }

    public static Long elapsedSinceRecordMs() {
        return elapsedSince(MDC.get(RECORD_STARTED_AT));
    }

    public static long durationSince(long startedAtMs) {
        return Math.max(0L, System.currentTimeMillis() - startedAtMs);
    }

    public static void clearRecordScope() {
        MDC.remove(RECORD_STARTED_AT);
    }

    public static void clearChunkScope() {
        MDC.remove(CHUNK_STARTED_AT);
    }

    public static void clearAll() {
        MDC.remove(TRACE_STARTED_AT);
        MDC.remove(CHUNK_STARTED_AT);
        MDC.remove(RECORD_STARTED_AT);
    }

    private static Long elapsedSince(String startedAt) {
        if (startedAt == null || startedAt.isBlank()) {
            return null;
        }
        try {
            return Math.max(0L, System.currentTimeMillis() - Long.parseLong(startedAt));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
