package com.customcharge.global_settings.support;

import com.customcharge.persistencelayer.dto.GlobalRuleCriteriaDto;
import com.customcharge.persistencelayer.dto.GlobalSettingDto;
import com.customcharge.persistencelayer.entities.CategoryValue;
import com.customcharge.persistencelayer.entities.GlobalRuleCriteria;
import com.customcharge.persistencelayer.entities.GlobalSetting;
import com.customcharge.persistencelayer.entities.User;
import com.customcharge.persistencelayer.enums.Operator;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class GlobalSettingsTestDataFactory {

    private GlobalSettingsTestDataFactory() {
    }

    public static CategoryValue categoryValue(String code, String name, String label) {
        CategoryValue value = new CategoryValue();
        value.setCode(code);
        value.setName(name);
        value.setLabel(label);
        return value;
    }

    public static GlobalSetting globalSetting(String destination, String targetValue, String fieldCode, String valueCode) {
        GlobalRuleCriteria criteria = new GlobalRuleCriteria();
        criteria.setFieldName(fieldCode);
        criteria.setOperator(Operator.IN);
        criteria.setValues(valueCode);

        GlobalSetting setting = new GlobalSetting();
        ReflectionTestUtils.setField(setting, "id", UUID.randomUUID());
        setting.setDestination(destination);
        setting.setTargetValue(targetValue);
        setting.setTargetField("shipmentType");
        setting.setRuleSignature(UUID.randomUUID().toString());
        setting.setStatus(Boolean.TRUE);
        setting.setSequenceNumber(1);
        setting.setDrlRule("rule-content");
        setting.setRules(new ArrayList<>(List.of(criteria)));
        return setting;
    }

    public static GlobalSettingDto globalSettingDto(String destination, String targetValue) {
        GlobalSettingDto dto = new GlobalSettingDto();
        dto.setDestination(destination);
        dto.setTargetValue(targetValue);
        dto.setTargetField("shipmentType");
        GlobalRuleCriteriaDto rule = new GlobalRuleCriteriaDto();
        rule.setFieldName("originCountry");
        rule.setOperator(Operator.EQUALS);
        rule.setValues("JO");
        dto.setRules(List.of(rule));
        return dto;
    }

    public static User user(String email, String username) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setActive(true);
        return user;
    }
}
