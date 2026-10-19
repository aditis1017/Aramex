package com.customcharge.chargescalculator.service.batch;

import com.customcharge.chargescalculator.dto.ProcessedChunkMessage;
import com.customcharge.chargescalculator.recordtracking.FileProcessingCompletionTracker;
import com.customcharge.chargescalculator.service.errorreport.ErrorReportOrchestratorService;
import com.customcharge.persistencelayer.service.ProcessedChunkService;
import com.customcharge.persistencelayer.service.StagedFileUploadCompletionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class FileChunkCompletionService {

    private static final Logger log = LoggerFactory.getLogger(FileChunkCompletionService.class);
    private final StagedFileUploadCompletionService stagedFileUploadCompletionService;
    private final ProcessedChunkService processedChunkService;
    private final ErrorReportOrchestratorService errorReportOrchestratorService;
    private final ObjectProvider<FileProcessingCompletionTracker> fileProcessingCompletionTracker;

    public FileChunkCompletionService(
            StagedFileUploadCompletionService stagedFileUploadCompletionService,
            ProcessedChunkService processedChunkService,
            ErrorReportOrchestratorService errorReportOrchestratorService,
            ObjectProvider<FileProcessingCompletionTracker> fileProcessingCompletionTracker) {
        this.stagedFileUploadCompletionService = stagedFileUploadCompletionService;
        this.processedChunkService = processedChunkService;
        this.errorReportOrchestratorService = errorReportOrchestratorService;
        this.fileProcessingCompletionTracker = fileProcessingCompletionTracker;
    }

    public void maybeTriggerErrorReport(ProcessedChunkMessage message, UUID fileId) {
        if (Boolean.TRUE.equals(message.getLastChunk())) {
            int expectedDataChunks = message.getTotalChunks() != null ? message.getTotalChunks() : 0;
            stagedFileUploadCompletionService.saveExpectedDataChunksIfAbsent(fileId, expectedDataChunks);
        }

        Integer expectedDataChunks = stagedFileUploadCompletionService.getExpectedDataChunks(fileId);
        if (expectedDataChunks == null) {
            return;
        }
        if (!processedChunkService.isFileFullyProcessed(fileId, expectedDataChunks)) {
            log.debug("Waiting for remaining chunks fileId={} expectedDataChunks={}", fileId, expectedDataChunks);
            return;
        }
        FileProcessingCompletionTracker completionTracker = fileProcessingCompletionTracker.getIfAvailable();
        if (completionTracker != null) {
            completionTracker.onFileFullyProcessed(message);
        }
        if (!stagedFileUploadCompletionService.tryMarkFileProcessingComplete(fileId)) {
            log.info(
                    "File completion check fileId={} status=ALL_CHUNKS_COMPLETE expectedDataChunks={} finalStatus=ALREADY_SET",
                    fileId,
                    expectedDataChunks);
            return;
        }
        log.info(
                "File completion check fileId={} status=ALL_CHUNKS_COMPLETE expectedDataChunks={} finalStatus=UPDATED notification=TRIGGERING",
                fileId,
                expectedDataChunks);
        errorReportOrchestratorService.generateAndSendReportAsync(fileId);
    }
}
