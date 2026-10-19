package com.customcharge.fileorchestration.recordtracking;

import com.customcharge.fileorchestration.pipeline.FileProcessingRequest;
import com.customcharge.persistencelayer.observability.CustomChargeMetricsProperties;
import com.customcharge.persistencelayer.observability.FileProcessingMetrics;
import com.customcharge.persistencelayer.recordtracking.RecordContext;
import com.customcharge.persistencelayer.recordtracking.RecordTrackingEvent;
import com.customcharge.persistencelayer.recordtracking.RecordTrackingLogger;
import com.customcharge.persistencelayer.recordtracking.RecordTrackingMdc;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileOrchestrationPipelineTrackerTest {

    @Mock
    private RecordTrackingLogger recordTrackingLogger;

    @Mock
    private ObjectProvider<com.customcharge.persistencelayer.observability.FileProcessingMetrics> fileProcessingMetrics;

    private final CustomChargeMetricsProperties metricsProperties = new CustomChargeMetricsProperties();

    private FileOrchestrationPipelineTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new FileOrchestrationPipelineTracker(
                recordTrackingLogger, fileProcessingMetrics, metricsProperties, "fileOrchestration");
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void onFileReceived_shouldPopulateMdcAndLogEvent() {
        FileProcessingRequest request = request();

        tracker.onFileReceived(request);

        assertThat(MDC.get(RecordTrackingMdc.TRACE_ID)).isEqualTo("trace-1");
        assertThat(MDC.get(RecordTrackingMdc.FILE_ID)).isEqualTo("file-1");
        assertThat(MDC.get(RecordTrackingMdc.UPLOADED_BY)).isEqualTo("uploader@aramex.com");
        assertThat(MDC.get(RecordTrackingMdc.ENTITY_CODE)).isEqualTo("RUH");
        verify(recordTrackingLogger).logEvent(
                eq(RecordTrackingEvent.FILE_RECEIVED),
                any(RecordContext.class));
    }

    @Test
    void onOrchestrationFinished_shouldRecordSuccessMetricsAndClearMdc() {
        FileProcessingRequest request = request();
        MDC.put(RecordTrackingMdc.FILE_ID, "stale");
        FileProcessingMetrics metrics = org.mockito.Mockito.mock(FileProcessingMetrics.class);
        when(fileProcessingMetrics.getIfAvailable()).thenReturn(metrics);

        tracker.onOrchestrationFinished(request, 1200L, 100, 3, 5);

        verify(metrics).recordFileOrchestrationDuration(
                eq(1_200_000_000L),
                eq("CSV"),
                eq("Estimates_Template"),
                eq(FileProcessingMetrics.outcome(true)));
        verify(metrics).recordOrchestrationRowsTotal(100, "CSV");
        verify(metrics).recordOrchestrationRowsDeduped(3, "CSV");
        verify(metrics).recordFileOrchestrationChunksPublished(5, "CSV", "Estimates_Template");
        org.mockito.Mockito.verify(recordTrackingLogger, org.mockito.Mockito.never())
                .logEvent(eq(RecordTrackingEvent.FILE_BATCH_COMPLETED), any(RecordContext.class), any(Long.class));
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    @Test
    void onFileFailed_shouldLogFailureWithMessageAndClearMdc() {
        FileProcessingRequest request = request();

        tracker.onFileFailed(request, new IllegalStateException("dedup failed"), 900L);

        ArgumentCaptor<Throwable> failureCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(recordTrackingLogger).logFailure(
                eq(RecordTrackingEvent.FILE_BATCH_FAILED),
                any(RecordContext.class),
                failureCaptor.capture(),
                eq(900L));
        assertThat(failureCaptor.getValue()).hasMessageContaining("dedup failed");
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    @Test
    void onFileFailed_shouldUseDefaultMessage_whenFailureMessageBlank() {
        tracker.onFileFailed(request(), new RuntimeException("   "), 100L);

        ArgumentCaptor<Throwable> failureCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(recordTrackingLogger).logFailure(
                eq(RecordTrackingEvent.FILE_BATCH_FAILED),
                any(RecordContext.class),
                failureCaptor.capture(),
                eq(100L));
        assertThat(failureCaptor.getValue()).hasMessage("File orchestration failed");
    }

    private static FileProcessingRequest request() {
        return new FileProcessingRequest(
                "file-1",
                "uploader@aramex.com",
                "s3://bucket/file.csv",
                "file.csv",
                "CSV",
                "Estimates_Template",
                "RUH",
                "trace-1");
    }
}
