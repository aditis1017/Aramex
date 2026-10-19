package com.customcharge.persistencelayer.export.registry;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DefaultExportPipelineRegistry implements ExportPipelineRegistry {

    private final Map<String, ExportPipeline> byType;

    public DefaultExportPipelineRegistry(List<ExportPipeline> pipelines) {
        this.byType = pipelines.stream()
                .collect(Collectors.toMap(
                        p -> p.exportType().toUpperCase(Locale.ROOT),
                        Function.identity(),
                        (a, b) -> {
                            throw new IllegalStateException("Duplicate ExportPipeline for exportType: " + a.exportType());
                        }));
    }

    @Override
    public ExportPipeline require(String exportType) {
        if (exportType == null || exportType.isBlank()) {
            throw new IllegalArgumentException("exportType is required");
        }
        ExportPipeline p = byType.get(exportType.toUpperCase(Locale.ROOT));
        if (p == null) {
            throw new IllegalArgumentException("No ExportPipeline registered for exportType=" + exportType);
        }
        return p;
    }

    @Override
    public boolean supports(String exportType) {
        if (exportType == null || exportType.isBlank()) {
            return false;
        }
        return byType.containsKey(exportType.toUpperCase(Locale.ROOT));
    }
}
