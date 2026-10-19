package com.customcharge.persistencelayer.export;

import com.customcharge.persistencelayer.export.model.ExportContext;
import com.customcharge.persistencelayer.export.model.ExportFormat;
import com.customcharge.persistencelayer.export.model.ExportRow;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Creates and writes export files (CSV / XLSX).
 */
public final class ExportWriters {

    private static final int XLSX_ROW_WINDOW = 200;

    private ExportWriters() {
    }

    public static Writer open(ExportContext context, ObjectMapper objectMapper) throws IOException {
        return context.format() == ExportFormat.XLSX
                ? XlsxWriter.open(context.localFilePath(), objectMapper, context.columnNamesJson())
                : CsvWriter.open(context.localFilePath(), objectMapper, context.columnNamesJson());
    }

    public interface Writer extends AutoCloseable {
        void writeRows(List<ExportRow> rows) throws IOException;

        @Override
        void close() throws IOException;
    }

    private static final class CsvWriter implements Writer {
        private final BufferedWriter writer;
        private boolean headerWritten;
        private final List<String> columnNames;

        private CsvWriter(BufferedWriter writer, List<String> columnNames) {
            this.writer = writer;
            this.columnNames = columnNames;
        }

        static CsvWriter open(Path path, ObjectMapper objectMapper, String columnNamesJson) throws IOException {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
            writer.write('\uFEFF');
            List<String> columnNames = null;
            if (columnNamesJson != null && !columnNamesJson.isBlank()) {
                columnNames = objectMapper.readValue(columnNamesJson, new TypeReference<List<String>>() {});
            }
            CsvWriter csvWriter = new CsvWriter(writer, columnNames);
            csvWriter.writeHeader();
            return csvWriter;
        }

        @Override
        public void writeRows(List<ExportRow> rows) throws IOException {
            for (ExportRow row : rows) {
                writer.write(row.cells().stream().map(CsvWriter::escape).collect(Collectors.joining(",")));
                writer.newLine();
            }
            writer.flush();
        }

        @Override
        public void close() throws IOException {
            writer.close();
        }

        private void writeHeader() throws IOException {
            if (headerWritten || columnNames == null || columnNames.isEmpty()) {
                return;
            }
            writer.write(columnNames.stream().map(CsvWriter::escape).collect(Collectors.joining(",")));
            writer.newLine();
            headerWritten = true;
        }

        private static String escape(String raw) {
            if (raw == null) {
                return "";
            }
            boolean needQuotes = raw.contains(",") || raw.contains("\"") || raw.contains("\r") || raw.contains("\n");
            String escaped = raw.replace("\"", "\"\"");
            return needQuotes ? "\"" + escaped + "\"" : escaped;
        }
    }

    private static final class XlsxWriter implements Writer {
        private final Path path;
        private final SXSSFWorkbook workbook;
        private final SXSSFSheet sheet;
        private int rowIndex;
        private boolean headerWritten;
        private final List<String> columnNames;

        private XlsxWriter(Path path, SXSSFWorkbook workbook, SXSSFSheet sheet, List<String> columnNames) {
            this.path = path;
            this.workbook = workbook;
            this.sheet = sheet;
            this.columnNames = columnNames;
        }

        static XlsxWriter open(Path path, ObjectMapper objectMapper, String columnNamesJson) throws IOException {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            List<String> columnNames = null;
            if (columnNamesJson != null && !columnNamesJson.isBlank()) {
                columnNames = objectMapper.readValue(columnNamesJson, new TypeReference<List<String>>() {});
            }
            SXSSFWorkbook workbook = new SXSSFWorkbook(XLSX_ROW_WINDOW);
            SXSSFSheet sheet = workbook.createSheet("export");
            XlsxWriter xlsxWriter = new XlsxWriter(path, workbook, sheet, columnNames);
            xlsxWriter.writeHeader();
            return xlsxWriter;
        }

        @Override
        public void writeRows(List<ExportRow> rows) {
            for (ExportRow row : rows) {
                Row dataRow = sheet.createRow(rowIndex++);
                List<String> cells = row.cells();
                for (int i = 0; i < cells.size(); i++) {
                    dataRow.createCell(i).setCellValue(cells.get(i) != null ? cells.get(i) : "");
                }
            }
        }

        @Override
        public void close() throws IOException {
            try (OutputStream out = Files.newOutputStream(path)) {
                workbook.write(out);
                workbook.dispose();
            }
        }

        private void writeHeader() {
            if (headerWritten || columnNames == null || columnNames.isEmpty()) {
                return;
            }
            Row header = sheet.createRow(rowIndex++);
            for (int i = 0; i < columnNames.size(); i++) {
                header.createCell(i).setCellValue(columnNames.get(i));
            }
            headerWritten = true;
        }
    }
}
