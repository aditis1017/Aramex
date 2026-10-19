package com.customcharge.chargescalculator.recordtracking;

import com.customcharge.chargescalculator.dto.ProcessedChunkMessage;
import com.customcharge.chargescalculator.support.CalculatorTestDataFactory;
import com.customcharge.persistencelayer.observability.CustomChargeMetricsProperties;
import com.customcharge.persistencelayer.recordtracking.RecordContext;
import com.customcharge.persistencelayer.recordtracking.RecordTrackingEvent;
import com.customcharge.persistencelayer.recordtracking.RecordTrackingLogger;
import com.customcharge.persistencelayer.recordtracking.RecordTrackingMdc;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FileProcessingCompletionTrackerTest {

    @Mock
    private RecordTrackingLogger recordTrackingLogger;

    @Mock
    private ObjectProvider<com.customcharge.persistencelayer.observability.FileProcessingMetrics> fileProcessingMetrics;

    private final CustomChargeMetricsProperties metricsProperties = new CustomChargeMetricsProperties();

    private FileProcessingCompletionTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new FileProcessingCompletionTracker(
                recordTrackingLogger, fileProcessingMetrics, metricsProperties, "chargesCalculator");
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void onFileFullyProcessed_shouldLogBatchCompletedWithDuration() {
        ProcessedChunkMessage message = CalculatorTestDataFactory.chunkMessage();
        message.setFileName("upload.csv");
        message.setFileType("CSV");
        message.setProcessFor("Estimates_Template");
        message.setUploadedBy("user@test.com");
        message.setEntityCode("RUH");
        long startedAt = System.currentTimeMillis() - 5_000L;
        message.setFileProcessingStartedAtEpochMs(startedAt);

        tracker.onFileFullyProcessed(message);

        verify(recordTrackingLogger).logEvent(
                eq(RecordTrackingEvent.FILE_BATCH_COMPLETED),
                any(RecordContext.class),
                org.mockito.ArgumentMatchers.longThat(duration -> duration >= 4_000L && duration <= 10_000L));
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }
}
