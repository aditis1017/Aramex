package com.customcharge.dashboard.importfile.service;

import com.customcharge.dashboard.constants.ImportConstants;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class ImportTemplateXlsxHeaderEnricher {

    private static final DataFormatter FORMATTER = new DataFormatter();

    private ImportTemplateXlsxHeaderEnricher() {
    }

    static void writeTemplateWithExtraHeaders(InputStream templateIn, OutputStream out, List<String> extraHeaders)
            throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(templateIn)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new IOException(ImportConstants.MESSAGE_TEMPLATE_WORKBOOK_NO_SHEETS);
            }
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                headerRow = sheet.createRow(0);
            }
            int nextColumn = lastPopulatedHeaderColumnIndex(headerRow) + 1;
            for (String header : extraHeaders) {
                Cell cell = headerRow.createCell(nextColumn++);
                cell.setCellValue(header);
            }
            workbook.write(out);
        }
    }

    static List<String> distinctServiceDescriptions(List<String> descriptions) {
        Set<String> seen = new LinkedHashSet<>();
        List<String> result = new ArrayList<>();
        for (String raw : descriptions) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String trimmed = raw.strip();
            if (seen.add(trimmed)) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private static int lastPopulatedHeaderColumnIndex(Row headerRow) {
        int last = -1;
        for (Cell cell : headerRow) {
            if (cell == null || cell.getCellType() == CellType.BLANK) {
                continue;
            }
            String text = FORMATTER.formatCellValue(cell);
            if (text != null && !text.isBlank()) {
                last = Math.max(last, cell.getColumnIndex());
            }
        }
        return last;
    }
}
