package com.customcharge.persistencelayer.recordtracking;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RecordTrackingEventDeduperTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void tryAcquire_shouldAllowNonDedupedEvents() {
        assertThat(RecordTrackingEventDeduper.tryAcquire(RecordTrackingEvent.FILE_RECEIVED)).isTrue();
        assertThat(RecordTrackingEventDeduper.tryAcquire(RecordTrackingEvent.FILE_RECEIVED)).isTrue();
    }

    @Test
    void tryAcquire_shouldDedupeChunkEvents() {
        String suffix = UUID.randomUUID().toString();
        MDC.put(RecordTrackingMdc.TRACE_ID, "trace-" + suffix);
        MDC.put(RecordTrackingMdc.FILE_ID, "file-" + suffix);
        MDC.put(RecordTrackingMdc.CHUNK_ID, "chunk-" + suffix);

        assertThat(RecordTrackingEventDeduper.tryAcquire(RecordTrackingEvent.CHUNK_CREATED)).isTrue();
        assertThat(RecordTrackingEventDeduper.tryAcquire(RecordTrackingEvent.CHUNK_CREATED)).isFalse();
    }

    @Test
    void tryAcquire_shouldDedupeRecordEvents() {
        String suffix = UUID.randomUUID().toString();
        MDC.put(RecordTrackingMdc.TRACE_ID, "trace-" + suffix);
        MDC.put(RecordTrackingMdc.FILE_ID, "file-" + suffix);
        MDC.put(RecordTrackingMdc.CHUNK_ID, "chunk-" + suffix);
        MDC.put(RecordTrackingMdc.RECORD_ID, "record-" + suffix);

        assertThat(RecordTrackingEventDeduper.tryAcquire(RecordTrackingEvent.RECORD_COMPLETED)).isTrue();
        assertThat(RecordTrackingEventDeduper.tryAcquire(RecordTrackingEvent.RECORD_COMPLETED)).isFalse();
    }

    @Test
    void tryAcquire_shouldDedupeFileEvents() {
        String suffix = UUID.randomUUID().toString();
        MDC.put(RecordTrackingMdc.TRACE_ID, "trace-" + suffix);
        MDC.put(RecordTrackingMdc.FILE_ID, "file-" + suffix);

        assertThat(RecordTrackingEventDeduper.tryAcquire(RecordTrackingEvent.FILE_BATCH_COMPLETED)).isTrue();
        assertThat(RecordTrackingEventDeduper.tryAcquire(RecordTrackingEvent.FILE_BATCH_COMPLETED)).isFalse();
    }

    @Test
    void tryAcquire_shouldAllowWhenMdcMissing() {
        assertThat(RecordTrackingEventDeduper.tryAcquire(RecordTrackingEvent.CHUNK_CREATED)).isTrue();
        assertThat(RecordTrackingEventDeduper.tryAcquire(RecordTrackingEvent.RECORD_COMPLETED)).isTrue();
    }
}
