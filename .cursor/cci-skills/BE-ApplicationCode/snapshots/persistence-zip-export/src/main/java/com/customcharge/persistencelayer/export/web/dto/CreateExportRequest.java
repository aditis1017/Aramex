package com.customcharge.persistencelayer.export.web.dto;

import com.customcharge.persistencelayer.export.model.ExportFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateExportRequest(
        @NotBlank String exportType,
        @NotNull ExportFormat format,
        String filterSpec,
        List<String> columnNames
) {
}
