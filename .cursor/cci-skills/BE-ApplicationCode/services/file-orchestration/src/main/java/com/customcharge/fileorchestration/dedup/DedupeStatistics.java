package com.customcharge.fileorchestration.dedup;

import java.util.List;
import java.util.Map;

/**
 * Per-file dedupe counters. Created once per {@code FileProcessingContext} — never a shared bean.
 */
public class DedupeStatistics {

    private int rawRowCount;
    private int dedupedRowCount;
    private List<Map<String, String>> duplicateRows = List.of();
    private boolean populated;

    public void record(int rawRowCount, int dedupedRowCount, List<Map<String, String>> duplicateRows) {
        this.rawRowCount = rawRowCount;
        this.dedupedRowCount = dedupedRowCount;
        this.duplicateRows = duplicateRows == null ? List.of() : List.copyOf(duplicateRows);
        this.populated = true;
    }

    public int getRawRowCount() {
        return rawRowCount;
    }

    public int getDedupedRowCount() {
        return dedupedRowCount;
    }

    public int getDroppedCount() {
        return Math.max(0, rawRowCount - dedupedRowCount);
    }

    public List<Map<String, String>> getDuplicateRows() {
        return duplicateRows;
    }

    public boolean isPopulated() {
        return populated;
    }

    /** Drop retained duplicate-row maps after they have been persisted. */
    public void clearDuplicateRows() {
        this.duplicateRows = List.of();
    }
}
