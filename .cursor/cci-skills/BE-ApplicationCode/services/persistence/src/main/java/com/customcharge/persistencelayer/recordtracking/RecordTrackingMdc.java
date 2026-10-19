package com.customcharge.persistencelayer.recordtracking;

import com.customcharge.persistencelayer.audit.AuditAuthorContext;
import org.slf4j.MDC;

import java.util.Map;
import java.util.Optional;

/**
 * MDC helpers for end-to-end record tracing (ELK / structured logs).
 * Always call {@link #clear()} in a finally block after processing.
 */
public final class RecordTrackingMdc {

    public static final String TRACE_ID = "traceId";
    public static final String FILE_ID = "fileId";
    public static final String FILE_NAME = "fileName";
    public static final String CHUNK_ID = "chunkId";
    public static final String RECORD_ID = "recordId";
    public static final String SERVICE_NAME = "serviceName";
    public static final String EVENT = "event";
    public static final String AWB = "awb";
    public static final String PROCESS_FOR = "processFor";
    public static final String PAYMENT_REFERENCE = "paymentReference";
    public static final String FILE_ROW_INDEX = "fileRowIndex";
    public static final String ERROR_CODE = "errorCode";
    public static final String UPLOADED_BY = "uploadedBy";
    public static final String ENTITY_CODE = "entityCode";
    public static final String ENTITY_NAME = "entityName";

    private RecordTrackingMdc() {
    }

    public static void putTraceId(String traceId) {
        putIfPresent(TRACE_ID, traceId);
    }

    public static void putFileId(String fileId) {
        putIfPresent(FILE_ID, fileId);
    }

    public static void putFileName(String fileName) {
        putIfPresent(FILE_NAME, fileName);
    }

    public static void putChunkId(Object chunkId) {
        if (chunkId != null) {
            putIfPresent(CHUNK_ID, String.valueOf(chunkId));
        }
    }

    public static void putRecordId(String recordId) {
        putIfPresent(RECORD_ID, recordId);
    }

    public static void putServiceName(String serviceName) {
        putIfPresent(SERVICE_NAME, serviceName);
    }

    public static void putAwb(String awb) {
        putIfPresent(AWB, awb);
    }

    public static void putProcessFor(String processFor) {
        putIfPresent(PROCESS_FOR, processFor);
    }

    public static void putPaymentReference(String paymentReference) {
        putIfPresent(PAYMENT_REFERENCE, paymentReference);
    }

    public static void putFileRowIndex(String fileRowIndex) {
        putIfPresent(FILE_ROW_INDEX, fileRowIndex);
    }

    public static void putErrorCode(String errorCode) {
        putIfPresent(ERROR_CODE, errorCode);
    }

    public static void putUploadedBy(String uploadedBy) {
        putIfPresent(UPLOADED_BY, uploadedBy);
        syncAuditAuthorContext(uploadedBy);
    }

    public static Optional<String> uploadedBy() {
        String value = MDC.get(UPLOADED_BY);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value.trim());
    }

    public static void putEntityCode(String entityCode) {
        putIfPresent(ENTITY_CODE, entityCode);
    }

    public static void putEntityName(String entityName) {
        putIfPresent(ENTITY_NAME, entityName);
    }

    public static void applyFileIdentity(String uploadedBy, String entityCode) {
        putUploadedBy(uploadedBy);
        putEntityCode(entityCode);
        if (entityCode != null && !entityCode.isBlank()) {
            putEntityName(entityCode.trim());
        }
    }

    public static void putContext(String traceId, String fileId, Object chunkId, String recordId, String serviceName) {
        putTraceId(traceId);
        putFileId(fileId);
        putChunkId(chunkId);
        putRecordId(recordId);
        putServiceName(serviceName);
    }

    public static void applyRecordContext(RecordContext context) {
        if (context == null) {
            return;
        }
        putProcessFor(context.processFor());
        putAwb(context.awb());
        putPaymentReference(context.paymentReference());
        putFileRowIndex(context.fileRowIndex());
        putFileName(context.fileName());
    }

    public static void putContext(Map<String, String> context) {
        if (context == null) {
            return;
        }
        context.forEach((key, value) -> {
            if (value != null && !value.isBlank()) {
                MDC.put(key, value);
            }
        });
    }

    public static Map<String, String> snapshot() {
        return MDC.getCopyOfContextMap();
    }

    public static void restore(Map<String, String> snapshot) {
        MDC.clear();
        if (snapshot != null) {
            MDC.setContextMap(snapshot);
            syncAuditAuthorContext(snapshot.get(UPLOADED_BY));
        } else {
            AuditAuthorContext.clear();
        }
    }

    public static void clear() {
        MDC.clear();
        AuditAuthorContext.clear();
    }

    public static void clearFileIdentity() {
        MDC.remove(UPLOADED_BY);
        MDC.remove(ENTITY_CODE);
        MDC.remove(ENTITY_NAME);
        AuditAuthorContext.clear();
    }

    private static void putIfPresent(String key, String value) {
        if (value != null && !value.isBlank()) {
            MDC.put(key, value.trim());
        }
    }

    private static void syncAuditAuthorContext(String uploadedBy) {
        if (uploadedBy == null || uploadedBy.isBlank()) {
            AuditAuthorContext.clear();
            return;
        }
        AuditAuthorContext.set(uploadedBy);
    }
}
