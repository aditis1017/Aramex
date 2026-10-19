package com.customcharge.persistencelayer.export.service;

import com.customcharge.persistencelayer.export.dto.ExportProgressPayload;
import com.customcharge.persistencelayer.export.entity.ExportJob;
import com.customcharge.persistencelayer.export.model.ExportJobStatus;
import com.customcharge.persistencelayer.export.repository.ExportJobRepository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

public class ExportJobProgressService {

    private final ExportJobRepository exportJobRepository;

    public ExportJobProgressService(ExportJobRepository exportJobRepository) {
        this.exportJobRepository = exportJobRepository;
    }

    @Transactional
    public void markInProgress(UUID jobId) {
        ExportJob job = exportJobRepository.findById(jobId).orElseThrow();
        job.setStatus(ExportJobStatus.IN_PROGRESS);
        exportJobRepository.save(job);
    }

    @Transactional
    public void setTotalRecords(UUID jobId, long totalRecords) {
        if (totalRecords <= 0) {
            return;
        }
        ExportJob job = exportJobRepository.findById(jobId).orElseThrow();
        job.setTotalRecords(totalRecords);
        long written = job.getProcessedRecords();
        if (written > 0) {
            job.setPercentComplete(BigDecimal.valueOf(written)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalRecords), 2, RoundingMode.HALF_UP));
        }
        exportJobRepository.save(job);
    }

    @Transactional
    public void applyWriteProgress(UUID jobId, long writeCount) {
        ExportJob job = exportJobRepository.findById(jobId).orElseThrow();
        job.setProcessedRecords(writeCount);
        if (job.getTotalRecords() != null && job.getTotalRecords() > 0) {
            job.setPercentComplete(BigDecimal.valueOf(writeCount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(job.getTotalRecords()), 2, RoundingMode.HALF_UP));
        }
        exportJobRepository.save(job);
    }

    @Transactional
    public void markFailed(UUID jobId, String errorCode, String message) {
        ExportJob job = exportJobRepository.findById(jobId).orElseThrow();
        job.setStatus(ExportJobStatus.FAILED);
        job.setErrorCode(errorCode);
        String safe = message == null ? "" : message;
        job.setErrorMessage(safe.length() > 4000 ? safe.substring(0, 4000) : safe);
        job.setCompletedAt(Instant.now());
        exportJobRepository.save(job);
    }

    @Transactional
    public void markCompleted(UUID jobId, String bucket, String key, long fileSizeBytes, String contentType) {
        ExportJob job = exportJobRepository.findById(jobId).orElseThrow();
        job.setStatus(ExportJobStatus.COMPLETED);
        job.setS3Bucket(bucket);
        job.setS3Key(key);
        job.setFileSizeBytes(fileSizeBytes);
        job.setContentType(contentType);
        job.setCompletedAt(Instant.now());
        if (job.getTotalRecords() == null || job.getTotalRecords() <= 0) {
            long fallback = Math.max(1L, job.getProcessedRecords());
            job.setTotalRecords(fallback);
        }
        job.setPercentComplete(BigDecimal.valueOf(100));
        exportJobRepository.save(job);
    }

    @Transactional(readOnly = true)
    public ExportProgressPayload payloadFor(UUID jobId) {
        ExportJob job = exportJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown export job: " + jobId));
        return toPayload(job);
    }

    public ExportProgressPayload toPayload(ExportJob job) {
        boolean download = job.getStatus() == ExportJobStatus.COMPLETED
                && job.getS3Key() != null && !job.getS3Key().isBlank();
        Instant updated = job.getUpdatedAt() != null ? job.getUpdatedAt() : Instant.now();
        return new ExportProgressPayload(
                job.getId(),
                job.getStatus(),
                job.getTotalRecords(),
                job.getProcessedRecords(),
                job.getPercentComplete(),
                updated,
                job.getErrorCode(),
                job.getErrorMessage(),
                download,
                null
        );
    }
}
