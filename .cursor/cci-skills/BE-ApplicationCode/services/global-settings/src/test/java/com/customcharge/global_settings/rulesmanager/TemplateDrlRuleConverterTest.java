package com.customcharge.global_settings.rulesmanager;

import com.customcharge.global_settings.support.GlobalSettingsTestDataFactory;
import com.customcharge.persistencelayer.cache.CategoryCodesGlobalDataCacheService;
import com.customcharge.persistencelayer.entities.GlobalSetting;
import com.customcharge.persistencelayer.exception.InternalServerException;
import com.customcharge.persistencelayer.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemplateDrlRuleConverterTest {

    @Mock
    private CategoryCodesGlobalDataCacheService categoryCodesGlobalDataCacheService;

    @Test
    void should_ThrowValidationException_When_SettingMissing() {
        TemplateDrlRuleConverter converter = new TemplateDrlRuleConverter(categoryCodesGlobalDataCacheService);

        assertThatThrownBy(() -> converter.convert(null, GlobalSetting.class))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Global setting is required");
    }

    @Test
    void should_ThrowValidationException_When_TargetClassMissing() {
        TemplateDrlRuleConverter converter = new TemplateDrlRuleConverter(categoryCodesGlobalDataCacheService);
        GlobalSetting setting = new GlobalSetting();

        assertThatThrownBy(() -> converter.convert(setting, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Target class is required");
    }

    @Test
    void should_ThrowInternalServerException_When_UnexpectedErrorOccurs() {
        TemplateDrlRuleConverter converter = new TemplateDrlRuleConverter(categoryCodesGlobalDataCacheService);
        GlobalSetting setting = new GlobalSetting();
        setting.setTargetValue("T1");
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("T1"))
                .thenThrow(new RuntimeException("cache down"));

        List<GlobalSetting> settings = List.of(setting);
        assertThatThrownBy(() -> converter.convertAll(settings, GlobalSetting.class))
                .isInstanceOf(InternalServerException.class)
                .hasMessageContaining("Unable to generate Drools rules");
    }

    @Test
    void convertAll_shouldGenerateDrl_whenTemplateAvailable() {
        TemplateDrlRuleConverter converter = new TemplateDrlRuleConverter(categoryCodesGlobalDataCacheService);
        GlobalSetting setting = GlobalSettingsTestDataFactory.globalSetting("DXB", "ddu", "originCountry", "JO");
        setting.getRules().getFirst().setOperator(com.customcharge.persistencelayer.enums.Operator.EQUALS);
        setting.getRules().getFirst().setValues(null);
        setting.getRules().getFirst().setExactValue("JO");
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("originCountry"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("originCountry", "originCountry", "Origin"));
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("ddu"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("ddu", "DDU", "DDU"));

        String drl = converter.convertAll(List.of(setting), GlobalSetting.class);

        assertThat(drl).isNotBlank();
    }

    @Test
    void convert_shouldDelegateToConvertAll_whenValidSetting() {
        TemplateDrlRuleConverter converter = new TemplateDrlRuleConverter(categoryCodesGlobalDataCacheService);
        GlobalSetting setting = GlobalSettingsTestDataFactory.globalSetting("DXB", "ddp", "originCountry", "JO");
        setting.getRules().getFirst().setOperator(com.customcharge.persistencelayer.enums.Operator.EQUALS);
        setting.getRules().getFirst().setValues(null);
        setting.getRules().getFirst().setExactValue("JO");
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("originCountry"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("originCountry", "originCountry", "Origin"));
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("ddp"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("ddp", "DDP", "DDP"));

        String drl = converter.convert(setting, GlobalSetting.class);

        assertThat(drl).isNotBlank();
    }

    @Test
    void convertAll_shouldSkipNullSettings_andUseCategorySetterForNonDduDdp() {
        TemplateDrlRuleConverter converter = new TemplateDrlRuleConverter(categoryCodesGlobalDataCacheService);
        GlobalSetting setting = GlobalSettingsTestDataFactory.globalSetting("DXB", "CUSTOM", "originCountry", "JO");
        setting.getRules().getFirst().setOperator(com.customcharge.persistencelayer.enums.Operator.EQUALS);
        setting.getRules().getFirst().setValues(null);
        setting.getRules().getFirst().setExactValue("JO");
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("originCountry"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("originCountry", "originCountry", "Origin"));
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("CUSTOM"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("CUSTOM", "CustomType", "Custom"));

        String drl = converter.convertAll(Arrays.asList(null, setting), GlobalSetting.class);

        assertThat(drl).isNotBlank();
    }

    @Test
    void convertAll_shouldThrow_whenSettingsListMissing() {
        TemplateDrlRuleConverter converter = new TemplateDrlRuleConverter(categoryCodesGlobalDataCacheService);

        assertThatThrownBy(() -> converter.convertAll(null, GlobalSetting.class))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Global settings list");
    }

    @Test
    void convertAll_shouldThrow_whenTargetClassMissing() {
        TemplateDrlRuleConverter converter = new TemplateDrlRuleConverter(categoryCodesGlobalDataCacheService);
        List<GlobalSetting> settings = List.of(new GlobalSetting());

        assertThatThrownBy(() -> converter.convertAll(settings, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Target class");
    }

    @Test
    void convert_shouldWrapUnexpectedFailuresAsInternalServerException() {
        TemplateDrlRuleConverter converter = new TemplateDrlRuleConverter(categoryCodesGlobalDataCacheService);
        GlobalSetting setting = GlobalSettingsTestDataFactory.globalSetting("DXB", "ddu", "originCountry", "JO");
        setting.getRules().getFirst().setOperator(com.customcharge.persistencelayer.enums.Operator.EQUALS);
        setting.getRules().getFirst().setValues(null);
        setting.getRules().getFirst().setExactValue("JO");
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("ddu"))
                .thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> converter.convert(setting, GlobalSetting.class))
                .isInstanceOf(InternalServerException.class)
                .hasMessageContaining("Unable to generate Drools rules");
    }

    @Test
    void convertAll_shouldGenerateDrl_forDdpShipmentType() {
        TemplateDrlRuleConverter converter = new TemplateDrlRuleConverter(categoryCodesGlobalDataCacheService);
        GlobalSetting setting = GlobalSettingsTestDataFactory.globalSetting("DXB", "ddp", "originCountry", "JO");
        setting.getRules().getFirst().setOperator(com.customcharge.persistencelayer.enums.Operator.EQUALS);
        setting.getRules().getFirst().setValues(null);
        setting.getRules().getFirst().setExactValue("JO");
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("originCountry"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("originCountry", "originCountry", "Origin"));
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("ddp"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("ddp", "DDP", "DDP"));

        String drl = converter.convertAll(List.of(setting), GlobalSetting.class);

        assertThat(drl).isNotBlank();
    }
}
