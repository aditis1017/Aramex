package com.customcharge.global_settings.rules.service.imp;


import com.customcharge.global_settings.rules.constent.ValidationConstant;
import com.customcharge.persistencelayer.dto.GlobalRuleCriteriaDto;
import com.customcharge.persistencelayer.dto.GlobalSettingDto;
import com.customcharge.persistencelayer.entities.GlobalSetting;
import com.customcharge.persistencelayer.enums.Operator;
import com.customcharge.persistencelayer.exception.BadRequestException;
import com.customcharge.persistencelayer.exception.ConflictException;
import com.customcharge.persistencelayer.exception.ErrorCode;
import com.customcharge.persistencelayer.exception.ValidationException;
import com.customcharge.global_settings.rules.repository.GlobalSettingRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class GlobalSettingValidator {

    private final GlobalSettingRepository repository;

    public GlobalSettingValidator(GlobalSettingRepository repository) {
        this.repository = repository;
    }

    public static List<String> resolveTargetValuesForScope(String targetValue) {
        if (targetValue == null || targetValue.isBlank()) {
            throw new BadRequestException("Target Value is required.");
        }
        String t = targetValue.trim();
        if (t.equalsIgnoreCase("Category") || t.equalsIgnoreCase("Catogoery")
                || List.of("HV", "LV", "B2B", "Exempted").contains(t)) {
            return List.of("HV", "LV", "B2B", "Exempted");
        }
        if(t.equalsIgnoreCase("ddu")){
            return List.of("DDU","DDO");
        }
        return List.of(t.toUpperCase());
    }

    public void validateRuleCriteria(GlobalSettingDto dto) {
        if (dto == null || dto.getRules() == null) {
            return;
        }
        for (GlobalRuleCriteriaDto rule : dto.getRules()) {
            if (rule == null) {
                continue;
            }
            if (rule.getOperator() == Operator.IN
                    && countNonBlankCsvValues(rule.getValues()) <= 1) {
                throw new ValidationException(ValidationConstant.IN_OPERATOR_REQUIRES_MULTIPLE_VALUES);
            }
            if (rule.getOperator() == Operator.NOT_IN
                    && countNonBlankCsvValues(rule.getValues()) <= 1) {
                throw new ValidationException(ValidationConstant.NOT_IN_OPERATOR_REQUIRES_MULTIPLE_VALUES);
            }
        }
    }

    public void validateDuplicateForCreate(GlobalSetting entity) {
        List<String> targetScope = resolveTargetValuesForScope(entity.getTargetValue());
        List<GlobalSetting> conflicts = repository.findByDestinationAndRuleConditionAndTargetValueIn(
                entity.getDestination(),
                entity.getRuleCondition(),
                targetScope
        );
        throwIfConflicts(conflicts);
    }

    public void validateDuplicateForUpdate(UUID id, GlobalSetting entity) {
        List<String> targetScope = resolveTargetValuesForScope(entity.getTargetValue());
        List<GlobalSetting> conflicts = repository.findByDestinationAndRuleConditionAndTargetValueInAndIdNot(
                entity.getDestination(),
                entity.getRuleCondition(),
                targetScope,
                id
        );
        throwIfConflicts(conflicts);
    }

    private static void throwIfConflicts(List<GlobalSetting> conflicts) {
        if (conflicts == null || conflicts.isEmpty()) {
            return;
        }
        boolean anyActive = conflicts.stream().anyMatch(c -> Boolean.TRUE.equals(c.getStatus()));
        String message =(anyActive ? ValidationConstant.DUPLICATE_GLOBAL_SETTING_ACTIVE
                : ValidationConstant.DUPLICATE_GLOBAL_SETTING_INACTIVE);
        throw new ConflictException(ErrorCode.DUPLICATE_GLOBAL_SETTING, message);
    }

    private static int countNonBlankCsvValues(String values) {
        if (values == null || values.isBlank()) {
            return 0;
        }
        int count = 0;
        for (String part : values.split(",")) {
            if (part != null && !part.isBlank()) {
                count++;
            }
        }
        return count;
    }

}
