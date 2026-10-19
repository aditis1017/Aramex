package com.customcharge.persistencelayer.recordtracking;

import com.customcharge.persistencelayer.RecordPayloadMetaKeys;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RecordContextTest {

    @Test
    void fromRecord_shouldExtractAwbAndRowIndex() {
        RecordContext context = RecordContext.fromRecord(
                Map.of("awb", " AWB123 ", RecordPayloadMetaKeys.META_FILE_ROW_INDEX, " 7 "),
                "actual_template");

        assertThat(context.awb()).isEqualTo("AWB123");
        assertThat(context.fileRowIndex()).isEqualTo("7");
        assertThat(context.paymentReference()).isNull();
    }

    @Test
    void fromRecord_shouldIncludePaymentReferenceForActualTemplate() {
        RecordContext context = RecordContext.fromRecord(
                Map.of(RecordContext.PAYMENT_REFERENCE_KEY, " PAY-1 "),
                "actual_template");

        assertThat(context.paymentReference()).isEqualTo("PAY-1");
    }

    @Test
    void fromRecord_shouldHandleNullData() {
        RecordContext context = RecordContext.fromRecord(null, "other");
        assertThat(context.awb()).isNull();
        assertThat(context.processFor()).isEqualTo("other");
    }

    @Test
    void factoryMethods_shouldPopulateFields() {
        assertThat(RecordContext.fileMetadata("a.csv", "csv", "template").fileName()).isEqualTo("a.csv");
        assertThat(RecordContext.fileCompletion("b.csv", "csv", "template", 5).recordCount()).isEqualTo(5);
    }

    @Test
    void isActual_shouldDetectActualTemplate() {
        assertThat(RecordContext.isActual(" actual_template ")).isTrue();
        assertThat(RecordContext.isActual("other")).isFalse();
    }

    @Test
    void resolveRecordId_shouldPreferAwb() {
        String id = RecordContext.resolveRecordId(Map.of("awb", "AWB1"), "file-1", 2);
        assertThat(id).isEqualTo("AWB1");
    }

    @Test
    void resolveRecordId_shouldUseRowIndexWhenAwbMissing() {
        String id = RecordContext.resolveRecordId(
                Map.of(RecordPayloadMetaKeys.META_FILE_ROW_INDEX, " 9 "),
                "file-1",
                2);
        assertThat(id).isEqualTo("file-1:2:9");
    }

    @Test
    void resolveRecordId_shouldFallbackToUnknown() {
        assertThat(RecordContext.resolveRecordId(null, "file-1", 2)).isEqualTo("file-1:2:unknown");
    }
}
