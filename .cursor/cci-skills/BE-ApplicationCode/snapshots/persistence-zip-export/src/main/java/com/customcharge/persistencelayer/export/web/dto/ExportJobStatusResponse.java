package com.customcharge.persistencelayer.export.web.dto;

import com.customcharge.persistencelayer.export.model.ExportJobStatus;

import java.math.BigDecimal;

public record ExportJobStatusResponse(
        ExportJobStatus status,
        Long totalRecords,
        long processedRecords,
        BigDecimal percentComplete,
        String errorCode,
        String errorMessage,
        boolean downloadAvailable
) {
}
