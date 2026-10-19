package com.customcharge.chargescalculator.dto;

import java.util.List;
import java.util.Map;

public record ChunkProcessingResult(int totalRecords, List<Map<String, String>> failedRecords) {

    public int failedCount() {
        return failedRecords != null ? failedRecords.size() : 0;
    }
}
