package com.customcharge.shipment.service.rules;

import com.customcharge.persistencelayer.cache.MasterCategoryClosureLookupService;
import com.customcharge.persistencelayer.dto.CurrencyConversionResult;
import com.customcharge.persistencelayer.dto.ShipmentDto;
import com.customcharge.persistencelayer.rules.GlobalSettingRuleLookupService;
import com.customcharge.persistencelayer.rules.RuleEngine;
import com.customcharge.persistencelayer.util.ExchangeRateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.customcharge.shipment.constants.MasterDataCategoryCodes.CATEGORY_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryTypeRuleResolverTest {

    @Mock
    private MasterCategoryClosureLookupService masterCategoryClosureLookupService;
    @Mock
    private GlobalSettingRuleLookupService globalSettingRuleLookupService;
    @Mock
    private RuleEngine ruleEngine;
    @Mock
    private ObjectProvider<ExchangeRateService> exchangeRateService;
    @Mock
    private ExchangeRateService exchangeRate;

    @InjectMocks
    private CategoryTypeRuleResolver resolver;

    @Test
    void resolveCategoryType_shouldReturnEmpty_whenMessageNull() {
        assertThat(resolver.resolveCategoryType(null)).isEmpty();
    }

    @Test
    void resolveCategoryType_shouldReturnExistingValue_whenAlreadySet() {
        ShipmentDto message = new ShipmentDto();
        message.setCategoryType("HV");

        Optional<RuleResolverSupport.ResolvedRule> result = resolver.resolveCategoryType(message);

        assertThat(result).isPresent();
        assertThat(result.get().value()).isEqualTo("HV");
        assertThat(result.get().assignment()).isNull();
        verify(masterCategoryClosureLookupService, never()).loadCandidateCodes(any());
    }

    @Test
    void resolveCategoryType_shouldReturnEmpty_whenDestinationMissing() {
        ShipmentDto message = new ShipmentDto();

        assertThat(resolver.resolveCategoryType(message)).isEmpty();
    }

    @Test
    void resolveCategoryType_shouldReturnEmpty_whenNoCandidateCodes() {
        ShipmentDto message = new ShipmentDto();
        message.setConsigneeCountryCode("AE");
        when(masterCategoryClosureLookupService.loadCandidateCodes(CATEGORY_TYPE))
                .thenReturn(List.of());

        assertThat(resolver.resolveCategoryType(message)).isEmpty();
    }

    @Test
    void resolveCategoryType_shouldResolveFromRules() {
        ShipmentDto message = new ShipmentDto();
        message.setConsigneeCountryCode("AE");
        UUID ruleId = UUID.randomUUID();

        when(masterCategoryClosureLookupService.loadCandidateCodes(CATEGORY_TYPE))
                .thenReturn(List.of("HV"));
        when(globalSettingRuleLookupService.loadDrlMap("AE", "HV"))
                .thenReturn(Map.of(ruleId.toString(), Map.of("priority", 1, "rule", "rule-hv")));
        doAnswer(invocation -> {
            ShipmentDto dto = invocation.getArgument(0);
            dto.setCategoryType("HV");
            return null;
        }).when(ruleEngine).execute(any(ShipmentDto.class), eq("rule-hv"));

        Optional<RuleResolverSupport.ResolvedRule> result = resolver.resolveCategoryType(message);

        assertThat(result).isPresent();
        assertThat(result.get().value()).isEqualTo("HV");
        assertThat(message.getCategoryType()).isEqualTo("HV");
        assertThat(result.get().assignment()).isNotNull();
        assertThat(result.get().assignment().ruleId()).isEqualTo(ruleId);
    }

    @Test
    void resolveCategoryType_shouldConvertCostOfGoodsForRules_thenRestoreOriginal() {
        ShipmentDto message = new ShipmentDto();
        message.setConsigneeCountryCode("AE");
        message.setCostOfGoods(100.0);
        message.setCustomsCurrencyCode("USD");
        UUID ruleId = UUID.randomUUID();

        when(exchangeRateService.getIfAvailable()).thenReturn(exchangeRate);
        when(exchangeRate.convertAmount(100.0, "USD", "SAR"))
                .thenReturn(new CurrencyConversionResult(375.0, 3.75));
        when(masterCategoryClosureLookupService.loadCandidateCodes(CATEGORY_TYPE))
                .thenReturn(List.of("HV"));
        when(globalSettingRuleLookupService.loadDrlMap("AE", "HV"))
                .thenReturn(Map.of(ruleId.toString(), Map.of("priority", 1, "rule", "rule-hv")));
        doAnswer(invocation -> {
            ShipmentDto dto = invocation.getArgument(0);
            assertThat(dto.getCostOfGoods()).isEqualTo(375.0);
            dto.setCategoryType("HV");
            return null;
        }).when(ruleEngine).execute(any(ShipmentDto.class), eq("rule-hv"));

        Optional<RuleResolverSupport.ResolvedRule> result = resolver.resolveCategoryType(message);

        assertThat(result).isPresent();
        assertThat(result.get().value()).isEqualTo("HV");
        assertThat(message.getCategoryType()).isEqualTo("HV");
        assertThat(message.getCostOfGoods()).isEqualTo(100.0);
    }
}
