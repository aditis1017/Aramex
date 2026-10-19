package com.customcharge.persistencelayer.recordtracking;

import com.customcharge.persistencelayer.exception.BadRequestException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.UUID;

class RecordTrackingLoggerTest {

    private final RecordTrackingLogger logger = new RecordTrackingLogger();

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString();
        MDC.put(RecordTrackingMdc.TRACE_ID, "trace-" + suffix);
        MDC.put(RecordTrackingMdc.FILE_ID, "file-" + suffix);
        MDC.put(RecordTrackingMdc.CHUNK_ID, "1");
        MDC.put(RecordTrackingMdc.RECORD_ID, "record-" + suffix);
        MDC.put(RecordTrackingMdc.FILE_NAME, "test.csv");
        MDC.put(RecordTrackingMdc.UPLOADED_BY, "tester");
        MDC.put(RecordTrackingMdc.ENTITY_CODE, "DXB");
        RecordTrackingTimeline.markTraceStart();
        RecordTrackingTimeline.markChunkStart();
        RecordTrackingTimeline.markRecordStart();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
        RecordTrackingTimeline.clearAll();
    }

    @Test
    void logEvent_shouldEmitProductionEvents() {
        RecordContext completion = RecordContext.fileCompletion("batch.csv", "csv", "actual_template", 10);
        logger.logEvent(RecordTrackingEvent.FILE_BATCH_COMPLETED, completion, 100L);
        logger.logEvent(RecordTrackingEvent.RECORD_COMPLETED, RecordContext.fromRecord(
                java.util.Map.of("awb", "AWB1"), "actual_template"), 25L);
    }

    @Test
    void logFailure_shouldEmitApiException() {
        RecordContext context = RecordContext.fromRecord(
                java.util.Map.of("awb", "AWB1"),
                "actual_template");
        logger.logFailure(
                RecordTrackingEvent.RECORD_FAILED,
                context,
                new BadRequestException("validation failed"),
                50L);
    }

    @Test
    void logFailure_shouldEmitGenericThrowable() {
        logger.logFailure(
                RecordTrackingEvent.RECORD_FAILED,
                null,
                new RuntimeException("unexpected"),
                null);
        logger.logFailure(
                RecordTrackingEvent.RECORD_FAILED,
                null,
                new BadRequestException("api error"),
                10L);
    }
}
