package com.customcharge.global_settings.rulesmanager;

import com.customcharge.global_settings.support.GlobalSettingsTestDataFactory;
import com.customcharge.persistencelayer.cache.CategoryCodesGlobalDataCacheService;
import com.customcharge.persistencelayer.entities.CategoryValue;
import com.customcharge.persistencelayer.entities.GlobalRuleCriteria;
import com.customcharge.persistencelayer.entities.GlobalSetting;
import com.customcharge.persistencelayer.enums.Operator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DrlRuleResolutionHelperTest {

    @Mock
    private CategoryCodesGlobalDataCacheService categoryCodesGlobalDataCacheService;

    @Test
    void buildCriteriaForDrl_shouldResolveFieldNameAndKeepCountryCodes_whenLabelsNotRequested() {
        CategoryValue originField = GlobalSettingsTestDataFactory.categoryValue(
                "G_ORIGIN", "shipperCountryCode", "Origin");
        originField.setLoadDataFrom("Country");
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("G_ORIGIN")).thenReturn(originField);

        GlobalRuleCriteria rule = new GlobalRuleCriteria();
        rule.setFieldName("G_ORIGIN");
        rule.setOperator(Operator.IN);
        rule.setValues("BG,FR");
        GlobalSetting setting = new GlobalSetting();
        setting.setRules(List.of(rule));

        List<GlobalRuleCriteria> criteria = DrlRuleResolutionHelper.buildCriteriaForDrl(
                setting, categoryCodesGlobalDataCacheService, false);

        assertThat(criteria).hasSize(1);
        assertThat(criteria.getFirst().getFieldName()).isEqualTo("shipperCountryCode");
        assertThat(criteria.getFirst().getValues()).isEqualTo("BG,FR");
    }

    @Test
    void buildCriteriaForDrl_shouldReturnEmptyList_whenRulesMissing() {
        GlobalSetting setting = new GlobalSetting();
        setting.setRules(null);

        List<GlobalRuleCriteria> criteria = DrlRuleResolutionHelper.buildCriteriaForDrl(
                setting, categoryCodesGlobalDataCacheService, false);

        assertThat(criteria).isEmpty();
    }

    @Test
    void buildCriteriaForDrl_shouldKeepCountryExactValueCode_forEqualsOperator() {
        CategoryValue destinationField = GlobalSettingsTestDataFactory.categoryValue(
                "G_DESTINATION", "consigneeCountryCode", "Destination");
        destinationField.setLoadDataFrom("Country");
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("G_DESTINATION")).thenReturn(destinationField);

        GlobalRuleCriteria rule = new GlobalRuleCriteria();
        rule.setFieldName("G_DESTINATION");
        rule.setOperator(Operator.EQUALS);
        rule.setExactValue("BG");
        GlobalSetting setting = new GlobalSetting();
        setting.setRules(List.of(rule));

        List<GlobalRuleCriteria> criteria = DrlRuleResolutionHelper.buildCriteriaForDrl(
                setting, categoryCodesGlobalDataCacheService, false);

        assertThat(criteria.getFirst().getFieldName()).isEqualTo("consigneeCountryCode");
        assertThat(criteria.getFirst().getExactValue()).isEqualTo("BG");
    }

    @Test
    void buildCriteriaForDrl_shouldKeepExactValueUnchanged_forAllOperators() {
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("G_BUSINESS_TYPE"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("G_BUSINESS_TYPE", "businessType", "Business Type"));
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("G_FRDM_FLAG"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("G_FRDM_FLAG", "frdmFlag", "FRDM Flag"));
        CategoryValue destinationField = GlobalSettingsTestDataFactory.categoryValue(
                "G_DESTINATION", "consigneeCountryCode", "Destination");
        destinationField.setLoadDataFrom("Country");
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("G_DESTINATION")).thenReturn(destinationField);

        GlobalRuleCriteria businessRule = new GlobalRuleCriteria();
        businessRule.setFieldName("G_BUSINESS_TYPE");
        businessRule.setOperator(Operator.NOT_EQUALS);
        businessRule.setExactValue("B2B_BT");

        GlobalRuleCriteria frdmRule = new GlobalRuleCriteria();
        frdmRule.setFieldName("G_FRDM_FLAG");
        frdmRule.setOperator(Operator.EQUALS);
        frdmRule.setExactValue("FRDM_NO");

        GlobalRuleCriteria destinationRule = new GlobalRuleCriteria();
        destinationRule.setFieldName("G_DESTINATION");
        destinationRule.setOperator(Operator.NOT_EQUALS);
        destinationRule.setExactValue("BG");

        GlobalSetting setting = new GlobalSetting();
        setting.setRules(List.of(businessRule, frdmRule, destinationRule));

        List<GlobalRuleCriteria> criteria = DrlRuleResolutionHelper.buildCriteriaForDrl(
                setting, categoryCodesGlobalDataCacheService, true);

        assertThat(criteria.get(0).getExactValue()).isEqualTo("B2B_BT");
        assertThat(criteria.get(1).getExactValue()).isEqualTo("FRDM_NO");
        assertThat(criteria.get(2).getExactValue()).isEqualTo("BG");
    }

    @Test
    void buildCriteriaForDrl_shouldResolveValuesToName_forDrlExceptCountryAndEntity() {
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("G_BUSINESS_TYPE"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("G_BUSINESS_TYPE", "businessType", "Business Type"));
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("B2B_BT"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("B2B_BT", "B2B", "B2B Label"));
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("B2C_BT"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("B2C_BT", "B2C", "B2C Label"));

        GlobalRuleCriteria rule = new GlobalRuleCriteria();
        rule.setFieldName("G_BUSINESS_TYPE");
        rule.setOperator(Operator.IN);
        rule.setValues("B2C_BT,B2B_BT");
        GlobalSetting setting = new GlobalSetting();
        setting.setRules(List.of(rule));

        List<GlobalRuleCriteria> criteria = DrlRuleResolutionHelper.buildCriteriaForDrl(
                setting, categoryCodesGlobalDataCacheService, false);

        assertThat(criteria.getFirst().getValues()).isEqualTo("B2B,B2C");
    }

    @Test
    void buildCriteriaForDrl_shouldKeepRawExactValue_forOtherEqualsFields() {
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("G_PRODUCT_TYPE"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("G_PRODUCT_TYPE", "productType", "Product Type"));

        GlobalRuleCriteria rule = new GlobalRuleCriteria();
        rule.setFieldName("G_PRODUCT_TYPE");
        rule.setOperator(Operator.EQUALS);
        rule.setExactValue("EXP");
        GlobalSetting setting = new GlobalSetting();
        setting.setRules(List.of(rule));

        List<GlobalRuleCriteria> criteria = DrlRuleResolutionHelper.buildCriteriaForDrl(
                setting, categoryCodesGlobalDataCacheService, false);

        assertThat(criteria.getFirst().getFieldName()).isEqualTo("productType");
        assertThat(criteria.getFirst().getExactValue()).isEqualTo("EXP");
    }

    @Test
    void buildCriteriaForDrl_shouldResolveValuesToLabel_whenLabelsRequested() {
        CategoryValue destinationField = GlobalSettingsTestDataFactory.categoryValue(
                "G_DESTINATION", "consigneeCountryCode", "Destination");
        destinationField.setLoadDataFrom("Country");
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("G_DESTINATION")).thenReturn(destinationField);
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("BG"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("BG", "Bulgaria", "Bulgaria Label"));

        GlobalRuleCriteria rule = new GlobalRuleCriteria();
        rule.setFieldName("G_DESTINATION");
        rule.setOperator(Operator.IN);
        rule.setValues("BG");
        rule.setExactValue("132");
        GlobalSetting setting = new GlobalSetting();
        setting.setRules(List.of(rule));

        List<GlobalRuleCriteria> criteria = DrlRuleResolutionHelper.buildCriteriaForDrl(
                setting, categoryCodesGlobalDataCacheService, true);

        assertThat(criteria.getFirst().getFieldName()).isEqualTo("Destination");
        assertThat(criteria.getFirst().getValues()).isEqualTo("Bulgaria Label");
        assertThat(criteria.getFirst().getExactValue()).isEqualTo("132");
    }

    @Test
    void buildCriteriaForDrl_shouldNormalizeExactValue() {
        GlobalRuleCriteria rule = new GlobalRuleCriteria();
        rule.setFieldName("weight");
        rule.setOperator(Operator.GREATERTHAN);
        rule.setExactValue("1.00");
        GlobalSetting setting = new GlobalSetting();
        setting.setRules(List.of(rule));
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("weight"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("weight", "weight", "Weight"));

        List<GlobalRuleCriteria> criteria = DrlRuleResolutionHelper.buildCriteriaForDrl(
                setting, categoryCodesGlobalDataCacheService, false);

        assertThat(criteria.getFirst().getExactValue()).isEqualTo("1");
    }

    @Test
    void buildCriteriaForDrl_shouldResolveLabels_whenRequested() {
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("field-code"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("field-code", "originCountry", "Origin Label"));
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("value-code"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("value-code", "Jordan", "Jordan Label"));
        GlobalSetting setting = GlobalSettingsTestDataFactory.globalSetting("DXB", "DDU", "field-code", "value-code");

        List<GlobalRuleCriteria> criteria = DrlRuleResolutionHelper.buildCriteriaForDrl(
                setting, categoryCodesGlobalDataCacheService, true);

        assertThat(criteria.getFirst().getFieldName()).isEqualTo("Origin Label");
    }

    @Test
    void buildCriteriaForDrl_shouldResolveValueLabels_whenRequested() {
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("field-code"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("field-code", "originCountry", "Origin Label"));
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("a-code"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("a-code", "Alpha", "Alpha Label"));
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("b-code"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("b-code", "Beta", "Beta Label"));
        GlobalRuleCriteria rule = new GlobalRuleCriteria();
        rule.setFieldName("field-code");
        rule.setOperator(Operator.IN);
        rule.setValues("b-code,a-code");
        GlobalSetting setting = new GlobalSetting();
        setting.setRules(List.of(rule));

        List<GlobalRuleCriteria> criteria = DrlRuleResolutionHelper.buildCriteriaForDrl(
                setting, categoryCodesGlobalDataCacheService, true);

        assertThat(criteria.getFirst().getValues()).isEqualTo("Alpha Label,Beta Label");
    }

    @Test
    void buildCriteriaForDrl_shouldSortResolvedValuesAndSkipBlankParts() {
        GlobalRuleCriteria rule = new GlobalRuleCriteria();
        rule.setFieldName("field");
        rule.setOperator(Operator.IN);
        rule.setValues("b-code, ,a-code");
        GlobalSetting setting = new GlobalSetting();
        List<GlobalRuleCriteria> rules = new ArrayList<>();
        rules.add(rule);
        rules.add(null);
        setting.setRules(rules);
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("field"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("field", "field", "Field"));

        List<GlobalRuleCriteria> criteria = DrlRuleResolutionHelper.buildCriteriaForDrl(
                setting, categoryCodesGlobalDataCacheService, false);

        assertThat(criteria).hasSize(1);
        assertThat(criteria.getFirst().getValues()).isEqualTo("a-code,b-code");
    }

    @Test
    void buildCriteriaForDrl_shouldKeepInvalidDecimalExactValue() {
        GlobalRuleCriteria rule = new GlobalRuleCriteria();
        rule.setFieldName("weight");
        rule.setOperator(Operator.GREATERTHAN);
        rule.setExactValue("1.2.3");
        GlobalSetting setting = new GlobalSetting();
        setting.setRules(List.of(rule));
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("weight"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("weight", "weight", "Weight"));

        List<GlobalRuleCriteria> criteria = DrlRuleResolutionHelper.buildCriteriaForDrl(
                setting, categoryCodesGlobalDataCacheService, false);

        assertThat(criteria.getFirst().getExactValue()).isEqualTo("1.2.3");
    }

    @Test
    void buildCriteriaForDrl_shouldReturnNullValues_whenBlank() {
        GlobalRuleCriteria rule = new GlobalRuleCriteria();
        rule.setFieldName(" ");
        rule.setOperator(Operator.EQUALS);
        rule.setValues(" ");
        rule.setExactValue(" ");
        GlobalSetting setting = new GlobalSetting();
        setting.setRules(List.of(rule));

        List<GlobalRuleCriteria> criteria = DrlRuleResolutionHelper.buildCriteriaForDrl(
                setting, categoryCodesGlobalDataCacheService, false);

        assertThat(criteria.getFirst().getFieldName()).isEqualTo(" ");
        assertThat(criteria.getFirst().getValues()).isNull();
        assertThat(criteria.getFirst().getExactValue()).isNull();
        assertThat(criteria.getFirst().getCurrency()).isNull();
    }

    @Test
    void buildCriteriaForDrl_shouldNormalizeCurrency() {
        GlobalRuleCriteria rule = new GlobalRuleCriteria();
        rule.setFieldName("amount");
        rule.setOperator(Operator.GREATERTHAN);
        rule.setExactValue("100");
        rule.setCurrency(" usd ");
        GlobalSetting setting = new GlobalSetting();
        setting.setRules(List.of(rule));
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("amount"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("amount", "amount", "Amount"));

        List<GlobalRuleCriteria> criteria = DrlRuleResolutionHelper.buildCriteriaForDrl(
                setting, categoryCodesGlobalDataCacheService, false);

        assertThat(criteria.getFirst().getCurrency()).isEqualTo("USD");
    }

    @Test
    void buildCriteriaForDrl_shouldKeepRawExactValue_forCostFields_whenBuildingRuleCondition() {
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("G_COST_OF_GOODS"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("G_COST_OF_GOODS", "costOfGoods", "Cost of Goods"));
        when(categoryCodesGlobalDataCacheService.getCategoryCodeByCode("G_CIF"))
                .thenReturn(GlobalSettingsTestDataFactory.categoryValue("G_CIF", "costOfGoods", "CIF"));

        GlobalRuleCriteria costRule = new GlobalRuleCriteria();
        costRule.setFieldName("G_COST_OF_GOODS");
        costRule.setOperator(Operator.EQUALS);
        costRule.setExactValue("132");

        GlobalRuleCriteria cifRule = new GlobalRuleCriteria();
        cifRule.setFieldName("G_CIF");
        cifRule.setOperator(Operator.NOT_EQUALS);
        cifRule.setExactValue("100.00");

        GlobalSetting setting = new GlobalSetting();
        setting.setRules(List.of(costRule, cifRule));

        List<GlobalRuleCriteria> criteria = DrlRuleResolutionHelper.buildCriteriaForDrl(
                setting, categoryCodesGlobalDataCacheService, true);

        assertThat(criteria.get(0).getFieldName()).isEqualTo("Cost of Goods");
        assertThat(criteria.get(0).getExactValue()).isEqualTo("132");
        assertThat(criteria.get(1).getFieldName()).isEqualTo("CIF");
        assertThat(criteria.get(1).getExactValue()).isEqualTo("100");
    }
}
