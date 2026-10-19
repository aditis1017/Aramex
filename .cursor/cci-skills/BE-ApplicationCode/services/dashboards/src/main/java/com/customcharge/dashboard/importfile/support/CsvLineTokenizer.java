package com.customcharge.dashboard.importfile.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CsvLineTokenizer {

    private CsvLineTokenizer() {
    }

    public static List<String> splitLine(String line) {
        return splitLine(line, true);
    }

    public static List<String> splitLine(String line, boolean trimCellValues) {
        if (line == null || line.isEmpty()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                out.add(normalizeToken(cur.toString(), trimCellValues));
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(normalizeToken(cur.toString(), trimCellValues));
        return Collections.unmodifiableList(out);
    }

    private static String normalizeToken(String token, boolean trimCellValues) {
        return trimCellValues ? token.strip() : token;
    }
}
