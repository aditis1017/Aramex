package com.customcharge.persistencelayer.export.registry;

import com.customcharge.persistencelayer.export.model.ExportContext;
import com.customcharge.persistencelayer.export.model.ExportRow;

import java.util.List;
import java.util.OptionalLong;

public interface ExportPipeline {

    String exportType();

    ExportItemReader createReader(ExportContext context) throws Exception;

    ExportItemProcessor createProcessor(ExportContext context);

    default List<String> columnNames() {
        return List.of();
    }

    default String fileNamePrefix() {
        return exportType();
    }

    default OptionalLong resolveExportTotalRecords(ExportContext context) throws Exception {
        return OptionalLong.empty();
    }
}
