package com.customcharge.fileorchestration.dedup;

import java.util.Locale;
import java.util.Map;


public final class FileRowDedupKeyResolver {

    private FileRowDedupKeyResolver() {
    }

    public static String resolveKey(String processFor, Map<String, String> row) {
        if (processFor == null || row == null) {
            return null;
        }
        if (FileOrchestrationDedupConstants.PROCESS_FOR_ESTIMATE.equalsIgnoreCase(processFor.trim())) {
            return normalizeAwbKey(row);
        }
        if (FileOrchestrationDedupConstants.PROCESS_FOR_ACTUAL.equalsIgnoreCase(processFor.trim())) {
            String awb = normalizeAwbKey(row);
            String paymentReference = normalizePaymentReference(row);
            return awb + FileOrchestrationDedupConstants.KEY_SEPARATOR + paymentReference;
        }
        return null;
    }

    /**
     * Secondary Actual-only key: non-blank declaration number.
     * Blank/null declaration numbers are ignored (no declaration-level dedup).
     */
    public static String resolveDeclarationNumberKey(String processFor, Map<String, String> row) {
        if (!isActualProcess(processFor) || row == null) {
            return null;
        }
        String declarationNumber = lookupColumn(row, FileOrchestrationDedupConstants.DECLARATION_NUMBER);
        if (declarationNumber == null || declarationNumber.isBlank()) {
            return null;
        }
        return declarationNumber.strip().toLowerCase(Locale.ROOT);
    }

    public static boolean dedupApplies(String processFor) {
        if (processFor == null) {
            return false;
        }
        String trimmed = processFor.trim();
        return FileOrchestrationDedupConstants.PROCESS_FOR_ESTIMATE.equalsIgnoreCase(trimmed)
                || FileOrchestrationDedupConstants.PROCESS_FOR_ACTUAL.equalsIgnoreCase(trimmed);
    }

    public static boolean isActualProcess(String processFor) {
        return processFor != null
                && FileOrchestrationDedupConstants.PROCESS_FOR_ACTUAL.equalsIgnoreCase(processFor.trim());
    }

    private static String normalizeAwbKey(Map<String, String> row) {
        String awb = lookupColumn(row, FileOrchestrationDedupConstants.COLUMN_AWB);
        if (awb == null || awb.isBlank()) {
            return FileOrchestrationDedupConstants.MISSING_AWB_KEY;
        }
        return awb.strip().toLowerCase(Locale.ROOT);
    }

    private static String normalizePaymentReference(Map<String, String> row) {
        String paymentReference = lookupColumn(row, FileOrchestrationDedupConstants.PAYMENT_REFERENCE);
        if (paymentReference == null || paymentReference.isBlank()) {
            return "";
        }
        return paymentReference.strip().toLowerCase(Locale.ROOT);
    }

    static String lookupColumn(Map<String, String> row, String columnName) {
        if (row.containsKey(columnName)) {
            return row.get(columnName);
        }
        String lower = columnName.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> e : row.entrySet()) {
            if (e.getKey() != null && e.getKey().toLowerCase(Locale.ROOT).equals(lower)) {
                return e.getValue();
            }
        }
        return null;
    }
}
