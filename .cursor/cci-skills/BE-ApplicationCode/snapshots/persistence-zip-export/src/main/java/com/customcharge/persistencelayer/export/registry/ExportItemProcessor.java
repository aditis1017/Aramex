package com.customcharge.persistencelayer.export.registry;

import com.customcharge.persistencelayer.export.model.ExportRow;

@FunctionalInterface
public interface ExportItemProcessor {

    ExportRow process(Object item) throws Exception;
}
