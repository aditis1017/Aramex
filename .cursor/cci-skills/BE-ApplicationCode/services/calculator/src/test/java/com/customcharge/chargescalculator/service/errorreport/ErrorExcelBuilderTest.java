package com.customcharge.chargescalculator.service.errorreport;

import com.customcharge.chargescalculator.constants.BatchProcessingConstants;
import com.customcharge.persistencelayer.RecordPayloadMetaKeys;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ErrorExcelBuilderTest {

    private final ErrorExcelBuilder builder = new ErrorExcelBuilder();

    @Test
    void finalizeHeaderOrder_shouldMoveTrailingColumnsToEnd() {
        Set<String> discovered = new LinkedHashSet<>();
        discovered.add("awb");
        discovered.add(BatchProcessingConstants.ERROR_KEY);
        discovered.add("saddad");
        discovered.add(BatchProcessingConstants.ERROR_CODE_KEY);
        discovered.add(RecordPayloadMetaKeys.META_FILE_ROW_INDEX);

        assertThat(builder.finalizeHeaderOrder(discovered)).containsExactly(
                "awb",
                "saddad",
                BatchProcessingConstants.ERROR_CODE_KEY,
                BatchProcessingConstants.ERROR_KEY,
                RecordPayloadMetaKeys.META_FILE_ROW_INDEX);
    }

    @Test
    void finalizeHeaderOrder_shouldAlwaysAppendTrailingColumns_whenMissingFromDiscovery() {
        assertThat(builder.finalizeHeaderOrder(Set.of("awb"))).containsExactly(
                "awb",
                BatchProcessingConstants.ERROR_CODE_KEY,
                BatchProcessingConstants.ERROR_KEY,
                RecordPayloadMetaKeys.META_FILE_ROW_INDEX);
    }

    @Test
    void toDisplayHeader_shouldMapReservedColumns() {
        assertThat(builder.toDisplayHeader(BatchProcessingConstants.ERROR_CODE_KEY)).isEqualTo("Error Code");
        assertThat(builder.toDisplayHeader(BatchProcessingConstants.ERROR_KEY)).isEqualTo("Error Message");
        assertThat(builder.toDisplayHeader(RecordPayloadMetaKeys.META_FILE_ROW_INDEX)).isEqualTo("Row Index");
        assertThat(builder.toDisplayHeader("awb")).isEqualTo("awb");
    }

    @Test
    void collectHeaders_shouldReturnCurrentHeaders_whenRowsMissing() {
        LinkedHashSet<String> existing = new LinkedHashSet<>(List.of("existing"));
        assertThat(builder.collectHeaders(null, existing)).isSameAs(existing);
        assertThat(builder.collectHeaders(List.of(), existing)).isSameAs(existing);
    }

    @Test
    void collectHeaders_shouldStartFresh_whenCurrentHeadersNull() {
        assertThat(builder.collectHeaders(List.of(Map.of("awb", "AWB-1")), null))
                .containsExactly("awb");
    }

    @Test
    void collectHeaders_shouldIgnoreNullRowsAndPreserveExistingHeaders() {
        java.util.List<java.util.Map<String, String>> rows = new java.util.ArrayList<>();
        rows.add(null);
        rows.add(Map.of("awb", "AWB-1"));
        rows.add(Map.of());
        Set<String> headers = builder.collectHeaders(
                rows,
                new LinkedHashSet<>(List.of("existing")));

        assertThat(headers).containsExactlyInAnyOrder("existing", "awb");
    }

    @Test
    void createStreamingWorkbook_shouldThrow_whenHeadersMissing() {
        List<String> emptyHeaders = List.of();
        assertThatThrownBy(() -> builder.createStreamingWorkbook(emptyHeaders, 50))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Headers are required");
    }

    @Test
    void appendRows_shouldWriteEmptyCells_forNullRowValues(@TempDir Path tempDir) throws Exception {
        List<String> headers = List.of("awb", "saddad");
        Path output = tempDir.resolve("null-values.xlsx");
        try (ErrorExcelBuilder.StreamingWorkbook workbook = builder.createStreamingWorkbook(headers, 50)) {
            workbook.appendRows(List.of(Map.of("awb", "AWB-1")));
            workbook.writeTo(output);
        }

        try (InputStream inputStream = Files.newInputStream(output);
             XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            assertThat(workbook.getSheetAt(0).getRow(1).getCell(1).getStringCellValue()).isEmpty();
        }
    }

    @Test
    void writeTo_shouldWrapIOException(@TempDir Path tempDir) throws Exception {
        List<String> headers = List.of("awb");
        try (ErrorExcelBuilder.StreamingWorkbook workbook = builder.createStreamingWorkbook(headers, 50)) {
            assertThatThrownBy(() -> workbook.writeTo(tempDir))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Failed to write streaming workbook");
        }
    }

    @Test
    void appendRows_shouldIgnoreNullAndEmptyRows(@TempDir Path tempDir) throws Exception {
        List<String> headers = List.of("awb");
        Path output = tempDir.resolve("empty-rows.xlsx");
        try (ErrorExcelBuilder.StreamingWorkbook workbook = builder.createStreamingWorkbook(headers, 50)) {
            workbook.appendRows(null);
            workbook.appendRows(List.of());
            workbook.appendRows(List.of(Map.of("awb", "AWB-ONLY")));
            workbook.writeTo(output);
        }

        try (InputStream inputStream = Files.newInputStream(output);
             XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            assertThat(workbook.getSheetAt(0).getLastRowNum()).isEqualTo(1);
        }
    }

    @Test
    void createStreamingWorkbook_shouldWriteDisplayHeadersAndPreserveRowValues(@TempDir Path tempDir) throws Exception {
        List<String> headers = builder.finalizeHeaderOrder(Set.of(
                "awb",
                BatchProcessingConstants.ERROR_KEY,
                BatchProcessingConstants.ERROR_CODE_KEY,
                RecordPayloadMetaKeys.META_FILE_ROW_INDEX));

        Path output = tempDir.resolve("errors.xlsx");
        try (ErrorExcelBuilder.StreamingWorkbook workbook = builder.createStreamingWorkbook(headers, 50)) {
            workbook.appendRows(List.of(Map.of(
                    "awb", "AWB-1",
                    BatchProcessingConstants.ERROR_CODE_KEY, "CC001",
                    BatchProcessingConstants.ERROR_KEY, "Shipment not found",
                    RecordPayloadMetaKeys.META_FILE_ROW_INDEX, "42")));
            workbook.writeTo(output);
        }

        try (InputStream inputStream = Files.newInputStream(output);
             XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            Row headerRow = workbook.getSheetAt(0).getRow(0);
            assertThat(headerRow.getCell(0).getStringCellValue()).isEqualTo("awb");
            assertThat(headerRow.getCell(1).getStringCellValue()).isEqualTo("Error Code");
            assertThat(headerRow.getCell(2).getStringCellValue()).isEqualTo("Error Message");
            assertThat(headerRow.getCell(3).getStringCellValue()).isEqualTo("Row Index");

            Row dataRow = workbook.getSheetAt(0).getRow(1);
            assertThat(dataRow.getCell(0).getStringCellValue()).isEqualTo("AWB-1");
            assertThat(dataRow.getCell(1).getStringCellValue()).isEqualTo("CC001");
            assertThat(dataRow.getCell(2).getStringCellValue()).isEqualTo("Shipment not found");
            assertThat(dataRow.getCell(3).getStringCellValue()).isEqualTo("42");
        }
    }
}
