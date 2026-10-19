package com.customcharge.persistencelayer.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ChunkRecordMetricsReporterTest {

    private SimpleMeterRegistry registry;
    private FileProcessingMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new FileProcessingMetrics(registry);
    }

    @Test
    void recordChunkOutcomes_shouldIncrementBatchCounters() {
        List<Map<String, String>> failed = List.of(
                Map.of("errorCode", "INVALID_HAWB"),
                Map.of("errorCode", "INVALID_HAWB"),
                Map.of("errorCode", "VALIDATION_FAILED"));

        ChunkRecordMetricsReporter.recordChunkOutcomes(metrics, "dtc", "AE", 10, failed, 50_000_000L);

        assertThat(processedSuccessCount()).isEqualTo(7.0);
        assertThat(processedFailedCount()).isEqualTo(3.0);
        assertThat(failedCount("INVALID_HAWB")).isEqualTo(2.0);
        assertThat(failedCount("VALIDATION_FAILED")).isEqualTo(1.0);
    }

    @Test
    void recordChunkOutcomes_shouldIgnoreEmptyChunk() {
        ChunkRecordMetricsReporter.recordChunkOutcomes(metrics, "dtc", "AE", 0, List.of(), 0L);

        assertThat(registry.getMeters()).isEmpty();
    }

    private double processedSuccessCount() {
        return registry.find("customcharge.file.processing.records.processed")
                .tag("outcome", "success")
                .counters().stream()
                .mapToDouble(counter -> counter.count())
                .sum();
    }

    private double processedFailedCount() {
        return registry.find("customcharge.file.processing.records.processed")
                .tag("outcome", "failed")
                .counters().stream()
                .mapToDouble(counter -> counter.count())
                .sum();
    }

    private double failedCount(String errorCode) {
        return registry.find("customcharge.file.processing.records.failed")
                .tag("error_code", errorCode)
                .counters().stream()
                .mapToDouble(counter -> counter.count())
                .sum();
    }
}
