package com.customcharge.global_settings.rulesmanager;

import com.customcharge.persistencelayer.cache.CategoryCodesGlobalDataCacheService;
import com.customcharge.persistencelayer.entities.CategoryValue;
import com.customcharge.persistencelayer.entities.GlobalRuleCriteria;
import com.customcharge.persistencelayer.entities.GlobalSetting;
import com.customcharge.persistencelayer.util.ExchangeRateService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class DrlRuleResolutionHelper {
    private DrlRuleResolutionHelper() {
    }

    public static List<GlobalRuleCriteria> buildCriteriaForDrl(
            GlobalSetting setting,
            CategoryCodesGlobalDataCacheService categoryCodesGlobalDataCacheService,
            boolean isGetLabel) {
        List<GlobalRuleCriteria> out = new ArrayList<>();
        if (setting.getRules() != null) {
            for (GlobalRuleCriteria r : setting.getRules()) {
                if (r == null) {
                    continue;
                }
                GlobalRuleCriteria copy = new GlobalRuleCriteria();
                copy.setFieldName(resolveCodeToName(r.getFieldName(), categoryCodesGlobalDataCacheService, isGetLabel));
                copy.setOperator(r.getOperator());
                copy.setValues(resolveValues(r.getValues(), r.getFieldName(), categoryCodesGlobalDataCacheService, isGetLabel));
                copy.setExactValue(normalizeExactValue(r.getExactValue()));
                copy.setCurrency(normalizeCurrency(r.getCurrency()));
                out.add(copy);
            }
        }
        return out;
    }

    private static String resolveValues(
            String values,
            String fieldCode,
            CategoryCodesGlobalDataCacheService categoryCodesGlobalDataCacheService,
            boolean isGetLabel) {
        if (values == null || values.isBlank()) {
            return null;
        }
        List<String> resolved = new ArrayList<>();
        for (String value : values.split(",")) {
            if (value.isBlank()) {
                continue;
            }
            resolved.add(resolveValue(value, fieldCode, categoryCodesGlobalDataCacheService, isGetLabel));
        }
        resolved.sort(String.CASE_INSENSITIVE_ORDER);
        return String.join(",", resolved);
    }

    private static String resolveValue(
            String value,
            String fieldCode,
            CategoryCodesGlobalDataCacheService categoryCodesGlobalDataCacheService,
            boolean isGetLabel) {
        if (!isGetLabel && isCountryOrEntityField(fieldCode, categoryCodesGlobalDataCacheService)) {
            return value.trim();
        }
        return resolveCodeToName(value, categoryCodesGlobalDataCacheService, isGetLabel);
    }

    private static boolean isCountryOrEntityField(
            String fieldCode, CategoryCodesGlobalDataCacheService categoryCodesGlobalDataCacheService) {
        if (fieldCode == null || fieldCode.isBlank()) {
            return false;
        }
        try {
            String loadFrom = categoryCodesGlobalDataCacheService.getCategoryCodeByCode(fieldCode.trim()).getLoadDataFrom();
            if (loadFrom != null) {
                String normalized = loadFrom.trim();
                if ("Country".equalsIgnoreCase(normalized) || "Entity".equalsIgnoreCase(normalized)) {
                    return true;
                }
            }
        } catch (RuntimeException ignored) {
            // fall through to code-name heuristic
        }
        String normalized = fieldCode.toLowerCase();
        return normalized.contains("country") || normalized.contains("entity");
    }

    private static String normalizeCurrency(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return ExchangeRateService.normalizeCurrency(raw);
    }

    private static String normalizeExactValue(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String t = raw.strip();
        if (!t.contains(".")) {
            return t;
        }
        try {
            return new BigDecimal(t).stripTrailingZeros().toPlainString();
        } catch (NumberFormatException | ArithmeticException ex) {
            return t;
        }
    }

    private static String resolveCodeToName(
            String code,
            CategoryCodesGlobalDataCacheService categoryCodesGlobalDataCacheService,
            boolean isGetLabel) {
        if (code == null || code.isBlank()) {
            return code;
        }
        CategoryValue tempCategoryValue = categoryCodesGlobalDataCacheService.getCategoryCodeByCode(code.trim());
        return isGetLabel ? tempCategoryValue.getLabel() : tempCategoryValue.getName();
    }
}
