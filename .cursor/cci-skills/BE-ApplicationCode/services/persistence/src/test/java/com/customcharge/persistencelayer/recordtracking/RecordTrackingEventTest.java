package com.customcharge.persistencelayer.recordtracking;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RecordTrackingEventTest {

    @Test
    void oncePerChunk_shouldIdentifyChunkEvents() {
        assertThat(RecordTrackingEvent.CHUNK_PROCESSING_STARTED.oncePerChunk()).isTrue();
        assertThat(RecordTrackingEvent.RECORD_COMPLETED.oncePerChunk()).isFalse();
    }

    @Test
    void oncePerRecord_shouldIdentifyRecordEvents() {
        assertThat(RecordTrackingEvent.RECORD_COMPLETED.oncePerRecord()).isTrue();
        assertThat(RecordTrackingEvent.RECORD_PROCESSING_STARTED.oncePerRecord()).isFalse();
        assertThat(RecordTrackingEvent.CHUNK_CREATED.oncePerRecord()).isFalse();
    }

    @Test
    void oncePerFile_shouldIdentifyFileCompletionEvents() {
        assertThat(RecordTrackingEvent.FILE_BATCH_COMPLETED.oncePerFile()).isTrue();
        assertThat(RecordTrackingEvent.FILE_BATCH_FAILED.oncePerFile()).isTrue();
        assertThat(RecordTrackingEvent.FILE_RECEIVED.oncePerFile()).isFalse();
    }
}
