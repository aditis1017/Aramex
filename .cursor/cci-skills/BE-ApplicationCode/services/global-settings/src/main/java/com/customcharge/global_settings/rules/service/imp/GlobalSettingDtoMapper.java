package com.customcharge.global_settings.rules.service.imp;

import com.customcharge.persistencelayer.cache.CategoryCodesGlobalDataCacheService;
import com.customcharge.persistencelayer.dto.GlobalRuleCriteriaDto;
import com.customcharge.persistencelayer.dto.GlobalSettingDto;
import com.customcharge.persistencelayer.entities.GlobalRuleCriteria;
import com.customcharge.persistencelayer.entities.GlobalSetting;
import com.customcharge.persistencelayer.enums.Operator;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class GlobalSettingDtoMapper {

    private final CategoryCodesGlobalDataCacheService categoryCodesGlobalDataCacheService;

    public GlobalSettingDtoMapper(CategoryCodesGlobalDataCacheService categoryCodesGlobalDataCacheService) {
        this.categoryCodesGlobalDataCacheService = categoryCodesGlobalDataCacheService;
    }

    public GlobalSettingDto toDtoCodeOnly(GlobalSetting entity) {
        if (entity == null) {
            return null;
        }
        GlobalSettingDto dto = baseDto(entity);
        dto.setDestination(entity.getDestination());
        dto.setTargetField(entity.getTargetField());
        dto.setTargetValue(entity.getTargetValue());
        dto.setRules(copyRulesAsCodes(entity.getRules()));
        return dto;
    }

    public GlobalSettingDto toDtoWithResolvedValues(GlobalSetting entity) {
        if (entity == null) {
            return null;
        }
        GlobalSettingDto dto = baseDto(entity);
        dto.setDestination(resolveCodeToValue(entity.getDestination()));
        dto.setTargetField(entity.getTargetField());
        dto.setTargetValue(resolveCodeToValue(entity.getTargetValue()));
        dto.setRules(copyRulesWithResolvedValues(entity.getRules()));
        return dto;
    }

    private static GlobalSettingDto baseDto(GlobalSetting entity) {
        GlobalSettingDto dto = new GlobalSettingDto();
        dto.setId(entity.getId());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setCreatedTime(entity.getCreatedTime());
        dto.setUpdatedTime(entity.getUpdatedTime());
        dto.setDrlRule(entity.getDrlRule());
        dto.setStatus(entity.getStatus());
        dto.setSequenceNumber(entity.getSequenceNumber());
        dto.setEditable(isEditableWithin24Hours(entity.getCreatedTime()));
        return dto;
    }

    private static Boolean isEditableWithin24Hours(Instant createdTime) {
        if (createdTime == null) {
            return Boolean.FALSE;
        }
        Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);
        return createdTime.isAfter(cutoff);
    }

    private String normalizedCsv(String refs) {
        if (refs == null || refs.isBlank()) {
            return null;
        }
        return Arrays.stream(refs.split(","))
                .map(String::trim)
                .filter(v -> !v.isBlank())
                .collect(Collectors.joining(","));
    }

    private List<GlobalRuleCriteriaDto> copyRulesAsCodes(List<GlobalRuleCriteria> rules) {
        if (rules == null) {
            return Collections.emptyList();
        }
        return rules.stream()
                .filter(Objects::nonNull)
                .map(this::toRuleDtoCodes)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private List<GlobalRuleCriteriaDto> copyRulesWithResolvedValues(List<GlobalRuleCriteria> rules) {
        if (rules == null) {
            return Collections.emptyList();
        }
        return rules.stream()
                .filter(Objects::nonNull)
                .map(this::toRuleDtoValues)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private GlobalRuleCriteriaDto toRuleDtoCodes(GlobalRuleCriteria rule) {
        GlobalRuleCriteriaDto dto = new GlobalRuleCriteriaDto();
        dto.setId(rule.getId());
        dto.setFieldName(rule.getFieldName());
        dto.setOperator(rule.getOperator());
        dto.setValues(normalizedCsv(rule.getValues()));
        dto.setExactValue(rule.getExactValue());
        dto.setCurrency(rule.getCurrency());
        dto.setCreatedBy(rule.getCreatedBy());
        dto.setUpdatedBy(rule.getUpdatedBy());
        dto.setCreatedTime(rule.getCreatedTime());
        dto.setUpdatedTime(rule.getUpdatedTime());
        return dto;
    }

    private GlobalRuleCriteriaDto toRuleDtoValues(GlobalRuleCriteria rule) {
        GlobalRuleCriteriaDto dto = toRuleDtoCodes(rule);
        String fieldCode = rule.getFieldName();
        dto.setFieldName(resolveCodeToValue(dto.getFieldName()));
        if (dto.getOperator() != null) {
            dto.setOperatorValue(dto.getOperator().getValue());
        }
        if (dto.getValues() != null && !dto.getValues().isBlank()) {
            dto.setValues(Arrays.stream(dto.getValues().split(","))
                    .map(String::trim)
                    .filter(v -> !v.isBlank())
                    .map(this::resolveCodeToValue)
                    .collect(Collectors.joining(",")));
        } else if (usesExactValueForDisplay(dto.getOperator())
                && resolvesExactValueFromCategory(fieldCode)
                && dto.getExactValue() != null
                && !dto.getExactValue().isBlank()) {
            dto.setValues(resolveCodeToValue(dto.getExactValue()));
        }
        return dto;
    }

    private static boolean usesExactValueForDisplay(Operator operator) {
        return operator == Operator.EQUALS
                || operator == Operator.NOT_EQUALS
                || operator == Operator.CONTAINS;
    }

    private static boolean resolvesExactValueFromCategory(String fieldCode) {
        if (fieldCode == null || fieldCode.isBlank()) {
            return false;
        }
        String normalized = fieldCode.trim();
        return "G_BUSINESS_TYPE".equalsIgnoreCase(normalized)
                || "G_FRDM_FLAG".equalsIgnoreCase(normalized);
    }

    private String resolveCodeToValue(String code) {
        if (code == null || code.isBlank()) {
            return code;
        }
        return categoryCodesGlobalDataCacheService.getCategoryCodeByCode(code.trim()).getLabel();
    }
}
