package com.customcharge.shipment.service.rules;

import static com.customcharge.shipment.constants.RuleCacheConstants.DTO_PATTERN;
import static com.customcharge.shipment.constants.RuleCacheConstants.ENTITY_PATTERN;
import static com.customcharge.shipment.constants.RuleCacheConstants.GLOBAL_SETTINGS_FACT_CLASS;
import static com.customcharge.shipment.constants.RuleCacheConstants.RULE_KEY_PREFIX;
import static com.customcharge.shipment.constants.RuleCacheConstants.SET_CATEGORY_TYPE;
import static com.customcharge.shipment.constants.RuleCacheConstants.SET_CATEGORY_TYPE_CODE;
import static com.customcharge.shipment.constants.RuleCacheConstants.SET_IS_SHIPMENT_FULL;
import static com.customcharge.shipment.constants.RuleCacheConstants.SET_SHIPMENT_TYPE;
import static com.customcharge.shipment.constants.RuleCacheConstants.SHIPMENT_DTO_FACT_CLASS;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Helpers aligned with global-settings Redis keys and DRL fact typing.
 */
public final class GlobalSettingRuleCacheSupport {

    private static final ConcurrentHashMap<String, String> ADAPTED_DRL_CACHE = new ConcurrentHashMap<>();

    private GlobalSettingRuleCacheSupport() {
    }

    public static String buildRuleDrlMapCacheKey(String destinationCode, String targetCode) {
        return RULE_KEY_PREFIX + normalizeKeyPart(destinationCode) + ":" + normalizeKeyPart(targetCode);
    }

    public static String normalizeKeyPart(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", "_");
    }

    public static String adaptCached(String adapterId, String drl, Function<String, String> adapter) {
        if (drl == null || drl.isBlank()) {
            return drl;
        }
        if (adapter == null) {
            return drl;
        }
        String cacheKey = adapterId + '\0' + drl;
        return ADAPTED_DRL_CACHE.computeIfAbsent(cacheKey, ignored -> adapter.apply(drl));
    }

    public static String adaptDrlFactToShipmentDto(String drl) {
        if (drl == null || drl.isBlank()) {
            return drl;
        }
        if (drl.contains(SHIPMENT_DTO_FACT_CLASS)) {
            return drl;
        }
        String adapted = drl.replace(GLOBAL_SETTINGS_FACT_CLASS, SHIPMENT_DTO_FACT_CLASS);
        adapted = adapted.replace(ENTITY_PATTERN, DTO_PATTERN);
        return adapted;
    }

    public static String adaptDrlFactToShipmentDtoCached(String drl) {
        return adaptCached("shipmentDto", drl, GlobalSettingRuleCacheSupport::adaptDrlFactToShipmentDto);
    }

    /**
     * Adapts cached global-settings DRL for shipment-fullness evaluation on {@link ShipmentDto}.
     * Existing rules may call {@code setCategoryType(...)}; those are rewritten to
     * {@code setIsShipmentFull(true)} at runtime so no global-settings redeploy is required.
     */
    public static String adaptDrlForShipmentFullness(String drl) {
        String adapted = adaptDrlFactToShipmentDto(drl);
        adapted = adapted.replaceAll(setterCallWithQuotedArgPattern(SET_CATEGORY_TYPE), SET_IS_SHIPMENT_FULL);
        adapted = adapted.replaceAll(setterCallWithQuotedArgPattern(SET_CATEGORY_TYPE_CODE), SET_IS_SHIPMENT_FULL);
        adapted = adapted.replaceAll(setterCallWithQuotedArgPattern(SET_SHIPMENT_TYPE), SET_IS_SHIPMENT_FULL);
        return adapted;
    }

    public static String adaptDrlForShipmentFullnessCached(String drl) {
        return adaptCached("fullness", drl, GlobalSettingRuleCacheSupport::adaptDrlForShipmentFullness);
    }

    private static String setterCallWithQuotedArgPattern(String setterPrefix) {
        return Pattern.quote(setterPrefix) + "\"[^\"]*\"\\)";
    }

    public static String adaptDrlForCategoryOnShipmentMessageDto(String drl) {
        String adapted = adaptDrlFactToShipmentDto(drl);
        adapted = adapted.replace(SET_CATEGORY_TYPE_CODE, SET_CATEGORY_TYPE);
        adapted = adapted.replace(SET_SHIPMENT_TYPE, SET_CATEGORY_TYPE);
        return adapted;
    }

    public static String adaptDrlForCategoryOnShipmentMessageDtoCached(String drl) {
        return adaptCached("category", drl, GlobalSettingRuleCacheSupport::adaptDrlForCategoryOnShipmentMessageDto);
    }
}
