package com.customcharge.global_settings.rulesmanager;

import com.customcharge.global_settings.support.GlobalSettingsTestDataFactory;
import com.customcharge.persistencelayer.cache.CategoryCodesGlobalDataCacheService;
import com.customcharge.persistencelayer.dto.ShipmentDto;
import com.customcharge.persistencelayer.entities.GlobalSetting;
import com.customcharge.persistencelayer.exception.InternalServerException;
import com.customcharge.persistencelayer.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JavaDrlRuleConverterTest {

    @Mock
    private CategoryCodesGlobalDataCacheService categoryCodesGlobalDataCacheService;

    @InjectMocks
    private JavaDrlRuleConverter converter;

    @Test
    void convert_shouldThrow_whenSettingMissing() {
        assertThatThrownBy(() -> converter.convert(null, ShipmentDto.class))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void convert_shouldThrow_whenTargetClassMissing() {
        GlobalSetting setting = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "originCountry", "JO");

        assertThatThrownBy(() -> converter.convert(setting, null))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void convert_shouldEscapeQuotesInTargetValue() {
        GlobalSetting setting = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "originCountry", "JO");
        setting.getRules().getFirst().setOperator(com.customcharge.persistencelayer.enums.Operator.EQUALS);
        setting.getRules().getFirst().setValues(null);
        setting.getRules().getFirst().setExactValue("JO");
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("originCountry"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("originCountry", "originCountry", "Origin"));
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("DDU"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("DDU", "DDU with \"quote\"", "DDU"));

        String drl = converter.convert(setting, ShipmentDto.class);

        assertThat(drl).contains("DDU with \\\"quote\\\"");
    }

    @Test
    void convert_shouldWrapUnexpectedErrors() {
        GlobalSetting setting = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "originCountry", "JO");
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("originCountry"))
                .thenThrow(new RuntimeException("cache down"));

        assertThatThrownBy(() -> converter.convert(setting, ShipmentDto.class))
                .isInstanceOf(InternalServerException.class);
    }

    @Test
    void convert_shouldGenerateDrl_whenValidSetting() {
        GlobalSetting setting = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "originCountry", "JO");
        setting.getRules().getFirst().setOperator(com.customcharge.persistencelayer.enums.Operator.EQUALS);
        setting.getRules().getFirst().setValues(null);
        setting.getRules().getFirst().setExactValue("JO");
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("originCountry"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("originCountry", "originCountry", "Origin"));
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("DDU"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("DDU", "DDU", "DDU"));

        String drl = converter.convert(setting, ShipmentDto.class);

        assertThat(drl)
                .contains("rule \"GlobalRule\"")
                .contains("setShipmentType");
    }
}
