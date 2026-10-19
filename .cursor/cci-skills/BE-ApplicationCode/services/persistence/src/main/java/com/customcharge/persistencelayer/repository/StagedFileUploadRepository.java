package com.customcharge.persistencelayer.repository;

import com.customcharge.persistencelayer.entities.StagedFileUpload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface StagedFileUploadRepository extends JpaRepository<StagedFileUpload, UUID> {

    Optional<StagedFileUpload> findByS3BucketAndS3Key(String s3Bucket, String s3Key);

    boolean existsByFileId(String fileId);

    Optional<StagedFileUpload> findByFileId(String fileId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value = """
                    UPDATE staged_file_upload s
                    SET error_report_sent_at = :completedAt,
                        status = CASE
                            WHEN EXISTS (
                                SELECT 1 FROM custom_charge_file_error e
                                WHERE e.file_id = CAST(:fileId AS uuid)
                            ) THEN 'PARTIAL_PROCESS_COMPLETED'
                            ELSE 'PROCESS_COMPLETED'
                        END
                    WHERE s.file_id = :fileId AND s.error_report_sent_at IS NULL
                    """,
            nativeQuery = true)
    int markFileProcessingCompleteIfAbsent(
            @Param("fileId") String fileId,
            @Param("completedAt") Instant completedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE StagedFileUpload s
            SET s.expectedDataChunks = :expectedDataChunks
            WHERE s.fileId = :fileId AND s.expectedDataChunks IS NULL
            """)
    int saveExpectedDataChunksIfAbsent(
            @Param("fileId") String fileId,
            @Param("expectedDataChunks") int expectedDataChunks);
}
