package com.customcharge.dashboard.service;

import com.customcharge.dashboard.constants.RateSheetConstants;
import com.customcharge.dashboard.constants.RateSheetItemKeyConstants;

import java.util.Locale;

/**
 * Builds a case-insensitive identity key for rate sheet item duplicate checks
 * (service code, shipment type, business type, and category type).
 */
public final class RateSheetItemKeyUtil {

    private RateSheetItemKeyUtil() {
    }

    public static String itemKey(
            String serviceCode,
            String shipmentType,
            String businessType,
            String categoryType) {
        String codeKey = normalizeKeyPart(serviceCode);
        String shipmentTypeKey = normalizeKeyPart(shipmentType);
        String businessTypeKey = normalizeKeyPart(businessType);
        String categoryTypeKey = normalizeKeyPart(categoryType);
        return codeKey
                + RateSheetItemKeyConstants.KEY_SEPARATOR
                + shipmentTypeKey
                + RateSheetItemKeyConstants.KEY_SEPARATOR
                + businessTypeKey
                + RateSheetItemKeyConstants.KEY_SEPARATOR
                + categoryTypeKey;
    }

    public static boolean isAll(String value) {
        return value != null && RateSheetConstants.VALUE_ALL.equalsIgnoreCase(value.strip());
    }

    public static boolean isCatchAll(String shipmentType, String businessType, String categoryType) {
        return isAll(shipmentType) && isAll(businessType) && isAll(categoryType);
    }

    /**
     * Returns true when two values on the same dimension could match the same shipment
     * (equal values or either side is ALL).
     */
    public static boolean dimensionValuesOverlap(String left, String right) {
        if (isAll(left) || isAll(right)) {
            return true;
        }
        return normalizeKeyPart(left).equals(normalizeKeyPart(right));
    }

    /**
     * Returns true when two shipment/business/category configurations could match the same shipment.
     */
    public static boolean configurationsOverlap(
            String shipmentType1,
            String businessType1,
            String categoryType1,
            String shipmentType2,
            String businessType2,
            String categoryType2) {
        return dimensionValuesOverlap(shipmentType1, shipmentType2)
                && dimensionValuesOverlap(businessType1, businessType2)
                && dimensionValuesOverlap(categoryType1, categoryType2);
    }

    private static String normalizeKeyPart(String value) {
        return value == null ? "" : value.strip().toUpperCase(Locale.ROOT);
    }
}
