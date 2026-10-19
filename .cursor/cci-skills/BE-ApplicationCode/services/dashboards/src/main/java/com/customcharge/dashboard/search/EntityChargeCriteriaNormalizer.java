package com.customcharge.dashboard.search;

import com.customcharge.dashboard.constants.EntityChargeFilterConstants;
import com.customcharge.dashboard.constants.PaymentTypeConstants;
import com.customcharge.dashboard.constants.ValidationConstants;
import com.customcharge.dashboard.util.EntityChargeStatusMapper;
import com.customcharge.persistencelayer.cache.CategoryCodesGlobalDataCacheService;
import com.customcharge.persistencelayer.entities.CategoryValue;
import com.customcharge.persistencelayer.exception.BadRequestException;
import com.customcharge.persistencelayer.search.SearchCriteria;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Component
public class EntityChargeCriteriaNormalizer {

    private final CategoryCodesGlobalDataCacheService categoryCodesGlobalDataCacheService;

    public EntityChargeCriteriaNormalizer(CategoryCodesGlobalDataCacheService categoryCodesGlobalDataCacheService) {
        this.categoryCodesGlobalDataCacheService = categoryCodesGlobalDataCacheService;
    }

    public EntityChargeNormalizedFilters normalize(List<SearchCriteria> filters) {
        return normalizeForView(filters);
    }

    /**
     * Maps UI filter field names onto dashboard view properties (same names as filter keys).
     */
    public EntityChargeNormalizedFilters normalizeForView(List<SearchCriteria> filters) {
        return normalize(filters, null);
    }

    private EntityChargeNormalizedFilters normalize(List<SearchCriteria> filters, Map<String, String> fieldPaths) {
        if (filters == null || filters.isEmpty()) {
            return EntityChargeNormalizedFilters.empty();
        }
        List<SearchCriteria> criteria = new ArrayList<>();
        List<String> accountNumbers = new ArrayList<>();
        List<String> accountTypes = new ArrayList<>();
        List<String> boeNumbers = new ArrayList<>();
        for (SearchCriteria filter : filters) {
            if (filter == null) {
                continue;
            }
            if (isAccountNumberField(filter.getFieldName())) {
                accountNumbers.addAll(extractAccountNumbers(filter));
                continue;
            }
            if (isAccountTypeField(filter.getFieldName())) {
                accountTypes.addAll(extractAccountTypes(filter));
                continue;
            }
            if (isBoeField(filter.getFieldName())) {
                boeNumbers.addAll(extractBoeNumbers(filter));
                continue;
            }
            SearchCriteria normalized = normalizeOne(filter, fieldPaths);
            if (normalized != null) {
                criteria.add(normalized);
            }
        }
        return new EntityChargeNormalizedFilters(criteria, accountNumbers, accountTypes, boeNumbers);
    }

    private static boolean isBoeField(String fieldName) {
        return fieldName != null
                && EntityChargeFilterConstants.BOE_FIELD.equals(
                        fieldName.trim().toLowerCase(Locale.ROOT));
    }

    private List<String> extractBoeNumbers(SearchCriteria filter) {
        String condition = normalizeCondition(filter.getCondition());
        if (!EntityChargeFilterConstants.FILTER_CONDITION_IN.equals(condition)) {
            throw new BadRequestException(ValidationConstants.BOE_FILTER_IN_ONLY);
        }
        List<String> values = toStringList(cleanCollectionValue(filter.getValue()));
        return values == null ? List.of() : values;
    }

    private static boolean isAccountNumberField(String fieldName) {
        return fieldName != null
                && EntityChargeFilterConstants.ACCOUNT_NUMBER_FIELD.equals(
                        fieldName.trim().toLowerCase(Locale.ROOT));
    }

    private static boolean isAccountTypeField(String fieldName) {
        return fieldName != null
                && EntityChargeFilterConstants.ACCOUNT_TYPE_FIELD.equals(
                        fieldName.trim().toLowerCase(Locale.ROOT));
    }

    private List<String> extractAccountNumbers(SearchCriteria filter) {
        String condition = normalizeCondition(filter.getCondition());
        if (!EntityChargeFilterConstants.FILTER_CONDITION_IN.equals(condition)) {
            throw new BadRequestException(ValidationConstants.ACCOUNT_NUMBER_FILTER_IN_ONLY);
        }
        List<String> values = toStringList(cleanCollectionValue(filter.getValue()));
        return values == null ? List.of() : values;
    }

    private static List<String> extractAccountTypes(SearchCriteria filter) {
        String condition = normalizeCondition(filter.getCondition());
        if (!EntityChargeFilterConstants.FILTER_CONDITION_IN.equals(condition)) {
            throw new BadRequestException(ValidationConstants.ACCOUNT_TYPE_FILTER_IN_ONLY);
        }
        List<String> values = toStringList(cleanCollectionValue(filter.getValue()));
        if (values.isEmpty()) {
            throw new BadRequestException(ValidationConstants.ACCOUNT_TYPE_FILTER_VALUE_BLANK);
        }
        List<String> accountTypes = new ArrayList<>();
        for (String text : values) {
            if (PaymentTypeConstants.FILTER_CASH.equalsIgnoreCase(text)) {
                accountTypes.add(PaymentTypeConstants.FILTER_CASH);
            } else if (PaymentTypeConstants.FILTER_ACCOUNT.equalsIgnoreCase(text)) {
                accountTypes.add(PaymentTypeConstants.FILTER_ACCOUNT);
            } else {
                throw new BadRequestException(ValidationConstants.ACCOUNT_TYPE_FILTER_VALUES);
            }
        }
        return accountTypes;
    }

    private SearchCriteria normalizeOne(SearchCriteria filter, Map<String, String> fieldPaths) {
        String fieldName = filter.getFieldName();
        if (fieldName == null || fieldName.isBlank()) {
            throw new BadRequestException(ValidationConstants.FILTER_FIELD_NAME_BLANK);
        }

        String key = fieldName.trim().toLowerCase(Locale.ROOT);
        String path;
        if (fieldPaths == null) {
            if (!EntityChargeFilterConstants.VIEW_FILTER_FIELDS.contains(key)) {
                throw new BadRequestException(ValidationConstants.UNKNOWN_FILTER_FIELD_PREFIX + fieldName);
            }
            path = key;
        } else {
            path = fieldPaths.get(key);
            if (path == null) {
                throw new BadRequestException(ValidationConstants.UNKNOWN_FILTER_FIELD_PREFIX + fieldName);
            }
        }

        String condition = normalizeCondition(filter.getCondition());
        Object value = normalizeValue(path, condition, filter.getValue());
        Object secondValue = normalizeDateSecondValue(condition, filter.getSecondValue());

        if (isInactiveCriterion(condition, value, secondValue)) {
            return null;
        }

        SearchCriteria criteria = new SearchCriteria();
        criteria.setFieldName(path);
        criteria.setCondition(condition);
        criteria.setValue(value);
        criteria.setSecondValue(secondValue);
        return criteria;
    }

    private static Object normalizeDateSecondValue(String condition, Object secondValue) {
        if (!EntityChargeFilterConstants.FILTER_CONDITION_BETWEEN_DATE.equals(condition)
                && !EntityChargeFilterConstants.FILTER_CONDITION_BETWEEN_AND_EQUAL_DATE.equals(condition)) {
            return secondValue;
        }
        if (secondValue instanceof String s) {
            String trimmed = s.trim();
            return trimmed.isEmpty() ? null : trimmed;
        }
        return secondValue;
    }

    private static String normalizeCondition(String condition) {
        if (condition == null || condition.isBlank()) {
            throw new BadRequestException(ValidationConstants.FILTER_CONDITION_BLANK);
        }
        return condition.trim().toLowerCase(Locale.ROOT);
    }

    private Object normalizeValue(String path, String condition, Object rawValue) {
        if (EntityChargeFilterConstants.PATH_STATUS.equals(path)
                || EntityChargeFilterConstants.VIEW_PATH_STATUS.equals(path)) {
            return normalizeStatusValue(rawValue);
        }
        if (EntityChargeFilterConstants.PATH_SHIPMENT_PAYMENT_TYPE.equals(path)) {
            return normalizePaymentTypeValue(rawValue);
        }
        if (EntityChargeFilterConstants.PATH_SHIPMENT_BUSINESS_TYPE.equals(path)
                || EntityChargeFilterConstants.VIEW_PATH_BUSINESS_TYPE.equals(path)) {
            return normalizeBusinessTypeValue(condition, rawValue);
        }
        return cleanCollectionValue(rawValue);
    }

    private Object normalizeBusinessTypeValue(String condition, Object rawValue) {
        if (EntityChargeFilterConstants.FILTER_CONDITION_IN.equals(condition)) {
            List<String> codes = toStringList(cleanCollectionValue(rawValue));
            if (codes.isEmpty()) {
                return null;
            }
            List<String> mapped = new ArrayList<>();
            for (String code : codes) {
                String name = mapBusinessTypeToDbName(code);
                if (name != null && !name.isBlank()) {
                    mapped.add(name);
                }
            }
            return mapped.isEmpty() ? null : mapped;
        }
        return mapBusinessTypeToDbName(rawValue);
    }

    private String mapBusinessTypeToDbName(Object value) {
        if (value == null) {
            return null;
        }
        String code = value.toString().trim();
        if (code.isEmpty()) {
            return null;
        }
        CategoryValue categoryValue = categoryCodesGlobalDataCacheService.getCategoryCodeByCode(code);
        return categoryValue.getName();
    }

    private static Object cleanCollectionValue(Object rawValue) {
        if (rawValue instanceof Collection<?> collection) {
            List<Object> cleaned = collection.stream()
                    .filter(Objects::nonNull)
                    .map(v -> v instanceof String s ? s.trim() : v)
                    .filter(v -> !(v instanceof String s) || !s.isEmpty())
                    .toList();
            return cleaned.isEmpty() ? null : cleaned;
        }
        if (rawValue instanceof String s) {
            String trimmed = s.trim();
            return trimmed.isEmpty() ? null : trimmed;
        }
        return rawValue;
    }

    private static List<String> toStringList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .map(Object::toString)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
        String text = value.toString().trim();
        return text.isEmpty() ? List.of() : List.of(text);
    }

    private static Object normalizeStatusValue(Object rawValue) {
        if (rawValue instanceof Collection<?> collection) {
            List<String> mapped = new ArrayList<>();
            for (Object item : collection) {
                String db = mapStatusToDb(item);
                if (db != null) {
                    mapped.add(db);
                }
            }
            return mapped.isEmpty() ? null : mapped;
        }
        String db = mapStatusToDb(rawValue);
        return db;
    }

    private static String mapStatusToDb(Object value) {
        return EntityChargeStatusMapper.toDatabase(value);
    }

    private static Object normalizePaymentTypeValue(Object rawValue) {
        if (rawValue instanceof Collection<?> collection) {
            List<String> mapped = new ArrayList<>();
            for (Object item : collection) {
                String db = mapPaymentTypeToDb(item);
                if (db != null && !db.isEmpty()) {
                    mapped.add(db);
                }
            }
            return mapped.isEmpty() ? null : mapped;
        }
        return mapPaymentTypeToDb(rawValue);
    }

    private static String mapPaymentTypeToDb(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            return null;
        }
        return EntityChargeFilterConstants.PAYMENT_TYPE_DISPLAY_TO_DB.getOrDefault(
                text.toLowerCase(Locale.ROOT), text);
    }

    private static boolean isInactiveCriterion(String condition, Object value, Object secondValue) {
        if (EntityChargeFilterConstants.FILTER_CONDITION_BETWEEN_DATE.equals(condition)
                || EntityChargeFilterConstants.FILTER_CONDITION_BETWEEN_AND_EQUAL_DATE.equals(condition)) {
            return isBlankDateValue(value) || isBlankDateValue(secondValue);
        }
        if (value == null) {
            return true;
        }
        if (value instanceof Collection<?> collection) {
            return collection.isEmpty();
        }
        if (EntityChargeFilterConstants.FILTER_CONDITION_IN.equals(condition)
                || EntityChargeFilterConstants.FILTER_CONDITION_NOT_IN.equals(condition)) {
            if (value instanceof Collection<?> collection) {
                return collection.isEmpty();
            }
        }
        return false;
    }

    private static boolean isBlankDateValue(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String s) {
            return s.isBlank();
        }
        return false;
    }
}
