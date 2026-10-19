package com.customcharge.persistencelayer.entities;

import com.customcharge.persistencelayer.entity.AbstractEntity;
import com.customcharge.persistencelayer.enums.FileUploadStatus;
import jakarta.persistence.*;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "staged_file_upload")
@Getter
@Setter
public class StagedFileUpload extends AbstractEntity {

    @Column(name = "file_id")
    private String fileId;

    @Column(name = "upload_profile", nullable = false, length = 128)
    private String uploadProfile;

    @Column(name = "original_file_name", nullable = false, length = 512)
    private String originalFileName;

    @Column(name = "s3_bucket", nullable = false, length = 255)
    private String s3Bucket;

    @Column(name = "s3_key", nullable = false, length = 1024)
    private String s3Key;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private FileUploadStatus status;

    @Column(name = "error_message", length = 4000)
    private String errorMessage;

    @Column(name = "total_data_rows")
    private Long totalDataRows;

    @Column(name = "expected_data_chunks")
    private Integer expectedDataChunks;

    @Column(name = "error_report_sent_at")
    private Instant errorReportSentAt;

    @Column(name = "entity_code")
    private String entityCode;
}
