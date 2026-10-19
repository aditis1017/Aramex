package com.customcharge.persistencelayer.export.model;

import java.util.List;

public record ExportRow(List<String> cells) {
    public ExportRow {
        cells = cells == null ? List.of() : List.copyOf(cells);
    }
}
