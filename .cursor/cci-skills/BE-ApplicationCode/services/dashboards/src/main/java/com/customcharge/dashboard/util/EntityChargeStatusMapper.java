package com.customcharge.dashboard.util;

import com.customcharge.dashboard.constants.EntityChargeStatusConstants;

import java.util.Locale;
import java.util.Map;

/**
 * Maps entity charge status values between database codes and UI display labels.
 */
public final class EntityChargeStatusMapper {

    private static final Map<String, String> DB_TO_DISPLAY = Map.of(
            EntityChargeStatusConstants.PENDING_CALCULATION,
                    EntityChargeStatusConstants.DISPLAY_PENDING_CALCULATION,
            EntityChargeStatusConstants.DUE,
                    EntityChargeStatusConstants.DISPLAY_DUE,
            EntityChargeStatusConstants.ACTUALIZED,
                    EntityChargeStatusConstants.DISPLAY_ACTUALIZED,
            EntityChargeStatusConstants.SETTLED,
                    EntityChargeStatusConstants.DISPLAY_SETTLED,
//            "NOT_DUE", "Not Due",
//            "NON_BILLABLE", "Non Billable",
            EntityChargeStatusConstants.EXEMPTED,
                    EntityChargeStatusConstants.DISPLAY_EXEMPTED
    );

    private static final Map<String, String> DISPLAY_TO_DB = Map.of(
            EntityChargeStatusConstants.KEY_PENDING_CALCULATION,
                    EntityChargeStatusConstants.PENDING_CALCULATION,
            EntityChargeStatusConstants.KEY_DUE,
                    EntityChargeStatusConstants.DUE,
            EntityChargeStatusConstants.KEY_ACTUALIZED,
                    EntityChargeStatusConstants.ACTUALIZED,
            EntityChargeStatusConstants.KEY_SETTLED,
                    EntityChargeStatusConstants.SETTLED,
//            "not due", "NOT_DUE",
//            "non billable", "NON_BILLABLE",
//            "non_billable", "NON_BILLABLE",
            EntityChargeStatusConstants.KEY_EXEMPTED,
                    EntityChargeStatusConstants.DISPLAY_EXEMPTED
    );

    private EntityChargeStatusMapper() {
    }

    public static String toDisplay(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String key = status.trim().toUpperCase(Locale.ROOT);
        return DB_TO_DISPLAY.getOrDefault(key, humanizeStatus(key));
    }

    public static String toDatabase(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            return null;
        }
        String mapped = DISPLAY_TO_DB.get(text.toLowerCase(Locale.ROOT));
        if (mapped != null) {
            return mapped;
        }
        return text.toUpperCase(Locale.ROOT);//.replace(' ', '_');
    }

    private static String humanizeStatus(String status) {
        String[] parts = status.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                sb.append(part.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return sb.toString();
    }
}
