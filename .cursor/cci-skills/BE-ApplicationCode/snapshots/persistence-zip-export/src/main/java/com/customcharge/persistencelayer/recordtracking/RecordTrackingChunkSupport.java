package com.customcharge.persistencelayer.recordtracking;

import java.util.List;

/**
 * Decides whether an SQS chunk message should emit chunk-level record-tracking events.
 * File-completion envelopes ({@code lastChunk=true}, empty records) are excluded.
 */
public final class RecordTrackingChunkSupport {

    public static final int FILE_COMPLETION_CHUNK_INDEX = 0;

    private RecordTrackingChunkSupport() {
    }

    /**
     * @return {@code true} only for real data chunks that should produce CHUNK_* tracking events
     */
    public static boolean isTrackableDataChunk(Boolean lastChunk, int chunkIndex, int recordCount, List<?> records) {
        if (Boolean.TRUE.equals(lastChunk)) {
            return false;
        }
        if (recordCount <= 0 || records == null || records.isEmpty()) {
            return false;
        }
        return chunkIndex > FILE_COMPLETION_CHUNK_INDEX;
    }
}
