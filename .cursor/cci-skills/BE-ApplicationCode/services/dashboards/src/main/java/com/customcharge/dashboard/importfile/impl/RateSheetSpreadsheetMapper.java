package com.customcharge.dashboard.importfile.impl;

import com.customcharge.dashboard.constants.ExportHeaderConstants;
import com.customcharge.dashboard.constants.ImportConstants;
import com.customcharge.dashboard.importfile.support.ImportColumnUtils;
import com.customcharge.dashboard.service.RateSheetRateValidation;
import com.customcharge.persistencelayer.exception.ValidationException;
import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class RateSheetSpreadsheetMapper {

    private RateSheetSpreadsheetMapper() {
    }

    public record ParsedRateEntry(String rateType, String percentageOf, BigDecimal value) {
    }

    public record ParsedRateSheetImportItem(
            String description,
            String serviceCode,
            String shipmentType,
            String businessType,
            String categoryType,
            List<ParsedRateEntry> rates) {
    }

    /**
     * @param requiredColumns column labels from {@code direct-import-screens.yml} for {@code RATE_SHEET}.
     */
    public static List<ParsedRateSheetImportItem> mapRows(List<JsonNode> rows, List<String> requiredColumns) {
        List<String> required = requiredColumns == null ? List.of() : requiredColumns;
        List<ParsedRateSheetImportItem> parsed = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            parsed.add(toItem(rows.get(i), i + ImportConstants.DATA_ROW_NUMBER_OFFSET, required));
        }
        return parsed;
    }

    private static ParsedRateSheetImportItem toItem(JsonNode row, int rowNumber, List<String> requiredColumns) {
        Map<String, String> cells = rowMap(row);
        for (String columnLabel : requiredColumns) {
            require(cells, Objects.requireNonNull(columnLabel, "required column name"), rowNumber);
        }

        String serviceCode = require(cells, ExportHeaderConstants.SERVICE_CODE, rowNumber);
        String description = require(cells, ExportHeaderConstants.DESCRIPTION, rowNumber);
        String shipmentType = require(cells, ExportHeaderConstants.SHIPMENT_TYPE, rowNumber);
        String businessType = require(cells, ExportHeaderConstants.BUSINESS_TYPE, rowNumber);
        String categoryType = require(cells, ExportHeaderConstants.CATEGORY_TYPE, rowNumber);

        List<ParsedRateEntry> rates = parseRateSlots(cells, rowNumber);
        if (rates.isEmpty()) {
            throw new ValidationException(
                    String.format(ImportConstants.MESSAGE_ROW_AT_LEAST_ONE_RATE_REQUIRED, rowNumber));
        }

        return new ParsedRateSheetImportItem(
                description, serviceCode, shipmentType, businessType, categoryType, rates);
    }

    private static List<ParsedRateEntry> parseRateSlots(Map<String, String> cells, int rowNumber) {
        List<ParsedRateEntry> rates = new ArrayList<>(ImportConstants.MAX_RATE_SLOTS);
        for (int slot = 1; slot <= ImportConstants.MAX_RATE_SLOTS; slot++) {
            ParsedRateEntry entry = parseRateSlot(cells, slot, rowNumber);
            if (entry != null) {
                rates.add(entry);
            }
        }
        return List.copyOf(rates);
    }

    private static ParsedRateEntry parseRateSlot(Map<String, String> cells, int slot, int rowNumber) {
        String rateType = optional(cells, ImportConstants.RATE_TYPE_COLUMN_PREFIX + slot);
        if (rateType == null) {
            rateType = slot == 1 ? optional(cells, ExportHeaderConstants.RATE_TYPE) : null;
        }
        String percentageOf = optional(cells, ImportConstants.PERCENTAGE_OF_COLUMN_PREFIX + slot);
        if (percentageOf == null && slot == 1) {
            percentageOf = optional(cells, ExportHeaderConstants.PERCENTAGE_OF);
        }
        String valueRaw = optional(cells, ImportConstants.VALUE_COLUMN_PREFIX + slot);
        if (valueRaw == null && slot == 1) {
            valueRaw = optional(cells, ExportHeaderConstants.VALUE);
        }

        boolean hasRateType = rateType != null && !rateType.isBlank();
        boolean hasValue = valueRaw != null && !valueRaw.isBlank();
        boolean hasPercentage = percentageOf != null && !percentageOf.isBlank();

        if (!hasRateType && !hasValue && !hasPercentage) {
            return null;
        }

        if (!hasRateType || !hasValue) {
            throw new ValidationException(
                    String.format(ImportConstants.MESSAGE_ROW_RATE_TYPE_AND_VALUE_REQUIRED, rowNumber, slot, slot));
        }

        BigDecimal value = parseValue(valueRaw, rowNumber, slot);
        RateSheetRateValidation.validateRate(rateType, value, percentageOf, rowNumber);
        return new ParsedRateEntry(rateType, percentageOf, value);
    }

    private static Map<String, String> rowMap(JsonNode row) {
        Map<String, String> cells = HashMap.newHashMap(16);
        for (Iterator<String> it = row.fieldNames(); it.hasNext(); ) {
            String name = it.next();
            cells.put(ImportColumnUtils.normalizeHeader(name), cellText(row.get(name)));
        }
        return cells;
    }

    private static String cellText(JsonNode value) {
        return value == null || value.isNull() || value.isMissingNode() ? "" : value.asText("");
    }

    private static String require(Map<String, String> cells, String columnLabel, int rowNumber) {
        String value = cells.get(ImportColumnUtils.normalizeHeader(columnLabel));
        if (value == null || value.isBlank()) {
            throw new ValidationException(
                    String.format(ImportConstants.MESSAGE_ROW_MISSING_REQUIRED_VALUE, rowNumber, columnLabel));
        }
        return value.strip();
    }

    private static String optional(Map<String, String> cells, String columnLabel) {
        String value = cells.get(ImportColumnUtils.normalizeHeader(columnLabel));
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }

    private static BigDecimal parseValue(String raw, int rowNumber, int slot) {
        try {
            return new BigDecimal(raw.strip());
        } catch (NumberFormatException ex) {
            throw new ValidationException(
                    String.format(ImportConstants.MESSAGE_ROW_INVALID_NUMERIC_VALUE, rowNumber, slot));
        }
    }
}
