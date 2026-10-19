package com.customcharge.persistencelayer.entities;

import com.customcharge.persistencelayer.entity.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
        name = "processed_chunk",
        uniqueConstraints = @UniqueConstraint(name = "uk_processed_chunk_file_chunk", columnNames = {"file_id", "chunk_index"}),
        indexes = {
                @Index(name = "idx_processed_chunk_file_id", columnList = "file_id"),
                @Index(name = "idx_processed_chunk_file_completed", columnList = "file_id,completed_at")
        })
public class ProcessedChunk extends AbstractEntity {

    @Column(name = "file_id", nullable = false)
    private UUID fileId;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(name = "completed_at")
    private Instant completedAt;
}
