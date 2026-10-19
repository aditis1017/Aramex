package com.customcharge.persistencelayer.export.entity;

import com.customcharge.persistencelayer.export.model.ExportFormat;
import com.customcharge.persistencelayer.export.model.ExportJobStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "export_job",
        indexes = {
                @Index(name = "idx_export_job_status_created", columnList = "status,created_at"),
                @Index(name = "idx_export_job_type_created", columnList = "export_type,created_at")
        }
)
@Getter
@Setter
public class ExportJob {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "export_type", nullable = false, length = 128)
    private String exportType;

    @Enumerated(EnumType.STRING)
    @Column(name = "format", nullable = false, length = 16)
    private ExportFormat format;

    @Column(name = "filter_spec", columnDefinition = "TEXT")
    private String filterSpec;

    @Column(name = "column_names_json", columnDefinition = "TEXT")
    private String columnNamesJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ExportJobStatus status = ExportJobStatus.INITIATED;

    @Column(name = "total_records")
    private Long totalRecords;

    @Column(name = "processed_records", nullable = false)
    private long processedRecords;

    @Column(name = "percent_complete", precision = 7, scale = 2)
    private BigDecimal percentComplete;

    @Column(name = "correlation_id", length = 128)
    private String correlationId;

    @Column(name = "requested_by", length = 256)
    private String requestedBy;

    @Column(name = "error_code", length = 64)
    private String errorCode;

    @Column(name = "error_message", length = 4000)
    private String errorMessage;

    @Column(name = "s3_bucket", length = 256)
    private String s3Bucket;

    @Column(name = "s3_key", length = 1024)
    private String s3Key;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "content_type", length = 128)
    private String contentType;

    @Column(name = "local_temp_path", length = 2048)
    private String localTempPath;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}
