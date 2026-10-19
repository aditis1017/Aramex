package com.customcharge.persistencelayer.export.registry;

public interface ExportPipelineRegistry {

    ExportPipeline require(String exportType);

    boolean supports(String exportType);
}
