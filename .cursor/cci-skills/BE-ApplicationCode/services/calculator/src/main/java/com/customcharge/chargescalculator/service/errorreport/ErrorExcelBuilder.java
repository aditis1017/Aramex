package com.customcharge.chargescalculator.service.errorreport;

import com.customcharge.chargescalculator.constants.BatchProcessingConstants;
import com.customcharge.persistencelayer.RecordPayloadMetaKeys;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class ErrorExcelBuilder {

    private static final List<String> TRAILING_HEADERS = List.of(
            BatchProcessingConstants.ERROR_CODE_KEY,
            BatchProcessingConstants.ERROR_KEY,
            RecordPayloadMetaKeys.META_FILE_ROW_INDEX);

    public Set<String> collectHeaders(List<Map<String, String>> rows, Set<String> currentHeaders) {
        Set<String> headers = currentHeaders == null ? new LinkedHashSet<>() : currentHeaders;
        if (rows == null || rows.isEmpty()) {
            return headers;
        }
        for (Map<String, String> row : rows) {
            if (row != null && !row.isEmpty()) {
                headers.addAll(row.keySet());
            }
        }
        return headers;
    }

    public List<String> finalizeHeaderOrder(Set<String> discoveredHeaders) {
        Set<String> trailingKeys = Set.copyOf(TRAILING_HEADERS);
        List<String> ordered = new ArrayList<>();
        if (discoveredHeaders != null) {
            for (String header : discoveredHeaders) {
                if (!trailingKeys.contains(header)) {
                    ordered.add(header);
                }
            }
        }
        ordered.addAll(TRAILING_HEADERS);
        return ordered;
    }

    public String toDisplayHeader(String internalKey) {
        if (BatchProcessingConstants.ERROR_CODE_KEY.equals(internalKey)) {
            return "Error Code";
        }
        if (BatchProcessingConstants.ERROR_KEY.equals(internalKey)) {
            return "Error Message";
        }
        if (RecordPayloadMetaKeys.META_FILE_ROW_INDEX.equals(internalKey)) {
            return "Row Index";
        }
        return internalKey;
    }

    public StreamingWorkbook createStreamingWorkbook(List<String> headers, int rowAccessWindowSize) {
        if (headers == null || headers.isEmpty()) {
            throw new IllegalArgumentException("Headers are required to create workbook");
        }
        SXSSFWorkbook workbook = new SXSSFWorkbook(Math.max(50, rowAccessWindowSize));
        SXSSFSheet sheet = workbook.createSheet("Errors");
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(toDisplayHeader(headers.get(i)));
        }
        return new StreamingWorkbook(workbook, sheet, headers);
    }

    public static class StreamingWorkbook implements AutoCloseable {
        private final SXSSFWorkbook workbook;
        private final SXSSFSheet sheet;
        private final List<String> headers;
        private int rowIndex = 1;

        private StreamingWorkbook(SXSSFWorkbook workbook, SXSSFSheet sheet, List<String> headers) {
            this.workbook = workbook;
            this.sheet = sheet;
            this.headers = headers;
        }

        public void appendRows(List<Map<String, String>> rows) {
            if (rows == null || rows.isEmpty()) {
                return;
            }
            for (Map<String, String> rowNode : rows) {
                Row sheetRow = sheet.createRow(rowIndex++);
                for (int colIndex = 0; colIndex < headers.size(); colIndex++) {
                    String header = headers.get(colIndex);
                    String value = rowNode == null ? "" : asText(rowNode.get(header));
                    sheetRow.createCell(colIndex).setCellValue(value);
                }
            }
        }

        public void writeTo(Path targetPath) {
            try (OutputStream outputStream = Files.newOutputStream(targetPath)) {
                workbook.write(outputStream);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to write streaming workbook", e);
            }
        }

        @Override
        public void close() throws IOException {
            workbook.close();
            workbook.dispose();
        }

        private String asText(String value) {
            if (value == null) {
                return "";
            }
            return value;
        }
    }
}
