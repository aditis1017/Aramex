package com.customcharge.persistencelayer.recordtracking;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecordTrackingChunkSupportTest {

    @Test
    void isTrackableDataChunk_shouldRejectCompletionEnvelope() {
        assertThat(RecordTrackingChunkSupport.isTrackableDataChunk(true, 1, 5, List.of("r"))).isFalse();
    }

    @Test
    void isTrackableDataChunk_shouldRejectEmptyRecords() {
        assertThat(RecordTrackingChunkSupport.isTrackableDataChunk(false, 1, 0, List.of())).isFalse();
        assertThat(RecordTrackingChunkSupport.isTrackableDataChunk(false, 1, 5, null)).isFalse();
    }

    @Test
    void isTrackableDataChunk_shouldRejectZeroChunkIndex() {
        assertThat(RecordTrackingChunkSupport.isTrackableDataChunk(false, 0, 5, List.of("r"))).isFalse();
    }

    @Test
    void isTrackableDataChunk_shouldAcceptValidDataChunk() {
        assertThat(RecordTrackingChunkSupport.isTrackableDataChunk(false, 2, 5, List.of("r1", "r2"))).isTrue();
    }
}
