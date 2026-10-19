package com.customcharge.chargescalculator.service.batch;

import com.customcharge.chargescalculator.dto.ProcessedChunkMessage;
import com.customcharge.persistencelayer.entities.CustomChargeFileError;
import com.customcharge.persistencelayer.repository.CustomChargeFileErrorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Persists failed chunk rows; callers typically invoke this inside a dedicated transaction after row processing.
 */
@Service
public class ChunkErrorPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(ChunkErrorPersistenceService.class);

    private final CustomChargeFileErrorRepository customChargeFileErrorRepository;

    public ChunkErrorPersistenceService(CustomChargeFileErrorRepository customChargeFileErrorRepository) {
        this.customChargeFileErrorRepository = customChargeFileErrorRepository;
    }

    public void persistErrors(ProcessedChunkMessage message, List<Map<String, String>> failedRecords) {
        if (message == null || failedRecords == null || failedRecords.isEmpty()) {
            return;
        }
        List<Map<String, String>> errors = new ArrayList<>(failedRecords.size());
        for (Map<String, String> row : failedRecords) {
            if (row != null) {
                errors.add(new HashMap<>(row));
            }
        }
        if (errors.isEmpty()) {
            return;
        }

        CustomChargeFileError fileError = new CustomChargeFileError();
        fileError.setFileId(UUID.fromString(message.getFileId()));
        fileError.setFileName(message.getFileName());
        fileError.setFileType(message.getFileType());
        fileError.setChunkNumber(message.getChunkIndex());
        fileError.setErrorList(errors);
        customChargeFileErrorRepository.save(fileError);
        log.info("Saved chunk error rows fileId={} chunkNumber={} errorRows={}",
                message.getFileId(), message.getChunkIndex(), errors.size());
    }
}
