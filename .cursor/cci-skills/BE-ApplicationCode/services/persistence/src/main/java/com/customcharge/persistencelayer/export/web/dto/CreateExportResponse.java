package com.customcharge.persistencelayer.export.web.dto;

import com.customcharge.persistencelayer.export.model.ExportJobStatus;

import java.util.UUID;

public record CreateExportResponse(UUID jobId, ExportJobStatus status) {
}
