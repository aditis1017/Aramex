package com.customcharge.persistencelayer.cache;

import java.util.Optional;
import java.util.StringJoiner;

public final class MasterDataCacheKeys {

    public static final String LEGACY_GLOBAL_SETTING_PATTERN = "Global_Setting:*";
    public static final String CATEGORY_TREE_PATTERN = "master:category:*";
    public static final String CATEGORY_VALUE_TREE_PATTERN = "master:category_value:*";
    public static final String CURRENCY_PATTERN = "currency:*";

    public static final String SYNTHETIC_COUNTRY_CATEGORY_CODE = "Country";
    public static final String SYNTHETIC_ENTITY_CATEGORY_CODE = "Entity";

    private MasterDataCacheKeys() {
    }

    public static Optional<String> syntheticCategoryCode(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        String t = name.trim();
        if (SYNTHETIC_COUNTRY_CATEGORY_CODE.equalsIgnoreCase(t)) {
            return Optional.of(SYNTHETIC_COUNTRY_CATEGORY_CODE);
        }
        if (SYNTHETIC_ENTITY_CATEGORY_CODE.equalsIgnoreCase(t)) {
            return Optional.of(SYNTHETIC_ENTITY_CATEGORY_CODE);
        }
        return Optional.empty();
    }

    public static String categoryAll() {
        return "master:category:all";
    }

    public static String category(String categoryCode) {
        return "master:category:" + categoryCode;
    }

    public static String categoryValues(String categoryCode) {
        return "master:category:" + categoryCode + ":values";
    }

    public static String categoryValuesClosure(String categoryCode) {
        return "master:category:" + categoryCode + ":values:closure";
    }

    public static String categoryLoadDataFromMap(String categoryCode) {
        return "master:category:" + categoryCode + ":values:load_from";
    }

    public static String categoryTree(String categoryCode) {
        return "master:category:" + categoryCode + ":tree";
    }

    public static String categoryTreePath(String categoryCode, java.util.List<String> path) {
        StringJoiner joiner = new StringJoiner("/");
        for (String p : path) {
            if (p != null && !p.isBlank()) {
                joiner.add(p.trim());
            }
        }
        return "master:category:" + categoryCode + ":tree:path:" + joiner;
    }

    public static String categoryEntityValues(String categoryCode) {
        return "master:category:" + categoryCode + ":values:entity";
    }

    public static String categoryValueByCode(String valueCode) {
        return "master:category_value:" + valueCode;
    }

    public static String entityDecimalPrecision(String entityCode) {
        return "entity:precision:" + entityCode.strip().toUpperCase(java.util.Locale.ROOT);
    }

    public static String currencyByCountry(String countryCode) {
        return "currency:country:" + countryCode.trim().toUpperCase(java.util.Locale.ROOT);
    }

    public static String currencyByEntity(String entityCode) {
        return "currency:entity:" + entityCode.trim().toUpperCase(java.util.Locale.ROOT);
    }
}
