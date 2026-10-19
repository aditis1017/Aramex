package com.customcharge.global_settings.rules.service.imp;

import com.customcharge.global_settings.support.GlobalSettingsTestDataFactory;
import com.customcharge.persistencelayer.cache.CategoryCodesGlobalDataCacheService;
import com.customcharge.persistencelayer.dto.GlobalRuleCriteriaDto;
import com.customcharge.persistencelayer.dto.GlobalSettingDto;
import com.customcharge.persistencelayer.entities.GlobalRuleCriteria;
import com.customcharge.persistencelayer.entities.GlobalSetting;
import com.customcharge.persistencelayer.enums.Operator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class GlobalSettingEntityFactoryTest {

    @Mock
    private CategoryCodesGlobalDataCacheService categoryCodesGlobalDataCacheService;

    @InjectMocks
    private GlobalSettingEntityFactory factory;

    @Test
    void newEntityFromDto_shouldCreateEntityWithRules() {
        GlobalSettingDto dto = GlobalSettingsTestDataFactory.globalSettingDto("DXB", "DDU");

        GlobalSetting entity = factory.newEntityFromDto(dto);

        assertThat(entity.getDestination()).isEqualTo("DXB");
        assertThat(entity.getTargetValue()).isEqualTo("DDU");
        assertThat(entity.getRules()).hasSize(1);
    }

    @Test
    void applyDtoToEntity_shouldClearRules_whenDtoRulesEmpty() {
        GlobalSetting existing = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "f", "v");
        GlobalSettingDto dto = new GlobalSettingDto();
        dto.setDestination("AMM");
        dto.setRules(List.of());

        factory.applyDtoToEntity(dto, existing);

        assertThat(existing.getDestination()).isEqualTo("AMM");
        assertThat(existing.getRules()).isEmpty();
    }

    @Test
    void applyDtoToEntity_shouldUpdateExistingRuleById() {
        UUID ruleId = UUID.randomUUID();
        GlobalSetting existing = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "f", "v");
        GlobalRuleCriteria existingRule = existing.getRules().getFirst();
        ReflectionTestUtils.setField(existingRule, "id", ruleId);

        GlobalRuleCriteriaDto dtoRule = new GlobalRuleCriteriaDto();
        dtoRule.setId(ruleId);
        dtoRule.setFieldName("updatedField");
        dtoRule.setOperator(Operator.CONTAINS);
        dtoRule.setValues("A,B");

        GlobalSettingDto dto = new GlobalSettingDto();
        dto.setDestination("DXB");
        dto.setTargetValue("DDU");
        dto.setRules(List.of(dtoRule));

        factory.applyDtoToEntity(dto, existing);

        assertThat(existing.getRules()).hasSize(1);
        assertThat(existing.getRules().getFirst().getFieldName()).isEqualTo("updatedField");
    }

    @Test
    void applyDtoToEntity_shouldRemoveOrphanRules() {
        GlobalSetting existing = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "f", "v");
        UUID keptRuleId = UUID.randomUUID();
        ReflectionTestUtils.setField(existing.getRules().getFirst(), "id", keptRuleId);
        GlobalRuleCriteria orphan = new GlobalRuleCriteria();
        ReflectionTestUtils.setField(orphan, "id", UUID.randomUUID());
        orphan.setFieldName("orphan");
        existing.getRules().add(orphan);

        GlobalSettingDto dto = GlobalSettingsTestDataFactory.globalSettingDto("DXB", "DDU");
        dto.getRules().getFirst().setId(keptRuleId);

        factory.applyDtoToEntity(dto, existing);

        assertThat(existing.getRules()).hasSize(1);
        assertThat(existing.getRules().getFirst().getFieldName()).isEqualTo("originCountry");
    }

    @Test
    void newEntityFromDto_shouldTrimHeaderFieldsAndNormalizeCsvValues() {
        GlobalSettingDto dto = GlobalSettingsTestDataFactory.globalSettingDto(" DXB ", "DDU");
        dto.setTargetField(" originCountry ");
        dto.getRules().getFirst().setValues(" A , ,B ");
        dto.getRules().getFirst().setCurrency(" usd ");

        GlobalSetting entity = factory.newEntityFromDto(dto);

        assertThat(entity.getDestination()).isEqualTo("DXB");
        assertThat(entity.getTargetField()).isEqualTo("originCountry");
        assertThat(entity.getRules().getFirst().getValues()).isEqualTo("A,B");
        assertThat(entity.getRules().getFirst().getCurrency()).isEqualTo("USD");
    }

    @Test
    void applyDtoToEntity_shouldAddNewRule_whenDtoRuleHasNoId() {
        GlobalSetting existing = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "f", "v");
        GlobalRuleCriteriaDto newRule = new GlobalRuleCriteriaDto();
        newRule.setFieldName("newField");
        newRule.setOperator(Operator.EQUALS);
        newRule.setExactValue("X");

        GlobalSettingDto dto = new GlobalSettingDto();
        dto.setDestination("DXB");
        dto.setTargetValue("DDU");
        dto.setRules(List.of(newRule));

        factory.applyDtoToEntity(dto, existing);

        assertThat(existing.getRules()).hasSize(1);
        assertThat(existing.getRules().getFirst().getFieldName()).isEqualTo("newField");
    }

    @Test
    void applyDtoToEntity_shouldSkipNullDtoRulesAndRemoveOrphans() {
        GlobalSetting existing = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "f", "v");
        UUID keptRuleId = UUID.randomUUID();
        ReflectionTestUtils.setField(existing.getRules().getFirst(), "id", keptRuleId);
        List<com.customcharge.persistencelayer.dto.GlobalRuleCriteriaDto> dtoRules = new ArrayList<>();
        dtoRules.add(null);
        GlobalSettingDto dto = new GlobalSettingDto();
        dto.setDestination("DXB");
        dto.setTargetValue("DDU");
        dto.setRules(dtoRules);

        factory.applyDtoToEntity(dto, existing);

        assertThat(existing.getRules()).isEmpty();
    }

    @Test
    void applyDtoToEntity_shouldReorderRules_whenMergedOrderDiffers() {
        GlobalSetting existing = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "first", "v1");
        GlobalRuleCriteria second = new GlobalRuleCriteria();
        second.setFieldName("second");
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        ReflectionTestUtils.setField(existing.getRules().getFirst(), "id", firstId);
        ReflectionTestUtils.setField(second, "id", secondId);
        existing.getRules().add(second);

        GlobalRuleCriteriaDto secondDto = new GlobalRuleCriteriaDto();
        secondDto.setId(secondId);
        secondDto.setFieldName("second");
        secondDto.setOperator(Operator.EQUALS);
        secondDto.setExactValue("2");
        GlobalRuleCriteriaDto firstDto = new GlobalRuleCriteriaDto();
        firstDto.setId(firstId);
        firstDto.setFieldName("first");
        firstDto.setOperator(Operator.EQUALS);
        firstDto.setExactValue("1");

        GlobalSettingDto dto = new GlobalSettingDto();
        dto.setDestination("DXB");
        dto.setTargetValue("DDU");
        dto.setRules(List.of(secondDto, firstDto));

        factory.applyDtoToEntity(dto, existing);

        assertThat(existing.getRules()).extracting(GlobalRuleCriteria::getFieldName)
                .containsExactly("second", "first");
    }
}
