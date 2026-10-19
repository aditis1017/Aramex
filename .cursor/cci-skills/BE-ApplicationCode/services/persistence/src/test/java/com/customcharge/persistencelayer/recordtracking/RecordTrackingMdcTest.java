package com.customcharge.persistencelayer.recordtracking;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RecordTrackingMdcTest {

    @AfterEach
    void tearDown() {
        RecordTrackingMdc.clear();
    }

    @Test
    void putHelpers_shouldTrimAndSkipBlankValues() {
        RecordTrackingMdc.putTraceId("  trace-1  ");
        RecordTrackingMdc.putFileId(" ");
        RecordTrackingMdc.putChunkId(42);
        RecordTrackingMdc.putChunkId(null);

        assertThat(MDC.get(RecordTrackingMdc.TRACE_ID)).isEqualTo("trace-1");
        assertThat(MDC.get(RecordTrackingMdc.FILE_ID)).isNull();
        assertThat(MDC.get(RecordTrackingMdc.CHUNK_ID)).isEqualTo("42");
    }

    @Test
    void applyFileIdentity_shouldSetEntityNameFromCode() {
        RecordTrackingMdc.applyFileIdentity("user@test.com", " DXB ");

        assertThat(MDC.get(RecordTrackingMdc.UPLOADED_BY)).isEqualTo("user@test.com");
        assertThat(MDC.get(RecordTrackingMdc.ENTITY_CODE)).isEqualTo("DXB");
        assertThat(MDC.get(RecordTrackingMdc.ENTITY_NAME)).isEqualTo("DXB");
    }

    @Test
    void putContext_shouldPopulateAllFields() {
        RecordTrackingMdc.putContext("t1", "f1", 3, "r1", "svc");

        assertThat(MDC.get(RecordTrackingMdc.TRACE_ID)).isEqualTo("t1");
        assertThat(MDC.get(RecordTrackingMdc.FILE_ID)).isEqualTo("f1");
        assertThat(MDC.get(RecordTrackingMdc.CHUNK_ID)).isEqualTo("3");
        assertThat(MDC.get(RecordTrackingMdc.RECORD_ID)).isEqualTo("r1");
        assertThat(MDC.get(RecordTrackingMdc.SERVICE_NAME)).isEqualTo("svc");
    }

    @Test
    void applyRecordContext_shouldPopulateBusinessFields() {
        RecordContext context = new RecordContext("AWB1", "actual_template", "PAY1", "5", "file.csv", "csv", 10, 1);

        RecordTrackingMdc.applyRecordContext(context);

        assertThat(MDC.get(RecordTrackingMdc.AWB)).isEqualTo("AWB1");
        assertThat(MDC.get(RecordTrackingMdc.PROCESS_FOR)).isEqualTo("actual_template");
        assertThat(MDC.get(RecordTrackingMdc.PAYMENT_REFERENCE)).isEqualTo("PAY1");
        assertThat(MDC.get(RecordTrackingMdc.FILE_ROW_INDEX)).isEqualTo("5");
        assertThat(MDC.get(RecordTrackingMdc.FILE_NAME)).isEqualTo("file.csv");
    }

    @Test
    void applyRecordContext_shouldNoOpWhenNull() {
        RecordTrackingMdc.applyRecordContext(null);
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    @Test
    void putContextMap_shouldSkipBlankValues() {
        RecordTrackingMdc.putContext(Map.of("customKey", "value", "blank", " "));

        assertThat(MDC.get("customKey")).isEqualTo("value");
        assertThat(MDC.get("blank")).isNull();
    }

    @Test
    void snapshotAndRestore_shouldRoundTrip() {
        RecordTrackingMdc.putTraceId("trace");
        Map<String, String> snapshot = RecordTrackingMdc.snapshot();

        RecordTrackingMdc.clear();
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();

        RecordTrackingMdc.restore(snapshot);
        assertThat(MDC.get(RecordTrackingMdc.TRACE_ID)).isEqualTo("trace");
    }

    @Test
    void restoreNull_shouldClearMdc() {
        RecordTrackingMdc.putTraceId("trace");
        RecordTrackingMdc.restore(null);
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

}
