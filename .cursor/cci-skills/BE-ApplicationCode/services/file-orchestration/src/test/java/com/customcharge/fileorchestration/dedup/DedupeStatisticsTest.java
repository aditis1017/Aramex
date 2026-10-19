package com.customcharge.fileorchestration.dedup;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DedupeStatisticsTest {

    @Test
    void record_shouldStoreCounts_andMarkPopulated() {
        DedupeStatistics statistics = new DedupeStatistics();

        statistics.record(10, 7, null);

        assertThat(statistics.isPopulated()).isTrue();
        assertThat(statistics.getRawRowCount()).isEqualTo(10);
        assertThat(statistics.getDedupedRowCount()).isEqualTo(7);
        assertThat(statistics.getDroppedCount()).isEqualTo(3);
    }

    @Test
    void record_shouldNotProduceNegativeDroppedCount() {
        DedupeStatistics statistics = new DedupeStatistics();

        statistics.record(3, 5, null);

        assertThat(statistics.getDroppedCount()).isZero();
    }
}
