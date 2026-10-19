package com.customcharge.global_settings.rules.service.imp;

import com.customcharge.persistencelayer.dto.GlobalRuleCriteriaDto;
import com.customcharge.persistencelayer.dto.GlobalSettingDto;
import com.customcharge.persistencelayer.entities.GlobalRuleCriteria;
import com.customcharge.persistencelayer.entities.GlobalSetting;
import com.customcharge.persistencelayer.util.ExchangeRateService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class GlobalSettingEntityFactory {

    public GlobalSetting newEntityFromDto(GlobalSettingDto dto) {
        GlobalSetting entity = new GlobalSetting();
        applyHeaderAndRulesFromDto(dto, entity);
        return entity;
    }

    public void applyDtoToEntity(GlobalSettingDto dto, GlobalSetting existing) {
        applyHeaderAndRulesFromDto(dto, existing);
    }

    private void applyHeaderAndRulesFromDto(GlobalSettingDto dto, GlobalSetting entity) {
        entity.setDestination(trimToNull(dto.getDestination()));
        entity.setTargetField(trimToNull(dto.getTargetField()));
        entity.setTargetValue(trimToNull(dto.getTargetValue()));
        entity.setSequenceNumber(dto.getSequenceNumber());

        mergeRulesFromDto(dto, entity);
    }

    private void mergeRulesFromDto(GlobalSettingDto dto, GlobalSetting entity) {
        List<GlobalRuleCriteria> rules = entity.getRules();
        if (dto.getRules() == null || dto.getRules().isEmpty()) {
            rules.clear();
            return;
        }

        Map<UUID, GlobalRuleCriteria> existingById = new HashMap<>();
        for (GlobalRuleCriteria r : rules) {
            if (r.getId() != null) {
                existingById.put(r.getId(), r);
            }
        }

        List<GlobalRuleCriteria> merged = new ArrayList<>();
        for (GlobalRuleCriteriaDto src : dto.getRules()) {
            if (src == null) {
                continue;
            }
            UUID dtoRuleId = src.getId();
            if (dtoRuleId != null && existingById.containsKey(dtoRuleId)) {
                GlobalRuleCriteria rule = existingById.remove(dtoRuleId);
                applyRuleFields(src, rule);
                merged.add(rule);
            } else {
                GlobalRuleCriteria rule = new GlobalRuleCriteria();
                applyRuleFields(src, rule);
                rule.setGlobalSetting(entity);
                rules.add(rule);
                merged.add(rule);
            }
        }

        for (GlobalRuleCriteria orphan : existingById.values()) {
            rules.remove(orphan);
        }

        if (!merged.isEmpty()) {
            boolean aligned = rules.size() == merged.size();
            if (aligned) {
                for (int i = 0; i < merged.size(); i++) {
                    if (rules.get(i) != merged.get(i)) {
                        aligned = false;
                        break;
                    }
                }
            }
            if (!aligned) {
                rules.clear();
                rules.addAll(merged);
            }
        }
    }

    private void applyRuleFields(GlobalRuleCriteriaDto src, GlobalRuleCriteria rule) {
        rule.setFieldName(trimToNull(src.getFieldName()));
        rule.setOperator(src.getOperator());
        rule.setValues(normalizeCsvCodes(src.getValues()));
        rule.setExactValue(src.getExactValue());
        rule.setCurrency(normalizeCurrency(src.getCurrency()));
    }

    private static String normalizeCurrency(String raw) {
        String trimmed = trimToNull(raw);
        if (trimmed == null) {
            return null;
        }
        return ExchangeRateService.normalizeCurrency(trimmed);
    }

    private static String normalizeCsvCodes(String valueCodes) {
        String normalized = trimToNull(valueCodes);
        if (normalized == null) {
            return null;
        }
        String[] split = normalized.split(",");
        StringBuilder out = new StringBuilder();
        for (String part : split) {
            String code = trimToNull(part);
            if (code == null) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(",");
            }
            out.append(code);
        }
        return out.isEmpty() ? null : out.toString();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
