package com.customcharge.persistencelayer.recordtracking;

import com.customcharge.persistencelayer.exception.AbstractApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.stereotype.Component;

/**
 * Emits structured business-event logs for ELK. Failures here must never interrupt processing.
 */
@Component
public class RecordTrackingLogger {

    private static final Logger log = LoggerFactory.getLogger(RecordTrackingLogger.class);

    public void logEvent(RecordTrackingEvent event, RecordContext context) {
        logEvent(event, context, null);
    }

    public void logEvent(RecordTrackingEvent event, RecordContext context, Long durationMs) {
        safe(() -> {
            if (!RecordTrackingEventDeduper.tryAcquire(event)) {
                return;
            }
            MDC.put(RecordTrackingMdc.EVENT, event.name());
            RecordTrackingMdc.applyRecordContext(context);
            LoggingEventBuilder logBuilder = log.atInfo()
                    .addKeyValue("event", event.name());
            applyContextFields(logBuilder, context);
            applyMdcIdentityFields(logBuilder);
            applyTimelineFields(logBuilder, event, durationMs);
            logBuilder.log("record_tracking");
            clearTransientEventFields();
        });
    }

    public void logFailure(RecordTrackingEvent event, RecordContext context, AbstractApiException exception, Long durationMs) {
        safe(() -> {
            if (!RecordTrackingEventDeduper.tryAcquire(event)) {
                return;
            }
            MDC.put(RecordTrackingMdc.EVENT, event.name());
            RecordTrackingMdc.applyRecordContext(context);
            if (exception.getErrorCode() != null) {
                RecordTrackingMdc.putErrorCode(exception.getErrorCode().getCode());
            }
            LoggingEventBuilder logBuilder = log.atError()
                    .addKeyValue("event", event.name());
            applyContextFields(logBuilder, context);
            applyMdcIdentityFields(logBuilder);
            applyTimelineFields(logBuilder, event, durationMs);
            logBuilder.addKeyValue("errorCode", resolveErrorCode(exception));
            String errorMessage = exception.getMessage();
            if (errorMessage != null && !errorMessage.isBlank()) {
                logBuilder.addKeyValue("errorMessage", errorMessage);
            }
            logBuilder.log("record_tracking");
            clearTransientEventFields();
        });
    }

    public void logFailure(RecordTrackingEvent event, RecordContext context, Throwable throwable, Long durationMs) {
        safe(() -> {
            if (!RecordTrackingEventDeduper.tryAcquire(event)) {
                return;
            }
            MDC.put(RecordTrackingMdc.EVENT, event.name());
            RecordTrackingMdc.applyRecordContext(context);
            if (throwable instanceof AbstractApiException apiException && apiException.getErrorCode() != null) {
                RecordTrackingMdc.putErrorCode(apiException.getErrorCode().getCode());
            }
            LoggingEventBuilder logBuilder = log.atError()
                    .addKeyValue("event", event.name());
            applyContextFields(logBuilder, context);
            applyMdcIdentityFields(logBuilder);
            applyTimelineFields(logBuilder, event, durationMs);
            logBuilder.addKeyValue("errorCode", resolveErrorCode(throwable));
            if (throwable.getMessage() != null && !throwable.getMessage().isBlank()) {
                logBuilder.addKeyValue("errorMessage", throwable.getMessage());
            }
            logBuilder.log("record_tracking");
            clearTransientEventFields();
        });
    }

    private static void applyTimelineFields(
            LoggingEventBuilder logBuilder,
            RecordTrackingEvent event,
            Long durationMs) {
        if (event == RecordTrackingEvent.FILE_BATCH_COMPLETED || event == RecordTrackingEvent.FILE_BATCH_FAILED) {
            Long elapsedMs = RecordTrackingTimeline.elapsedSinceTraceMs();
            if (elapsedMs != null) {
                logBuilder.addKeyValue("elapsedMs", elapsedMs);
            }
        } else if (event == RecordTrackingEvent.RECORD_COMPLETED || event == RecordTrackingEvent.RECORD_FAILED) {
            Long recordElapsedMs = RecordTrackingTimeline.elapsedSinceRecordMs();
            if (recordElapsedMs != null) {
                logBuilder.addKeyValue("recordElapsedMs", recordElapsedMs);
            }
        }

        if (durationMs != null) {
            logBuilder.addKeyValue("durationMs", durationMs);
        }
    }

    private static void applyMdcIdentityFields(LoggingEventBuilder logBuilder) {
        addIfPresent(logBuilder, "fileId", MDC.get(RecordTrackingMdc.FILE_ID));
        addIfPresent(logBuilder, "uploadedBy", MDC.get(RecordTrackingMdc.UPLOADED_BY));
        addIfPresent(logBuilder, "entityCode", MDC.get(RecordTrackingMdc.ENTITY_CODE));
        addIfPresent(logBuilder, "processFor", MDC.get(RecordTrackingMdc.PROCESS_FOR));
    }

    private static void addIfPresent(LoggingEventBuilder logBuilder, String field, String value) {
        if (value != null && !value.isBlank()) {
            logBuilder.addKeyValue(field, value);
        }
    }

    private static void applyContextFields(LoggingEventBuilder logBuilder, RecordContext context) {
        if (context == null) {
            return;
        }
        if (context.awb() != null) {
            logBuilder.addKeyValue("awb", context.awb());
        }
        if (context.processFor() != null) {
            logBuilder.addKeyValue("processFor", context.processFor());
        }
        if (context.paymentReference() != null) {
            logBuilder.addKeyValue("paymentReference", context.paymentReference());
        }
        if (context.fileRowIndex() != null) {
            logBuilder.addKeyValue("fileRowIndex", context.fileRowIndex());
        }
        if (context.fileName() != null) {
            logBuilder.addKeyValue("fileName", context.fileName());
        }
        if (context.fileType() != null) {
            logBuilder.addKeyValue("fileType", context.fileType());
        }
        if (context.recordCount() != null) {
            logBuilder.addKeyValue("recordCount", context.recordCount());
        }
        if (context.failedCount() != null) {
            logBuilder.addKeyValue("failedCount", context.failedCount());
        }
    }

    private static void clearTransientEventFields() {
        MDC.remove(RecordTrackingMdc.EVENT);
        MDC.remove(RecordTrackingMdc.AWB);
        MDC.remove(RecordTrackingMdc.PAYMENT_REFERENCE);
        MDC.remove(RecordTrackingMdc.FILE_ROW_INDEX);
        MDC.remove(RecordTrackingMdc.ERROR_CODE);
    }

    private static String resolveErrorCode(Throwable throwable) {
        if (throwable instanceof AbstractApiException apiException && apiException.getErrorCode() != null) {
            return apiException.getErrorCode().getCode();
        }
        if (throwable != null && throwable.getClass() != null) {
            return throwable.getClass().getSimpleName();
        }
        return "UNEXPECTED";
    }

    private static void safe(Runnable action) {
        RecordTrackingGuard.runSafely(action);
    }
}
