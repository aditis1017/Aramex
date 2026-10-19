package com.customcharge.persistencelayer.service;

import com.customcharge.persistencelayer.entities.ProcessedChunk;
import com.customcharge.persistencelayer.repository.ProcessedChunkRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProcessedChunkService {

    static final Duration STALE_INCOMPLETE_CLAIM = Duration.ofMinutes(15);

    private static final int MAX_INSERT_ATTEMPTS = 3;

    private final ProcessedChunkRepository processedChunkRepository;

    public ProcessedChunkService(ProcessedChunkRepository processedChunkRepository) {
        this.processedChunkRepository = processedChunkRepository;
    }

    /**
     * Attempts to claim a chunk for processing.
     *
     * @return {@link ChunkBeginResult#ACCEPTED} when this invocation should process the chunk;
     *         {@link ChunkBeginResult#ALREADY_COMPLETED} when the chunk is done (safe to ACK);
     *         {@link ChunkBeginResult#IN_PROGRESS} when another worker holds an active claim (must not ACK).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ChunkBeginResult tryBeginChunk(UUID fileId, int chunkIndex) {
        for (int attempt = 0; attempt < MAX_INSERT_ATTEMPTS; attempt++) {
            Optional<ProcessedChunk> existing = processedChunkRepository.findByFileIdAndChunkIndex(fileId, chunkIndex);
            if (existing.isPresent()) {
                ChunkBeginResult existingResult = resolveExistingClaim(existing.get());
                if (existingResult != ChunkBeginResult.ACCEPTED) {
                    return existingResult;
                }
            }
            try {
                ProcessedChunk row = new ProcessedChunk();
                row.setFileId(fileId);
                row.setChunkIndex(chunkIndex);
                processedChunkRepository.saveAndFlush(row);
                return ChunkBeginResult.ACCEPTED;
            } catch (DataIntegrityViolationException ex) {
                // concurrent insert; retry loop
            }
        }
        return processedChunkRepository.findByFileIdAndChunkIndex(fileId, chunkIndex)
                .map(this::resolveExistingClaim)
                .orElse(ChunkBeginResult.IN_PROGRESS);
    }

    private ChunkBeginResult resolveExistingClaim(ProcessedChunk row) {
        if (row.getCompletedAt() != null) {
            return ChunkBeginResult.ALREADY_COMPLETED;
        }
        Instant created = row.getCreatedTime();
        if (created != null && created.isBefore(Instant.now().minus(STALE_INCOMPLETE_CLAIM))) {
            processedChunkRepository.delete(row);
            processedChunkRepository.flush();
            return ChunkBeginResult.ACCEPTED;
        }
        return ChunkBeginResult.IN_PROGRESS;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markChunkCompleted(UUID fileId, int chunkIndex) {
        processedChunkRepository.findByFileIdAndChunkIndex(fileId, chunkIndex).ifPresent(row -> {
            row.setCompletedAt(Instant.now());
            processedChunkRepository.save(row);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void releaseChunk(UUID fileId, int chunkIndex) {
        processedChunkRepository.deleteByFileIdAndChunkIndex(fileId, chunkIndex);
    }

    public long countCompletedChunksByFileId(UUID fileId) {
        return processedChunkRepository.countByFileIdAndCompletedAtIsNotNull(fileId);
    }

    public static final int FILE_COMPLETION_CHUNK_INDEX = 0;

    public boolean isFileFullyProcessed(UUID fileId, int expectedDataChunks) {
        if (!processedChunkRepository.existsByFileIdAndChunkIndexAndCompletedAtIsNotNull(
                fileId, FILE_COMPLETION_CHUNK_INDEX)) {
            return false;
        }
        long completedDataChunks = processedChunkRepository
                .countByFileIdAndChunkIndexGreaterThanAndCompletedAtIsNotNull(
                        fileId, FILE_COMPLETION_CHUNK_INDEX);
        return completedDataChunks >= expectedDataChunks;
    }
}
