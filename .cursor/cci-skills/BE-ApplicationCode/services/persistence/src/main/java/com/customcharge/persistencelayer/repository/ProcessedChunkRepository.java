package com.customcharge.persistencelayer.repository;

import com.customcharge.persistencelayer.entities.ProcessedChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProcessedChunkRepository extends JpaRepository<ProcessedChunk, UUID> {

    Optional<ProcessedChunk> findByFileIdAndChunkIndex(UUID fileId, int chunkIndex);

    long countByFileIdAndCompletedAtIsNotNull(UUID fileId);

    boolean existsByFileIdAndChunkIndexAndCompletedAtIsNotNull(UUID fileId, int chunkIndex);

    long countByFileIdAndChunkIndexGreaterThanAndCompletedAtIsNotNull(UUID fileId, int chunkIndex);

    void deleteByFileIdAndChunkIndex(UUID fileId, int chunkIndex);
}
