package com.customcharge.fileorchestration.service.impl;

import com.customcharge.fileorchestration.service.FileRecordReader;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class XlsxMapFileRecordReader implements FileRecordReader<Map<String, String>> {

    private static final Logger log = LoggerFactory.getLogger(XlsxMapFileRecordReader.class);

    private OPCPackage opcPackage;
    private List<Map<String, String>> rows;
    private int rowIndex;

    @Override
    public boolean supports(String fileType) {
        return "XLSX".equalsIgnoreCase(fileType);
    }

    @Override
    public void open(InputStream inputStream) {
        try {
            this.opcPackage = OPCPackage.open(inputStream);
            XSSFReader xssfReader = new XSSFReader(opcPackage);
            StylesTable styles = xssfReader.getStylesTable();
            ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(opcPackage);
            XSSFReader.SheetIterator sheetIterator = (XSSFReader.SheetIterator) xssfReader.getSheetsData();

            if (!sheetIterator.hasNext()) {
                throw new IllegalArgumentException("XLSX file has no sheets");
            }

            XlsxSheetHandler handler = new XlsxSheetHandler();
            try (InputStream sheetStream = sheetIterator.next()) {
                XMLReader parser = XMLReaderFactory.createXMLReader();
                parser.setContentHandler(new XSSFSheetXMLHandler(styles, null, strings, handler, null, false));
                parser.parse(new InputSource(sheetStream));
            }

            this.rows = handler.getRows();
            this.rowIndex = 0;
            log.info("Loaded {} data rows from first XLSX sheet", rows.size());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to open XLSX reader", ex);
        }
    }

    @Override
    public Map<String, String> read() {
        if (rows == null || rowIndex >= rows.size()) {
            return null;
        }
        return rows.get(rowIndex++);
    }

    @Override
    public void close() {
        if (opcPackage != null) {
            try {
                opcPackage.close();
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to close XLSX reader", ex);
            }
        }
        rows = null;
        rowIndex = 0;
    }

    private static final class XlsxSheetHandler implements XSSFSheetXMLHandler.SheetContentsHandler {

        private final List<String> headers = new ArrayList<>();
        private final List<Map<String, String>> rows = new ArrayList<>();
        private List<String> currentRowValues;
        private int currentRowNum;

        @Override
        public void startRow(int rowNum) {
            currentRowNum = rowNum;
            currentRowValues = new ArrayList<>();
        }

        @Override
        public void endRow(int rowNum) {
            if (rowNum == 0) {
                extractHeaders();
                return;
            }
            if (headers.isEmpty()) {
                throw new IllegalArgumentException("XLSX header row is missing");
            }
            Map<String, String> row = new LinkedHashMap<>(headers.size());
            boolean hasAnyData = false;
            for (int col = 0; col < headers.size(); col++) {
                String value = col < currentRowValues.size() ? toNullIfBlank(currentRowValues.get(col)) : null;
                row.put(headers.get(col), value);
                if (value != null) {
                    hasAnyData = true;
                }
            }
            if (hasAnyData) {
                rows.add(row);
            }
        }

        @Override
        public void cell(String cellReference, String formattedValue, XSSFComment comment) {
            if (cellReference == null) {
                return;
            }
            int colIndex = parseColumnIndex(cellReference);
            ensureSize(currentRowValues, colIndex + 1);
            currentRowValues.set(colIndex, formattedValue);
        }

        @Override
        public void headerFooter(String text, boolean isHeader, String tagName) {
            // no-op
        }

        List<Map<String, String>> getRows() {
            return rows;
        }

        private void extractHeaders() {
            if (currentRowValues == null || currentRowValues.isEmpty()) {
                throw new IllegalArgumentException("XLSX header row is empty");
            }
            for (int i = 0; i < currentRowValues.size(); i++) {
                String header = toNullIfBlank(currentRowValues.get(i));
                if (header == null) {
                    throw new IllegalArgumentException("Invalid XLSX header: blank column name at index " + i);
                }
                String normalized = header.toLowerCase(Locale.ROOT);
                for (String existing : headers) {
                    if (existing.toLowerCase(Locale.ROOT).equals(normalized)) {
                        throw new IllegalArgumentException("Invalid XLSX header: duplicate column '" + header + "'");
                    }
                }
                headers.add(header);
            }
        }

        private static int parseColumnIndex(String cellReference) {
            int index = 0;
            while (index < cellReference.length() && Character.isLetter(cellReference.charAt(index))) {
                index++;
            }
            String colRef = cellReference.substring(0, index);
            int colIndex = 0;
            for (int i = 0; i < colRef.length(); i++) {
                colIndex = colIndex * 26 + (Character.toUpperCase(colRef.charAt(i)) - 'A' + 1);
            }
            return colIndex - 1;
        }

        private static void ensureSize(List<String> values, int size) {
            while (values.size() < size) {
                values.add(null);
            }
        }

        private static String toNullIfBlank(String value) {
            if (value == null) {
                return null;
            }
            String trimmed = value.trim();
            return trimmed.isEmpty() ? null : trimmed;
        }
    }
}
