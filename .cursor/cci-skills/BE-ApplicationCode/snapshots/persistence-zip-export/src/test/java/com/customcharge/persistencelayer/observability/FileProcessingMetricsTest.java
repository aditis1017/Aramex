package com.customcharge.persistencelayer.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FileProcessingMetricsTest {

    private SimpleMeterRegistry registry;
    private FileProcessingMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new FileProcessingMetrics(registry);
    }

    @Test
    void normalize_shouldDefaultUnknownValues() {
        assertThat(FileProcessingMetrics.normalize(null)).isEqualTo("unknown");
        assertThat(FileProcessingMetrics.normalize("  dtc  ")).isEqualTo("dtc");
    }

    @Test
    void outcomeAndErrorCode_shouldMapValues() {
        assertThat(FileProcessingMetrics.outcome(true)).isEqualTo("success");
        assertThat(FileProcessingMetrics.outcome(false)).isEqualTo("failed");
        assertThat(FileProcessingMetrics.errorCode(" ERR ")).isEqualTo("ERR");
    }

    @Test
    void recordMethods_shouldRegisterMeters() {
        metrics.recordFileReceived("csv", "template", "AE", "user@example.com");
        metrics.recordFileOrchestrationDuration(1_000_000L, "csv", "template", "success");
        metrics.recordFileOrchestrationFailed("csv", "template");
        metrics.recordOrchestrationRowsTotal(10, "csv");
        metrics.recordOrchestrationRowsDeduped(0, "csv");
        metrics.recordOrchestrationRowsDeduped(3, "csv");
        metrics.recordFileOrchestrationChunksPublished("csv", "template");
        metrics.recordFileCalculatorCompleted("template", "success");
        metrics.recordFileCalculatorEndToEndDuration(2_000_000L, "template", "csv");
        metrics.recordChunkPublished("template", "csv");
        metrics.recordChunkReceived("template");
        metrics.recordChunkFailed("template");
        metrics.recordChunkSkippedDuplicate("duplicate");
        metrics.recordChunkRecordsCount(5, "template");
        metrics.recordChunkFailedRecordsCount(1, "template");
        metrics.recordRecordProcessed("template", "success", "AE");
        metrics.recordRecordOutcomes(2, 1, "template", "AE");
        metrics.recordRecordFailures(1, "template", "VALIDATION_FAILED", "AE");
        metrics.compensateRecordFailed("template", "ERR", "AE");
        metrics.compensateRecordProcessed("template", "failed", "AE");
        metrics.recordRecordFailed("template", "VALIDATION_FAILED", "AE");
        metrics.recordRecordFailedUnexpected("template");
        metrics.recordSqsPublishFailed("queue");
        metrics.recordSqsPoisonMessage("queue");

        var sample = metrics.startTimer();
        metrics.recordChunkPublishDuration(sample, "success");
        sample = metrics.startTimer();
        metrics.recordChunkProcessingDuration(sample, "template", "success");
        sample = metrics.startTimer();
        metrics.recordRecordProcessingDuration(sample, "template", "AE");
        metrics.recordRecordProcessingDurationNanos(5_000_000L, "Actual_Template", "AE");
        metrics.recordRecordPhaseDuration(12L, "calculation", "Actual_Template");
        metrics.recordChunkAverageRecordProcessingDuration(20_000_000L, 4, "template", "AE");
        sample = metrics.startTimer();
        metrics.recordSqsListenerDuration(sample, "success");

        assertThat(registry.find("customcharge.file.processing.files.received").counter().count()).isEqualTo(1.0);
        assertThat(registry.find("customcharge.file.processing.files.orchestration.rows.deduped").counter().count())
                .isEqualTo(3.0);
        assertThat(registry.find("customcharge.file.processing.chunks.publish.duration").timer().count()).isEqualTo(1);
        assertThat(registry.find("customcharge.file.processing.records.processing.duration")
                .tag("process_for", "Actual_Template")
                .timer()
                .count()).isEqualTo(1);
        assertThat(registry.find("customcharge.file.processing.records.processing.duration")
                .tag("process_for", "template")
                .timer()
                .count()).isEqualTo(1);
        assertThat(registry.find("customcharge.file.processing.records.processing.phase.duration")
                .tag("phase", "calculation")
                .timer()
                .count()).isEqualTo(1);
        assertThat(registry.find("customcharge.file.processing.chunks.processing.wall_per_record.duration")
                .timer()
                .count()).isEqualTo(1);
    }
}
