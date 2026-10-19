package com.customcharge.fileorchestration.dedup;

import com.customcharge.persistencelayer.RecordPayloadMetaKeys;
import com.customcharge.persistencelayer.exception.ErrorCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LastRowWinsDedupeCollector {

    private final String processFor;
    private final boolean dedupEnabled;
    private final LinkedHashMap<String, Map<String, String>> byKey = new LinkedHashMap<>();
    private final List<Map<String, String>> passThroughRows = new ArrayList<>();
    private final List<Map<String, String>> duplicateRows = new ArrayList<>();
    private int rawRowCount;
    private boolean finalized;
    private List<Map<String, String>> finalizedRows;

    public LastRowWinsDedupeCollector(String processFor, boolean dedupEnabled) {
        this.processFor = processFor;
        this.dedupEnabled = dedupEnabled;
    }

    public void accept(Map<String, String> row) {
        if (finalized) {
            throw new IllegalStateException("Cannot accept rows after dedup has been finalized");
        }
        rawRowCount++;
        if (row == null) {
            return;
        }
        if (!dedupEnabled || !FileRowDedupKeyResolver.dedupApplies(processFor)) {
            passThroughRows.add(copyRow(row, rawRowCount));
            return;
        }
        String key = FileRowDedupKeyResolver.resolveKey(processFor, row);
        if (key == null) {
            passThroughRows.add(copyRow(row, rawRowCount));
            return;
        }
        Map<String, String> previous = byKey.put(key, copyRow(row, rawRowCount));
        if (previous != null) {
            duplicateRows.add(toErrorRow(previous, primaryDuplicateMessage()));
        }
    }

    public List<Map<String, String>> toOrderedRows() {
        ensureFinalized();
        return finalizedRows;
    }

    public List<Map<String, String>> getDuplicateRows() {
        ensureFinalized();
        return List.copyOf(duplicateRows);
    }

    public int getRawRowCount() {
        return rawRowCount;
    }

    public int getDedupedRowCount() {
        ensureFinalized();
        return finalizedRows.size();
    }

    public int getDroppedCount() {
        return Math.max(0, getRawRowCount() - getDedupedRowCount());
    }

    private void ensureFinalized() {
        if (finalized) {
            return;
        }
        finalized = true;
        if (!passThroughRows.isEmpty()) {
            finalizedRows = List.copyOf(passThroughRows);
            return;
        }
        List<Map<String, String>> primaryRows = List.copyOf(byKey.values());
        if (dedupEnabled && FileRowDedupKeyResolver.isActualProcess(processFor)) {
            finalizedRows = List.copyOf(applyDeclarationNumberLastWins(primaryRows));
            return;
        }
        finalizedRows = primaryRows;
    }

    /**
     * Actual uploads: among rows that survived awb|saddad dedup, keep the last row
     * for each non-blank declaration number. Blank/null declaration numbers pass through.
     */
    private List<Map<String, String>> applyDeclarationNumberLastWins(List<Map<String, String>> rows) {
        LinkedHashMap<String, Map<String, String>> byDeclarationNumber = new LinkedHashMap<>();

        for (Map<String, String> row : rows) {
            String declarationKey = FileRowDedupKeyResolver.resolveDeclarationNumberKey(processFor, row);
            if (declarationKey == null) {
                continue;
            }
            Map<String, String> previous = byDeclarationNumber.put(declarationKey, row);
            if (previous != null) {
                duplicateRows.add(toErrorRow(
                        previous,
                        FileOrchestrationDedupConstants.DUPLICATE_DECLARATION_NUMBER_ERROR_MESSAGE));
            }
        }

        List<Map<String, String>> ordered = new ArrayList<>();
        for (Map<String, String> row : rows) {
            String declarationKey = FileRowDedupKeyResolver.resolveDeclarationNumberKey(processFor, row);
            if (declarationKey == null) {
                ordered.add(row);
                continue;
            }
            if (byDeclarationNumber.get(declarationKey) == row) {
                ordered.add(row);
            }
        }
        return ordered;
    }

    private String primaryDuplicateMessage() {
        if (FileRowDedupKeyResolver.isActualProcess(processFor)) {
            return FileOrchestrationDedupConstants.DUPLICATE_AWB_SADDAD_ERROR_MESSAGE;
        }
        return FileOrchestrationDedupConstants.DUPLICATE_AWB_ERROR_MESSAGE;
    }

    private static Map<String, String> copyRow(Map<String, String> row, int fileRowIndex) {
        Map<String, String> copy = copyWithoutNullValues(row);
        copy.put(RecordPayloadMetaKeys.META_FILE_ROW_INDEX, Integer.toString(fileRowIndex));
        return Map.copyOf(copy);
    }

    private static Map<String, String> toErrorRow(Map<String, String> row, String errorMessage) {
        Map<String, String> copy = copyWithoutNullValues(row);
        copy.put("errorCode", ErrorCode.VALIDATION_FAILED.getCode());
        copy.put("errorMessage", errorMessage);
        return Map.copyOf(copy);
    }

    private static Map<String, String> copyWithoutNullValues(Map<String, String> row) {
        Map<String, String> copy = new HashMap<>();
        for (Map.Entry<String, String> entry : row.entrySet()) {
            String key = entry.getKey();
            if (key == null) {
                continue;
            }
            String value = entry.getValue();
            copy.put(key, value == null ? "" : value);
        }
        return copy;
    }
}
