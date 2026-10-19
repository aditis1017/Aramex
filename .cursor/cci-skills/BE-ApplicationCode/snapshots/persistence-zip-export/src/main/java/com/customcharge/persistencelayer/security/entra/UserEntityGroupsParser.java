package com.customcharge.persistencelayer.security.entra;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Parses the authenticated user's {@code users.groups} column into normalized entity codes.
 */
public final class UserEntityGroupsParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private UserEntityGroupsParser() {
    }

    public static List<String> parseEntityCodes(String groupsColumn) {
        if (groupsColumn == null || groupsColumn.isBlank()) {
            return List.of();
        }
        String trimmed = groupsColumn.trim();
        List<String> raw;
        if (trimmed.startsWith("[")) {
            raw = parseJsonArray(trimmed);
        } else {
            raw = Arrays.asList(trimmed.split(","));
        }
        return normalize(raw);
    }

    private static List<String> parseJsonArray(String json) {
        try {
            String[] values = MAPPER.readValue(json, String[].class);
            return values == null ? List.of() : Arrays.asList(values);
        } catch (JsonProcessingException ex) {
            return Arrays.asList(json.split(","));
        }
    }

    private static List<String> normalize(List<String> codes) {
        List<String> normalized = new ArrayList<>();
        for (String code : codes) {
            if (code == null) {
                continue;
            }
            String text = code.trim().toUpperCase(Locale.ROOT);
            if (!text.isEmpty() && !normalized.contains(text)) {
                normalized.add(text);
            }
        }
        return List.copyOf(normalized);
    }
}
