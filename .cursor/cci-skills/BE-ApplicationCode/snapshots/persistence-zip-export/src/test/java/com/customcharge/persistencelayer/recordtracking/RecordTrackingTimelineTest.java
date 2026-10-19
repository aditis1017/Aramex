package com.customcharge.persistencelayer.recordtracking;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

class RecordTrackingTimelineTest {

    @AfterEach
    void tearDown() {
        RecordTrackingTimeline.clearAll();
    }

    @Test
    void markTraceStart_shouldSetOnce() {
        RecordTrackingTimeline.markTraceStart();
        String first = MDC.get(RecordTrackingTimeline.TRACE_STARTED_AT);
        RecordTrackingTimeline.markTraceStart();
        assertThat(MDC.get(RecordTrackingTimeline.TRACE_STARTED_AT)).isEqualTo(first);
    }

    @Test
    void markChunkStart_shouldSetChunkTimestamp() {
        RecordTrackingTimeline.markChunkStart();
        assertThat(MDC.get(RecordTrackingTimeline.CHUNK_STARTED_AT)).isNotBlank();
    }

    @Test
    void markRecordStart_shouldSetRecordTimestamp() {
        RecordTrackingTimeline.markRecordStart();
        assertThat(MDC.get(RecordTrackingTimeline.RECORD_STARTED_AT)).isNotBlank();
    }

    @Test
    void elapsedSinceTraceMs_shouldReturnValueWhenStarted() {
        RecordTrackingTimeline.markTraceStart();
        assertThat(RecordTrackingTimeline.elapsedSinceTraceMs()).isNotNull().isGreaterThanOrEqualTo(0L);
    }

    @Test
    void elapsedSinceTraceMs_shouldReturnNullWhenMissing() {
        assertThat(RecordTrackingTimeline.elapsedSinceTraceMs()).isNull();
    }

    @Test
    void elapsedSinceChunkMs_shouldReturnNullWhenMissing() {
        assertThat(RecordTrackingTimeline.elapsedSinceChunkMs()).isNull();
    }

    @Test
    void elapsedSinceRecordMs_shouldReturnNullWhenMissing() {
        assertThat(RecordTrackingTimeline.elapsedSinceRecordMs()).isNull();
    }

    @Test
    void durationSince_shouldNeverBeNegative() {
        long future = System.currentTimeMillis() + 1000L;
        assertThat(RecordTrackingTimeline.durationSince(future)).isZero();
    }

    @Test
    void clearScopes_shouldRemoveScopedFields() {
        RecordTrackingTimeline.markRecordStart();
        RecordTrackingTimeline.markChunkStart();

        RecordTrackingTimeline.clearRecordScope();
        RecordTrackingTimeline.clearChunkScope();

        assertThat(MDC.get(RecordTrackingTimeline.RECORD_STARTED_AT)).isNull();
        assertThat(MDC.get(RecordTrackingTimeline.CHUNK_STARTED_AT)).isNull();
    }
}
