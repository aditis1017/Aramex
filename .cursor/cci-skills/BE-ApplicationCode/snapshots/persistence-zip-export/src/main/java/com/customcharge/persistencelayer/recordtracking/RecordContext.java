package com.customcharge.persistencelayer.recordtracking;

import com.customcharge.persistencelayer.RecordPayloadMetaKeys;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Optional business fields attached to selective record-tracking log events.
 * Never includes the full record payload.
 */
public record RecordContext(
        String awb,
        String processFor,
        String paymentReference,
        String fileRowIndex,
        String fileName,
        String fileType,
        Integer recordCount,
        Integer failedCount) {

    public static final String AWB_KEY = "awb";
    public static final String PAYMENT_REFERENCE_KEY = "saddad";
    public static final String PROCESS_FOR_ACTUAL = "actual_template";

    public static RecordContext fromRecord(Map<String, String> recordData, String processFor) {
        if (recordData == null) {
            return new RecordContext(null, processFor, null, null, null, null, null, null);
        }
        Map<String, String> normalized = lowercaseKeys(recordData);
        String awb = textOrNull(normalized.get(AWB_KEY));
        String fileRowIndex = textOrNull(normalized.get(RecordPayloadMetaKeys.META_FILE_ROW_INDEX));
        String paymentReference = isActual(processFor) ? textOrNull(normalized.get(PAYMENT_REFERENCE_KEY)) : null;
        return new RecordContext(awb, processFor, paymentReference, fileRowIndex, null, null, null, null);
    }

    public static RecordContext fileMetadata(String fileName, String fileType, String processFor) {
        return new RecordContext(null, processFor, null, null, fileName, fileType, null, null);
    }

    public static RecordContext fileCompletion(String fileName, String fileType, String processFor, Integer recordCount) {
        return new RecordContext(null, processFor, null, null, fileName, fileType, recordCount, null);
    }

    public static boolean isActual(String processFor) {
        return processFor != null
                && PROCESS_FOR_ACTUAL.equals(processFor.trim().toLowerCase(Locale.ROOT));
    }

    public static String resolveRecordId(Map<String, String> recordData, String fileId, int chunkIndex) {
        if (recordData == null) {
            return fileId + ":" + chunkIndex + ":unknown";
        }
        String awb = textOrNull(recordData.get(AWB_KEY));
        if (awb == null) {
            awb = textOrNull(recordData.get("AWB"));
        }
        if (awb != null) {
            return awb;
        }
        String row = recordData.get(RecordPayloadMetaKeys.META_FILE_ROW_INDEX);
        if (row != null && !row.isBlank()) {
            return fileId + ":" + chunkIndex + ":" + row.strip();
        }
        return fileId + ":" + chunkIndex + ":unknown";
    }

    private static Map<String, String> lowercaseKeys(Map<String, String> source) {
        Map<String, String> normalized = new HashMap<>();
        source.forEach((key, value) -> {
            if (key != null) {
                normalized.put(key.toLowerCase(Locale.ROOT), value);
            }
        });
        return normalized;
    }

    private static String textOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }
}
