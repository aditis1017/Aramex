package com.customcharge.dashboard.util;

import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Set;

import static com.customcharge.dashboard.constants.ArBusinessTypeConstants.AR_BUSINESS_TYPE_CORPORATE;
import static com.customcharge.dashboard.constants.ArBusinessTypeConstants.AR_BUSINESS_TYPE_CORPORATE_CODE;
import static com.customcharge.dashboard.constants.ArBusinessTypeConstants.AR_BUSINESS_TYPE_CORPORATE_LEGACY_CODE;
import static com.customcharge.dashboard.constants.ArBusinessTypeConstants.AR_BUSINESS_TYPE_E_COMMERCE_CODE;
import static com.customcharge.dashboard.constants.ArBusinessTypeConstants.AR_BUSINESS_TYPE_OIL_AND_GAS;
import static com.customcharge.dashboard.constants.ArBusinessTypeConstants.AR_BUSINESS_TYPE_OIL_AND_GAS_CODE;
import static com.customcharge.dashboard.constants.ArBusinessTypeConstants.AR_BUSINESS_TYPE_RETAIL_CASH;
import static com.customcharge.dashboard.constants.ArBusinessTypeConstants.AR_BUSINESS_TYPE_RETAIL_CASH_CODE;
import static com.customcharge.dashboard.constants.ArBusinessTypeConstants.AR_BUSINESS_TYPE_WHOLESALE;
import static com.customcharge.dashboard.constants.ArBusinessTypeConstants.AR_BUSINESS_TYPE_WHOLESALE_CODE;
import static com.customcharge.dashboard.constants.ArBusinessTypeConstants.BUSINESS_TYPE_E_COMMERCE;
import static com.customcharge.dashboard.constants.ArBusinessTypeConstants.SHIPMENT_BUSINESS_TYPE_B2B;
import static com.customcharge.dashboard.constants.ArBusinessTypeConstants.SHIPMENT_BUSINESS_TYPE_B2C;

/**
 * Maps AR business segment (display name or category code) to B2B/B2C and resolves effective business type.
 */
public final class ArShipmentBusinessTypeResolver {

    private static final Set<String> B2B_BUSINESS_TYPES = Set.of(
            normalize(AR_BUSINESS_TYPE_CORPORATE),
            normalize(AR_BUSINESS_TYPE_CORPORATE_CODE),
            normalize(AR_BUSINESS_TYPE_CORPORATE_LEGACY_CODE),
            normalize(AR_BUSINESS_TYPE_OIL_AND_GAS),
            normalize(AR_BUSINESS_TYPE_OIL_AND_GAS_CODE),
            normalize(AR_BUSINESS_TYPE_WHOLESALE),
            normalize(AR_BUSINESS_TYPE_WHOLESALE_CODE),
            normalize(AR_BUSINESS_TYPE_RETAIL_CASH),
            normalize(AR_BUSINESS_TYPE_RETAIL_CASH_CODE));


    private static final Set<String> B2C_BUSINESS_TYPES = Set.of(
            normalize(BUSINESS_TYPE_E_COMMERCE),
            normalize(AR_BUSINESS_TYPE_E_COMMERCE_CODE));

    private ArShipmentBusinessTypeResolver() {
    }

    /**
     * Uses explicit {@code businessType} when it is B2B/B2C; otherwise derives from {@code businessSegment}
     * (or legacy {@code businessType} when it still holds a segment value).
     */
    public static String resolveEffective(String businessType, String businessSegment) {
        String segmentSource =
                StringUtils.hasText(businessSegment) ? businessSegment : businessType;
        return resolve(segmentSource);
    }

    public static boolean isExplicitBusinessType(String businessType) {
        if (!StringUtils.hasText(businessType)) {
            return false;
        }
        String normalized = normalize(businessType);
        return normalized.equals(normalize(SHIPMENT_BUSINESS_TYPE_B2B))
                || normalized.equals(normalize(SHIPMENT_BUSINESS_TYPE_B2C));
    }

    public static String resolve(String businessSegment) {
        if (!StringUtils.hasText(businessSegment)) {
            return null;
        }
        String normalized = normalize(businessSegment);
        if (B2B_BUSINESS_TYPES.contains(normalized)) {
            return SHIPMENT_BUSINESS_TYPE_B2B;
        }
        if (B2C_BUSINESS_TYPES.contains(normalized)) {
            return SHIPMENT_BUSINESS_TYPE_B2C;
        }
        return null;
    }

    private static String toCanonicalBusinessType(String businessType) {
        if (normalize(businessType).equals(normalize(SHIPMENT_BUSINESS_TYPE_B2C))) {
            return SHIPMENT_BUSINESS_TYPE_B2C;
        }
        return SHIPMENT_BUSINESS_TYPE_B2B;
    }

    private static String normalize(String value) {
        return value.strip()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\s_-]+", "");
    }
}
