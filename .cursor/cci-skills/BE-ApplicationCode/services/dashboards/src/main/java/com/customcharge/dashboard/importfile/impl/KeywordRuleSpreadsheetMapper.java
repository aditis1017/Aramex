package com.customcharge.dashboard.importfile.impl;

import com.customcharge.dashboard.constants.ExportHeaderConstants;
import com.customcharge.dashboard.constants.ImportConstants;
import com.customcharge.dashboard.importfile.support.ImportColumnUtils;
import com.customcharge.dashboard.keywordrules.dto.ParsedKeywordRuleRow;
import com.customcharge.persistencelayer.enums.MatchType;
import com.customcharge.persistencelayer.exception.ValidationException;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class KeywordRuleSpreadsheetMapper {

    private KeywordRuleSpreadsheetMapper() {
    }

    public static List<ParsedKeywordRuleRow> mapRows(List<JsonNode> rows) {
        List<ParsedKeywordRuleRow> parsed = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            parsed.add(toRow(rows.get(i), i + ImportConstants.DATA_ROW_NUMBER_OFFSET));
        }
        return parsed;
    }

    private static ParsedKeywordRuleRow toRow(JsonNode row, int rowNumber) {
        Map<String, String> m = rowMap(row);
        String keyword = requirePreservingWhitespace(m, ExportHeaderConstants.KEYWORD, rowNumber);
        validateKeyword(keyword, rowNumber);
        return new ParsedKeywordRuleRow(
                keyword,
                parseMatchType(require(m, ExportHeaderConstants.MATCH_TYPE, rowNumber), rowNumber),
                parseActive(m.get(ImportColumnUtils.normalizeHeader(ExportHeaderConstants.ACTIVE))));
    }

    private static Map<String, String> rowMap(JsonNode row) {
        Map<String, String> m = HashMap.newHashMap(8);
        for (Iterator<String> it = row.fieldNames(); it.hasNext(); ) {
            String name = it.next();
            m.put(ImportColumnUtils.normalizeHeader(name), cellText(row.get(name)));
        }
        return m;
    }

    private static String cellText(JsonNode v) {
        return v == null || v.isNull() || v.isMissingNode() ? "" : v.asText("");
    }

    private static String require(Map<String, String> m, String columnLabel, int rowNumber) {
        String v = m.get(ImportColumnUtils.normalizeHeader(columnLabel));
        if (v == null || v.isBlank()) {
            throw new ValidationException(
                    String.format(ImportConstants.MESSAGE_ROW_MISSING_REQUIRED_VALUE, rowNumber, columnLabel));
        }
        return v.strip();
    }

    private static String requirePreservingWhitespace(Map<String, String> m, String columnLabel, int rowNumber) {
        String v = m.get(ImportColumnUtils.normalizeHeader(columnLabel));
        if (v == null || v.isBlank()) {
            throw new ValidationException(
                    String.format(ImportConstants.MESSAGE_ROW_MISSING_REQUIRED_VALUE, rowNumber, columnLabel));
        }
        return v;
    }

    private static void validateKeyword(String keyword, int rowNumber) {
        if (keyword.length() < ImportConstants.KEYWORD_MIN_LENGTH) {
            throw new ValidationException(
                    String.format(ImportConstants.MESSAGE_ROW_KEYWORD_MIN_LENGTH, rowNumber, ImportConstants.KEYWORD_MIN_LENGTH));
        }
        if (!ImportConstants.KEYWORD_ALLOWED_CHARACTERS.matcher(keyword).matches()) {
            throw new ValidationException(
                    String.format(ImportConstants.MESSAGE_ROW_KEYWORD_INVALID_CHARACTERS, rowNumber));
        }
    }

    private static boolean parseActive(String raw) {
        if (raw == null || raw.isBlank()) {
            return true;
        }
        return switch (raw.strip().toLowerCase(Locale.ROOT)) {
            case ImportConstants.ACTIVE_TRUE, ImportConstants.ACTIVE_YES, ImportConstants.ACTIVE_Y,
                 ImportConstants.ACTIVE_ONE, ImportConstants.ACTIVE_ON, ImportConstants.ACTIVE_ACTIVE -> true;
            case ImportConstants.ACTIVE_FALSE, ImportConstants.ACTIVE_NO, ImportConstants.ACTIVE_N,
                 ImportConstants.ACTIVE_ZERO, ImportConstants.ACTIVE_OFF, ImportConstants.ACTIVE_INACTIVE -> false;
            default -> true;
        };
    }

    private static MatchType parseMatchType(String raw, int rowNumber) {
        String normalized = ImportConstants.normalizeMatchType(raw);
        if (!ImportConstants.isAllowedMatchType(normalized)) {
            throw new ValidationException(
                    String.format(ImportConstants.MESSAGE_ROW_INVALID_MATCH_TYPE, rowNumber));
        }
        return switch (normalized) {
            case "CONTAINS" -> MatchType.CONTAINS;
            case "EXACT" -> MatchType.EXACT;
            case "STARTWITH" -> MatchType.STARTWITH;
            case "ENDWITH" -> MatchType.ENDWITH;
            default -> throw new ValidationException(
                    String.format(ImportConstants.MESSAGE_ROW_INVALID_MATCH_TYPE, rowNumber));
        };
    }

}
