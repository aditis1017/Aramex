package com.customcharge.persistencelayer.export.dto;

import com.customcharge.persistencelayer.export.model.ExportJobStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ExportProgressPayload(
        UUID jobId,
        ExportJobStatus status,
        Long totalRecords,
        long processedRecords,
        BigDecimal percentComplete,
        Instant updatedAt,
        String errorCode,
        String errorMessage,
        boolean downloadAvailable,
        String downloadUrl
) {
}
