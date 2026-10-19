package com.customcharge.chargescalculator.service.batch;

import com.customcharge.chargescalculator.dto.ChunkProcessingResult;
import com.customcharge.chargescalculator.dto.ProcessedChunkMessage;
import com.customcharge.chargescalculator.recordtracking.RecordTrackingChunkAcceptanceTracker;
import com.customcharge.chargescalculator.service.chunk.ProcessedChunkProcessingService;
import com.customcharge.chargescalculator.support.CalculatorTestDataFactory;
import com.customcharge.persistencelayer.observability.CustomChargeMetricsProperties;
import com.customcharge.persistencelayer.observability.FileProcessingMetrics;
import com.customcharge.persistencelayer.service.ChunkBeginResult;
import com.customcharge.persistencelayer.service.ProcessedChunkService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChunkProcessingCoordinatorTest {

    @Mock
    private ProcessedChunkProcessingService processedChunkProcessingService;
    @Mock
    private ProcessedChunkService processedChunkService;
    @Mock
    private FileChunkCompletionService fileChunkCompletionService;
    @Mock
    private ObjectProvider<RecordTrackingChunkAcceptanceTracker> recordTrackingChunkAcceptanceTracker;
    @Mock
    private ObjectProvider<FileProcessingMetrics> fileProcessingMetrics;

    private final CustomChargeMetricsProperties metricsProperties = new CustomChargeMetricsProperties();
    private ChunkProcessingCoordinator coordinator;

    @BeforeEach
    void setUp() {
        coordinator = new ChunkProcessingCoordinator(
                processedChunkProcessingService,
                processedChunkService,
                fileChunkCompletionService,
                recordTrackingChunkAcceptanceTracker,
                fileProcessingMetrics,
                metricsProperties);
        lenient().when(recordTrackingChunkAcceptanceTracker.getIfAvailable()).thenReturn(null);
        lenient().when(processedChunkProcessingService.processChunk(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new ChunkProcessingResult(0, List.of()));
    }

    @Test
    void parseFileId_shouldParseValidUuid() {
        assertThat(ChunkProcessingCoordinator.parseFileId(CalculatorTestDataFactory.fileId().toString()))
                .isEqualTo(CalculatorTestDataFactory.fileId());
    }

    @ParameterizedTest
    @CsvSource({
            "' ', fileId is required",
            "not-a-uuid, Invalid fileId"
    })
    void parseFileId_shouldRejectInvalidInput(String rawFileId, String expectedMessageFragment) {
        assertThatThrownBy(() -> ChunkProcessingCoordinator.parseFileId(rawFileId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(expectedMessageFragment);
    }

    @Test
    void process_shouldSkip_whenChunkAlreadyCompleted() throws Exception {
        ProcessedChunkMessage message = CalculatorTestDataFactory.chunkMessage();
        when(processedChunkService.tryBeginChunk(CalculatorTestDataFactory.fileId(), 0))
                .thenReturn(ChunkBeginResult.ALREADY_COMPLETED);

        coordinator.process(message);

        verify(processedChunkProcessingService, never()).processChunk(message);
        verify(fileChunkCompletionService).maybeTriggerErrorReport(message, CalculatorTestDataFactory.fileId());
    }

    @Test
    void process_shouldThrow_whenChunkInProgressElsewhere() {
        ProcessedChunkMessage message = CalculatorTestDataFactory.chunkMessage();
        when(processedChunkService.tryBeginChunk(CalculatorTestDataFactory.fileId(), 0))
                .thenReturn(ChunkBeginResult.IN_PROGRESS);

        assertThatThrownBy(() -> coordinator.process(message))
                .isInstanceOf(ChunkInProgressException.class)
                .hasMessageContaining(CalculatorTestDataFactory.fileId().toString());

        verify(processedChunkProcessingService, never()).processChunk(message);
        verify(fileChunkCompletionService, never()).maybeTriggerErrorReport(message, CalculatorTestDataFactory.fileId());
    }

    @Test
    void process_shouldCompleteChunk_whenProcessingSucceeds() throws Exception {
        ProcessedChunkMessage message = CalculatorTestDataFactory.chunkMessage();
        when(processedChunkService.tryBeginChunk(CalculatorTestDataFactory.fileId(), 0))
                .thenReturn(ChunkBeginResult.ACCEPTED);

        coordinator.process(message);

        verify(processedChunkProcessingService).processChunk(message);
        verify(processedChunkService).markChunkCompleted(CalculatorTestDataFactory.fileId(), 0);
        verify(fileChunkCompletionService).maybeTriggerErrorReport(message, CalculatorTestDataFactory.fileId());
    }

    @Test
    void process_shouldEmitChunkMetrics_afterChunkMarkedCompleted() throws Exception {
        ProcessedChunkMessage message = CalculatorTestDataFactory.chunkMessage();
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        FileProcessingMetrics metrics = new FileProcessingMetrics(registry);
        when(processedChunkService.tryBeginChunk(CalculatorTestDataFactory.fileId(), 0))
                .thenReturn(ChunkBeginResult.ACCEPTED);
        when(fileProcessingMetrics.getIfAvailable()).thenReturn(metrics);
        when(processedChunkProcessingService.processChunk(message))
                .thenReturn(new ChunkProcessingResult(
                        2,
                        List.of(Map.of("errorCode", "INVALID_HAWB"))));

        coordinator.process(message);

        assertThat(registry.find("customcharge.file.processing.records.processed")
                .tag("outcome", "success")
                .counter()
                .count()).isEqualTo(1.0);
        assertThat(registry.find("customcharge.file.processing.records.processed")
                .tag("outcome", "failed")
                .counter()
                .count()).isEqualTo(1.0);
    }

    @Test
    void process_shouldReleaseChunk_whenProcessingFails() {
        ProcessedChunkMessage message = CalculatorTestDataFactory.chunkMessage();
        when(processedChunkService.tryBeginChunk(CalculatorTestDataFactory.fileId(), 0))
                .thenReturn(ChunkBeginResult.ACCEPTED);
        doThrow(new RuntimeException("boom")).when(processedChunkProcessingService).processChunk(message);

        assertThatThrownBy(() -> coordinator.process(message))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("boom");

        verify(processedChunkService).releaseChunk(CalculatorTestDataFactory.fileId(), 0);
        verify(processedChunkService, never()).markChunkCompleted(CalculatorTestDataFactory.fileId(), 0);
    }
}
