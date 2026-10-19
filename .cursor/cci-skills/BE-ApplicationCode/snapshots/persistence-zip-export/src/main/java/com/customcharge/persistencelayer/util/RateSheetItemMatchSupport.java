package com.customcharge.persistencelayer.util;

import com.customcharge.persistencelayer.entities.RateSheetItem;
import com.customcharge.persistencelayer.entities.Shipment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Shared rate sheet item matching for charge lookup. Shipment, business, and category type
 * support ALL and legacy NULL/blank as wildcards. When multiple rows match the same service code,
 * the most specific row wins (specific values over ALL).
 */
public final class RateSheetItemMatchSupport {

    public static final String VALUE_ALL = "ALL";

    private RateSheetItemMatchSupport() {
    }

    public static boolean matchesRateSheetItem(RateSheetItem item, Shipment shipment) {
        return matchesRateSheetItem(item, shipment, null);
    }

    public static boolean matchesRateSheetItem(
            RateSheetItem item, Shipment shipment, String categoryTypeOverride) {
        if (shipment == null) {
            return true;
        }
        return matchesShipmentType(item, shipment)
                && matchesBusinessType(item, shipment)
                && matchesCategoryType(item, shipment, categoryTypeOverride);
    }

    public static boolean matchesShipmentType(RateSheetItem item, Shipment shipment) {
        return wildcardFieldMatch(item.getShipmentType(), shipment.getShipmentType());
    }

    public static boolean matchesBusinessType(RateSheetItem item, Shipment shipment) {
        return wildcardFieldMatch(item.getBusinessType(), shipment.getShipmentBusinessType());
    }

    public static boolean matchesCategoryType(RateSheetItem item, Shipment shipment) {
        return matchesCategoryType(item, shipment, null);
    }

    public static boolean matchesCategoryType(
            RateSheetItem item, Shipment shipment, String categoryTypeOverride) {
        String category = categoryTypeOverride != null && !categoryTypeOverride.isBlank()
                ? categoryTypeOverride.strip()
                : shipment.getCategoryType();
        return wildcardFieldMatch(item.getCategoryType(), category);
    }

    /**
     * Returns how many of shipment/business/category type are specific (not ALL/blank).
     * Higher score means a better match when multiple rows share a service code.
     */
    public static int specificityScore(RateSheetItem item) {
        if (item == null) {
            return 0;
        }
        int score = 0;
        if (isSpecificValue(item.getShipmentType())) {
            score++;
        }
        if (isSpecificValue(item.getBusinessType())) {
            score++;
        }
        if (isSpecificValue(item.getCategoryType())) {
            score++;
        }
        return score;
    }

    /**
     * Keeps one row per service code, preferring the most specific matching configuration.
     */
    public static List<RateSheetItem> selectMostSpecificPerServiceCode(List<RateSheetItem> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        Map<String, RateSheetItem> bestByServiceCode = new LinkedHashMap<>();
        for (RateSheetItem item : items) {
            String serviceCode = resolveServiceCode(item);
            if (serviceCode.isEmpty()) {
                continue;
            }
            bestByServiceCode.merge(serviceCode, item, RateSheetItemMatchSupport::preferMoreSpecific);
        }
        return List.copyOf(bestByServiceCode.values());
    }

    public static String resolveServiceCode(RateSheetItem item) {
        if (item == null) {
            return "";
        }
        if (item.getServiceCode() != null && !item.getServiceCode().isBlank()) {
            return item.getServiceCode().strip().toUpperCase(Locale.ROOT);
        }
        if (item.getDescription() != null && !item.getDescription().isBlank()) {
            return item.getDescription().strip().toUpperCase(Locale.ROOT);
        }
        return "";
    }

    public static boolean isCatchAll(RateSheetItem item) {
        if (item == null) {
            return false;
        }
        return isAllValue(item.getShipmentType())
                && isAllValue(item.getBusinessType())
                && isAllValue(item.getCategoryType());
    }

    private static RateSheetItem preferMoreSpecific(RateSheetItem existing, RateSheetItem candidate) {
        int existingScore = specificityScore(existing);
        int candidateScore = specificityScore(candidate);
        if (candidateScore > existingScore) {
            return candidate;
        }
        return existing;
    }

    private static boolean wildcardFieldMatch(String itemValue, String shipmentValue) {
        if (itemValue == null || itemValue.isBlank()) {
            return true;
        }
        String normalizedItem = itemValue.strip();
        if (isAllValue(normalizedItem)) {
            return true;
        }
        return normalizedItem.equalsIgnoreCase(normalize(shipmentValue));
    }

    private static boolean isSpecificValue(String value) {
        return value != null && !value.isBlank() && !isAllValue(value);
    }

    private static boolean isAllValue(String value) {
        return value != null && VALUE_ALL.equalsIgnoreCase(value.strip());
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
