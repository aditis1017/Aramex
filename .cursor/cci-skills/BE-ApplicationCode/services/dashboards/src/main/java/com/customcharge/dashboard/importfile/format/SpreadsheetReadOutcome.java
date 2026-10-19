package com.customcharge.dashboard.importfile.format;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public record SpreadsheetReadOutcome(int dataRowCount, List<JsonNode> rows) {

    public SpreadsheetReadOutcome {
        rows = List.copyOf(rows);
    }
}
