package com.customcharge.chargescalculator.service.batch;

import com.customcharge.chargescalculator.dto.ProcessedChunkMessage;
import com.customcharge.chargescalculator.recordtracking.FileProcessingCompletionTracker;
import com.customcharge.chargescalculator.service.errorreport.ErrorReportOrchestratorService;
import com.customcharge.chargescalculator.support.CalculatorTestDataFactory;
import com.customcharge.persistencelayer.service.ProcessedChunkService;
import com.customcharge.persistencelayer.service.StagedFileUploadCompletionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileChunkCompletionServiceTest {

    @Mock
    private StagedFileUploadCompletionService stagedFileUploadCompletionService;
    @Mock
    private ProcessedChunkService processedChunkService;
    @Mock
    private ErrorReportOrchestratorService errorReportOrchestratorService;
    @Mock
    private FileProcessingCompletionTracker fileProcessingCompletionTracker;
    @Mock
    private ObjectProvider<FileProcessingCompletionTracker> fileProcessingCompletionTrackerProvider;

    @InjectMocks
    private FileChunkCompletionService service;

    @BeforeEach
    void setUpCompletionTracker() {
        lenient().when(fileProcessingCompletionTrackerProvider.getIfAvailable())
                .thenReturn(fileProcessingCompletionTracker);
    }

    @Test
    void maybeTriggerErrorReport_shouldNoOp_whenCompletionEnvelopeNotSeenYet() {
        ProcessedChunkMessage message = CalculatorTestDataFactory.chunkMessage();
        message.setLastChunk(false);
        when(stagedFileUploadCompletionService.getExpectedDataChunks(CalculatorTestDataFactory.fileId()))
                .thenReturn(null);

        service.maybeTriggerErrorReport(message, CalculatorTestDataFactory.fileId());

        verify(errorReportOrchestratorService, never()).generateAndSendReportAsync(CalculatorTestDataFactory.fileId());
    }

    @Test
    void maybeTriggerErrorReport_shouldNoOp_whenDataChunksStillInFlight() {
        ProcessedChunkMessage message = completionMessage();
        when(stagedFileUploadCompletionService.getExpectedDataChunks(CalculatorTestDataFactory.fileId()))
                .thenReturn(4);
        when(processedChunkService.isFileFullyProcessed(CalculatorTestDataFactory.fileId(), 4))
                .thenReturn(false);

        service.maybeTriggerErrorReport(message, CalculatorTestDataFactory.fileId());

        verify(stagedFileUploadCompletionService).saveExpectedDataChunksIfAbsent(CalculatorTestDataFactory.fileId(), 4);
        verify(errorReportOrchestratorService, never()).generateAndSendReportAsync(CalculatorTestDataFactory.fileId());
    }

    @Test
    void maybeTriggerErrorReport_shouldNoOp_whenAlreadyClaimed() {
        ProcessedChunkMessage message = completionMessage();
        when(stagedFileUploadCompletionService.getExpectedDataChunks(CalculatorTestDataFactory.fileId()))
                .thenReturn(4);
        when(processedChunkService.isFileFullyProcessed(CalculatorTestDataFactory.fileId(), 4))
                .thenReturn(true);
        when(stagedFileUploadCompletionService.tryMarkFileProcessingComplete(CalculatorTestDataFactory.fileId()))
                .thenReturn(false);

        service.maybeTriggerErrorReport(message, CalculatorTestDataFactory.fileId());

        verify(errorReportOrchestratorService, never()).generateAndSendReportAsync(CalculatorTestDataFactory.fileId());
    }

    @Test
    void maybeTriggerErrorReport_shouldTriggerReport_whenAllChunksComplete() {
        ProcessedChunkMessage message = completionMessage();
        when(stagedFileUploadCompletionService.getExpectedDataChunks(CalculatorTestDataFactory.fileId()))
                .thenReturn(4);
        when(processedChunkService.isFileFullyProcessed(CalculatorTestDataFactory.fileId(), 4))
                .thenReturn(true);
        when(stagedFileUploadCompletionService.tryMarkFileProcessingComplete(CalculatorTestDataFactory.fileId()))
                .thenReturn(true);

        service.maybeTriggerErrorReport(message, CalculatorTestDataFactory.fileId());

        verify(stagedFileUploadCompletionService).saveExpectedDataChunksIfAbsent(CalculatorTestDataFactory.fileId(), 4);
        verify(fileProcessingCompletionTracker).onFileFullyProcessed(message);
        verify(errorReportOrchestratorService).generateAndSendReportAsync(CalculatorTestDataFactory.fileId());
    }

    @Test
    void maybeTriggerErrorReport_shouldTriggerReport_whenLastDataChunkCompletesAfterEnvelope() {
        ProcessedChunkMessage message = CalculatorTestDataFactory.chunkMessage();
        message.setLastChunk(false);
        when(stagedFileUploadCompletionService.getExpectedDataChunks(CalculatorTestDataFactory.fileId()))
                .thenReturn(1);
        when(processedChunkService.isFileFullyProcessed(CalculatorTestDataFactory.fileId(), 1))
                .thenReturn(true);
        when(stagedFileUploadCompletionService.tryMarkFileProcessingComplete(CalculatorTestDataFactory.fileId()))
                .thenReturn(true);

        service.maybeTriggerErrorReport(message, CalculatorTestDataFactory.fileId());

        verify(stagedFileUploadCompletionService, never()).saveExpectedDataChunksIfAbsent(
                CalculatorTestDataFactory.fileId(), 1);
        verify(fileProcessingCompletionTracker).onFileFullyProcessed(message);
        verify(errorReportOrchestratorService).generateAndSendReportAsync(CalculatorTestDataFactory.fileId());
    }

    private static ProcessedChunkMessage completionMessage() {
        ProcessedChunkMessage message = CalculatorTestDataFactory.chunkMessage();
        message.setLastChunk(true);
        message.setTotalChunks(4);
        message.setRecords(java.util.List.of());
        return message;
    }

}
