package com.customcharge.dashboard.importfile.support;

import com.customcharge.dashboard.constants.ImportConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ImportColumnUtils {

    private ImportColumnUtils() {
    }

    public static String normalizeHeader(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.strip().toLowerCase(Locale.ROOT);
    }

    public static Optional<String> missingRequiredColumns(List<String> requiredColumns, List<String> headerCells) {
        Set<String> present = new HashSet<>();
        for (String c : headerCells) {
            present.add(normalizeHeader(c));
        }
        List<String> missing = new ArrayList<>();
        for (String req : requiredColumns) {
            if (!present.contains(normalizeHeader(req))) {
                missing.add(req);
            }
        }
        if (missing.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(String.format(
                ImportConstants.MESSAGE_MISSING_REQUIRED_COLUMNS,
                String.join(", ", missing)));
    }

    public static Map<String, String> trimRowValues(Map<String, String> row) {
        Map<String, String> out = HashMap.newHashMap(row.size());
        for (Map.Entry<String, String> e : row.entrySet()) {
            String k = e.getKey();
            String v = e.getValue();
            if (v == null) {
                out.put(k, null);
            } else {
                String t = v.strip();
                out.put(k, t.isEmpty() ? null : t);
            }
        }
        return out;
    }
}
