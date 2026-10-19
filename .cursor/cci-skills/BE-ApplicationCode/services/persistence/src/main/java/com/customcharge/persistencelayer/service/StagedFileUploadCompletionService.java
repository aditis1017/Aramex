package com.customcharge.persistencelayer.service;

import com.customcharge.persistencelayer.repository.StagedFileUploadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class StagedFileUploadCompletionService {

    private final StagedFileUploadRepository stagedFileUploadRepository;

    public StagedFileUploadCompletionService(StagedFileUploadRepository stagedFileUploadRepository) {
        this.stagedFileUploadRepository = stagedFileUploadRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryMarkFileProcessingComplete(UUID fileId) {
        int updated = stagedFileUploadRepository.markFileProcessingCompleteIfAbsent(
                fileId.toString(), Instant.now());
        return updated > 0;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveExpectedDataChunksIfAbsent(UUID fileId, int expectedDataChunks) {
        stagedFileUploadRepository.saveExpectedDataChunksIfAbsent(fileId.toString(), expectedDataChunks);
    }

    public Integer getExpectedDataChunks(UUID fileId) {
        return stagedFileUploadRepository.findByFileId(fileId.toString())
                .map(upload -> upload.getExpectedDataChunks())
                .orElse(null);
    }
}
