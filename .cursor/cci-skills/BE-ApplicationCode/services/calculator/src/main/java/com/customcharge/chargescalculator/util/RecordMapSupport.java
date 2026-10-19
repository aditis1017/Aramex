package com.customcharge.chargescalculator.util;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class RecordMapSupport {
    private RecordMapSupport() {
    }

    public static Map<String, String> lowercaseKeys(Map<String, String> recordData) {
        if (recordData == null || recordData.isEmpty()) {
            return Map.of();
        }
        Map<String, String> out = HashMap.newHashMap(recordData.size());
        for (Map.Entry<String, String> e : recordData.entrySet()) {
            String key = e.getKey();
            if (key != null) {
                out.put(key.toLowerCase(Locale.ROOT), e.getValue());
            }
        }
        return out;
    }
}
