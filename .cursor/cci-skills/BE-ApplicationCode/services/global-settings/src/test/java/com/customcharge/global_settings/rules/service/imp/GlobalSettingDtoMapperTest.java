package com.customcharge.global_settings.rules.service.imp;

import com.customcharge.global_settings.support.GlobalSettingsTestDataFactory;
import com.customcharge.persistencelayer.cache.CategoryCodesGlobalDataCacheService;
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalSettingDtoMapperTest {

    @Mock
    private CategoryCodesGlobalDataCacheService categoryCodesGlobalDataCacheService;

    @InjectMocks
    private GlobalSettingDtoMapper mapper;

    @Test
    void toDtoCodeOnly_shouldReturnNull_whenEntityNull() {
        assertThat(mapper.toDtoCodeOnly(null)).isNull();
    }

    @Test
    void toDtoCodeOnly_shouldMapBaseFields() {
        GlobalSetting entity = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "field", "val");
        ReflectionTestUtils.setField(entity, "createdTime", Instant.now());

        GlobalSettingDto dto = mapper.toDtoCodeOnly(entity);

        assertThat(dto.getDestination()).isEqualTo("DXB");
        assertThat(dto.getTargetValue()).isEqualTo("DDU");
        assertThat(dto.getEditable()).isTrue();
    }

    @Test
    void toDtoWithResolvedValues_shouldResolveLabels() {
        GlobalSetting entity = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "field", "val");
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("DXB"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("DXB", "Dubai", "Dubai Label"));
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("DDU"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("DDU", "DDU Name", "DDU Label"));
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("field"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("field", "fieldName", "Field Label"));
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("val"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("val", "valName", "Value Label"));

        GlobalSettingDto dto = mapper.toDtoWithResolvedValues(entity);

        assertThat(dto.getDestination()).isEqualTo("Dubai Label");
        assertThat(dto.getTargetValue()).isEqualTo("DDU Label");
        assertThat(dto.getRules().getFirst().getFieldName()).isEqualTo("Field Label");
    }

    @Test
    void toDtoCodeOnly_shouldMarkNotEditable_whenCreatedTimeMissing() {
        GlobalSetting entity = new GlobalSetting();
        ReflectionTestUtils.setField(entity, "createdTime", null);

        GlobalSettingDto dto = mapper.toDtoCodeOnly(entity);

        assertThat(dto.getEditable()).isFalse();
    }

    @Test
    void toDtoWithResolvedValues_shouldReturnNull_whenEntityNull() {
        assertThat(mapper.toDtoWithResolvedValues(null)).isNull();
    }

    @Test
    void toDtoCodeOnly_shouldReturnEmptyRules_whenEntityRulesNull() {
        GlobalSetting entity = new GlobalSetting();
        entity.setRules(null);

        GlobalSettingDto dto = mapper.toDtoCodeOnly(entity);

        assertThat(dto.getRules()).isEmpty();
    }

    @Test
    void toDtoCodeOnly_shouldMarkEditable_whenCreatedWithin24Hours() {
        GlobalSetting entity = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "field", "val");
        ReflectionTestUtils.setField(entity, "createdTime", Instant.now().minus(1, java.time.temporal.ChronoUnit.HOURS));

        GlobalSettingDto dto = mapper.toDtoCodeOnly(entity);

        assertThat(dto.getEditable()).isTrue();
    }

    @Test
    void toDtoWithResolvedValues_shouldResolveOperatorAndMultipleValues() {
        GlobalSetting entity = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "field", "val");
        entity.getRules().getFirst().setOperator(Operator.IN);
        entity.getRules().getFirst().setValues("val,other");
        entity.getRules().getFirst().setExactValue("1.00");
        entity.getRules().getFirst().setCurrency("USD");
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("DXB"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("DXB", "Dubai", "Dubai Label"));
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("DDU"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("DDU", "DDU Name", "DDU Label"));
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("field"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("field", "fieldName", "Field Label"));
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("val"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("val", "valName", "Value Label"));
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("other"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("other", "Other Name", "Other Label"));

        GlobalSettingDto dto = mapper.toDtoWithResolvedValues(entity);

        assertThat(dto.getRules().getFirst().getOperatorValue()).isEqualTo(Operator.IN.getValue());
        assertThat(dto.getRules().getFirst().getValues()).isEqualTo("Value Label,Other Label");
        assertThat(dto.getRules().getFirst().getExactValue()).isEqualTo("1.00");
        assertThat(dto.getRules().getFirst().getCurrency()).isEqualTo("USD");
    }

    @Test
    void toDtoWithResolvedValues_shouldKeepExactValueWithoutCategoryLookup_forEqualsOperator() {
        GlobalSetting entity = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "G_DESTINATION", "BG");
        GlobalRuleCriteria rule = entity.getRules().getFirst();
        rule.setFieldName("G_DESTINATION");
        rule.setOperator(Operator.EQUALS);
        rule.setValues(null);
        rule.setExactValue("132");

        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("DXB"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("DXB", "Dubai", "Dubai Label"));
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("DDU"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("DDU", "DDU Name", "DDU Label"));
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("G_DESTINATION"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("G_DESTINATION", "consigneeCountryCode", "Destination"));

        GlobalSettingDto dto = mapper.toDtoWithResolvedValues(entity);

        assertThat(dto.getRules().getFirst().getOperatorValue()).isEqualTo(Operator.EQUALS.getValue());
        assertThat(dto.getRules().getFirst().getValues()).isNull();
        assertThat(dto.getRules().getFirst().getExactValue()).isEqualTo("132");
    }
}
