package com.customcharge.dashboard.importfile.format;

import com.customcharge.dashboard.constants.ImportConstants;
import com.customcharge.dashboard.importfile.exception.ImportInternalException;
import com.customcharge.dashboard.importfile.support.ImportColumnUtils;
import com.customcharge.persistencelayer.exception.AbstractApiException;
import com.customcharge.persistencelayer.exception.ValidationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
public class XlsxSpreadsheetMultipartReader implements SpreadsheetMultipartReader {

    private final DataFormatter formatter = new DataFormatter();
    private final ObjectMapper objectMapper;

    public XlsxSpreadsheetMultipartReader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        String name = file.getOriginalFilename();
        if (name == null) {
            return false;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(ImportConstants.EXTENSION_XLS)) {
            return false;
        }
        return lower.endsWith(ImportConstants.EXTENSION_XLSX);
    }

    @Override
    public SpreadsheetReadOutcome read(MultipartFile file, List<String> requiredColumns, boolean trimCellValues) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new ValidationException(ImportConstants.MESSAGE_WORKBOOK_NO_SHEETS);
            }
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new ValidationException(ImportConstants.MESSAGE_SPREADSHEET_HEADER_ROW_REQUIRED);
            }
            List<String> headers = new ArrayList<>();
            headerRow.forEach(c -> headers.add(cellText(c, true)));
            Optional<String> missing = ImportColumnUtils.missingRequiredColumns(requiredColumns, headers);
            if (missing.isPresent()) {
                throw new ValidationException(missing.get());
            }

            int lastRow = sheet.getLastRowNum();
            int dataRows = 0;
            List<JsonNode> rows = new ArrayList<>();
            for (int r = 1; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (!rowIsEmpty(row)) {
                    rows.add(rowObject(headers, row, trimCellValues));
                    dataRows++;
                }
            }
            return new SpreadsheetReadOutcome(dataRows, rows);
        } catch (AbstractApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ImportInternalException(ImportConstants.MESSAGE_EXCEL_READ_FAILED, e);
        }
    }

    private boolean rowIsEmpty(Row row) {
        if (row == null) {
            return true;
        }
        for (Cell cell : row) {
            if (!cellText(cell,true).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String cellText(Cell cell, boolean trimCellValues) {
        if (cell == null) {
            return "";
        }
        String value = formatter.formatCellValue(cell);
        return trimCellValues ? value.strip() : value;
    }

    private JsonNode rowObject(List<String> headers, Row row, boolean trimCellValues) {
        ObjectNode node = objectMapper.createObjectNode();
        for (int i = 0; i < headers.size(); i++) {
            Cell cell = row.getCell(i);
            node.put(headers.get(i), cellText(cell, trimCellValues));
        }
        return node;
    }
}
