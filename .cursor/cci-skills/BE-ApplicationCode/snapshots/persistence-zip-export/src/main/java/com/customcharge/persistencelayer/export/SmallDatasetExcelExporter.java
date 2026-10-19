package com.customcharge.persistencelayer.export;

import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;

@Component
public class SmallDatasetExcelExporter {

    private static final Logger log = LoggerFactory.getLogger(SmallDatasetExcelExporter.class);

    private static final int ROW_WINDOW = 200;

    public void writeExcelFile(List<String> headerList, List<List<String>> dataList, OutputStream outputStream) {
        validateInputs(headerList, dataList);
        if (outputStream == null) {
            throw new IllegalArgumentException("outputStream must not be null");
        }

        SXSSFWorkbook workbook = new SXSSFWorkbook(ROW_WINDOW);
        try {
            var sheet = workbook.createSheet("export");
            var headerRow = sheet.createRow(0);
            for (int columnIndex = 0; columnIndex < headerList.size(); columnIndex++) {
                headerRow.createCell(columnIndex).setCellValue(safeValue(headerList.get(columnIndex)));
            }

            int rowIndex = 1;
            for (List<String> rowData : dataList) {
                var row = sheet.createRow(rowIndex++);
                for (int columnIndex = 0; columnIndex < rowData.size(); columnIndex++) {
                    row.createCell(columnIndex).setCellValue(safeValue(rowData.get(columnIndex)));
                }
            }
            workbook.write(outputStream);
            outputStream.flush();
        } catch (IOException ex) {
            log.error("Failed to create excel file for small dataset export", ex);
            throw new IllegalStateException("Unable to create excel file", ex);
        } finally {
            workbook.dispose();
            try {
                workbook.close();
            } catch (IOException ex) {
                log.warn("Failed to close SXSSF workbook after export", ex);
            }
        }
    }

    private void validateInputs(List<String> headerList, List<List<String>> dataList) {
        if (headerList == null || headerList.isEmpty()) {
            throw new IllegalArgumentException("headerList must not be empty");
        }
        if (dataList == null) {
            throw new IllegalArgumentException("dataList must not be null");
        }
        int expectedColumnCount = headerList.size();
        for (List<String> rowData : dataList) {
            if (rowData == null || rowData.size() != expectedColumnCount) {
                throw new IllegalArgumentException("Each data row must match header size");
            }
        }
    }

    private String safeValue(String value) {
        return Objects.toString(value, "");
    }
}
