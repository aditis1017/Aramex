package com.customcharge.global_settings.rulesmanager;

import com.customcharge.persistencelayer.entities.GlobalRuleCriteria;
import com.customcharge.persistencelayer.enums.Operator;
import com.customcharge.persistencelayer.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RuleConditionBuilderTest {

    @Test
    void build_shouldReturnEmptyString_whenRulesMissing() {
        assertThat(RuleConditionBuilder.build(null)).isEmpty();
        assertThat(RuleConditionBuilder.build(List.of())).isEmpty();
    }

    @Test
    void build_shouldBuildEqualsCondition() {
        GlobalRuleCriteria rule = new GlobalRuleCriteria();
        rule.setFieldName("originCountry");
        rule.setOperator(Operator.EQUALS);
        rule.setExactValue("JO");

        assertThat(RuleConditionBuilder.build(List.of(rule)))
                .isEqualTo("originCountry == \"JO\"");
    }

    @Test
    void build_shouldBuildInCondition() {
        GlobalRuleCriteria rule = new GlobalRuleCriteria();
        rule.setFieldName("originCountry");
        rule.setOperator(Operator.IN);
        rule.setValues("JO,AE");

        assertThat(RuleConditionBuilder.build(List.of(rule)))
                .isEqualTo("originCountry in (\"JO\",\"AE\")");
    }

    @Test
    void build_shouldBuildNotInCondition() {
        GlobalRuleCriteria rule = new GlobalRuleCriteria();
        rule.setFieldName("productType");
        rule.setOperator(Operator.NOT_IN);
        rule.setValues("RDG,RDL,RMY");

        assertThat(RuleConditionBuilder.build(List.of(rule)))
                .isEqualTo("productType not in (\"RDG\",\"RDL\",\"RMY\")");
    }

    @Test
    void build_shouldThrow_whenFieldMissing() {
        GlobalRuleCriteria rule = new GlobalRuleCriteria();
        rule.setOperator(Operator.EQUALS);
        rule.setExactValue("JO");

        List<GlobalRuleCriteria> rules = List.of(rule);
        assertThatThrownBy(() -> RuleConditionBuilder.build(rules))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("field name");
    }

    @Test
    void build_shouldThrow_whenNumericOperatorHasInvalidValue() {
        GlobalRuleCriteria rule = new GlobalRuleCriteria();
        rule.setFieldName("weight");
        rule.setOperator(Operator.GREATERTHAN);
        rule.setExactValue("abc");

        List<GlobalRuleCriteria> rules = List.of(rule);
        assertThatThrownBy(() -> RuleConditionBuilder.build(rules))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("numeric value");
    }

    @Test
    void build_shouldBuildContainsCondition() {
        GlobalRuleCriteria rule = new GlobalRuleCriteria();
        rule.setFieldName("description");
        rule.setOperator(Operator.CONTAINS);
        rule.setExactValue("fragile");

        assertThat(RuleConditionBuilder.build(List.of(rule)))
                .isEqualTo("description.contains(\"fragile\")");
    }

    @Test
    void build_shouldThrow_whenInOperatorMissingValues() {
        GlobalRuleCriteria rule = new GlobalRuleCriteria();
        rule.setFieldName("originCountry");
        rule.setOperator(Operator.IN);

        List<GlobalRuleCriteria> rules = List.of(rule);
        assertThatThrownBy(() -> RuleConditionBuilder.build(rules))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("value list");
    }

    @Test
    void build_shouldThrow_whenNotInOperatorMissingValues() {
        GlobalRuleCriteria rule = new GlobalRuleCriteria();
        rule.setFieldName("productType");
        rule.setOperator(Operator.NOT_IN);

        List<GlobalRuleCriteria> rules = List.of(rule);
        assertThatThrownBy(() -> RuleConditionBuilder.build(rules))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("NOT IN");
    }

    @Test
    void build_shouldBuildNumericComparison() {
        GlobalRuleCriteria rule = new GlobalRuleCriteria();
        rule.setFieldName("weight");
        rule.setOperator(Operator.LESSTHANEQUALS);
        rule.setExactValue("10.5");

        assertThat(RuleConditionBuilder.build(List.of(rule)))
                .isEqualTo("weight <= 10.5");
    }

    @Test
    void build_shouldSkipNullRules_andJoinMultipleConditions() {
        GlobalRuleCriteria first = new GlobalRuleCriteria();
        first.setFieldName("originCountry");
        first.setOperator(Operator.NOT_EQUALS);
        first.setExactValue("JO");
        GlobalRuleCriteria second = new GlobalRuleCriteria();
        second.setFieldName("weight");
        second.setOperator(Operator.GREATERTHAN);
        second.setExactValue("5");

        assertThat(RuleConditionBuilder.build(Arrays.asList(null, first, second)))
                .isEqualTo("originCountry != \"JO\" && weight > 5");
    }

    @Test
    void build_shouldUseFirstCsvValue_whenValuesPresent() {
        GlobalRuleCriteria rule = new GlobalRuleCriteria();
        rule.setFieldName("originCountry");
        rule.setOperator(Operator.EQUALS);
        rule.setValues("JO,AE");
        rule.setExactValue("fallback");

        assertThat(RuleConditionBuilder.build(List.of(rule)))
                .isEqualTo("originCountry == \"JO\"");
    }

    @Test
    void build_shouldThrow_whenOperatorMissing() {
        GlobalRuleCriteria rule = new GlobalRuleCriteria();
        rule.setFieldName("originCountry");
        rule.setExactValue("JO");

        List<GlobalRuleCriteria> rules = List.of(rule);
        assertThatThrownBy(() -> RuleConditionBuilder.build(rules))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("operator");
    }

    @Test
    void build_shouldThrow_whenNumericValueMissing() {
        GlobalRuleCriteria rule = new GlobalRuleCriteria();
        rule.setFieldName("weight");
        rule.setOperator(Operator.LESSTHAN);

        List<GlobalRuleCriteria> rules = List.of(rule);
        assertThatThrownBy(() -> RuleConditionBuilder.build(rules))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("numeric value");
    }

    @Test
    void build_shouldThrow_whenInValuesAreBlankOnly() {
        GlobalRuleCriteria rule = new GlobalRuleCriteria();
        rule.setFieldName("originCountry");
        rule.setOperator(Operator.IN);
        rule.setValues(" , ");

        List<GlobalRuleCriteria> rules = List.of(rule);
        assertThatThrownBy(() -> RuleConditionBuilder.build(rules))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("value list");
    }

    @Test
    void build_shouldBuildGreaterThanEqualsCondition() {
        GlobalRuleCriteria rule = new GlobalRuleCriteria();
        rule.setFieldName("weight");
        rule.setOperator(Operator.GREATERTHANEQUALS);
        rule.setExactValue("1");

        assertThat(RuleConditionBuilder.build(List.of(rule)))
                .isEqualTo("weight >= 1");
    }

    @Test
    void build_shouldBuildLessThanCondition() {
        GlobalRuleCriteria rule = new GlobalRuleCriteria();
        rule.setFieldName("weight");
        rule.setOperator(Operator.LESSTHAN);
        rule.setExactValue("9");

        assertThat(RuleConditionBuilder.build(List.of(rule)))
                .isEqualTo("weight < 9");
    }

    @Test
    void build_shouldUseExactValue_whenFirstCsvValueBlank() {
        GlobalRuleCriteria rule = new GlobalRuleCriteria();
        rule.setFieldName("originCountry");
        rule.setOperator(Operator.EQUALS);
        rule.setValues(" ,JO");
        rule.setExactValue("AE");

        assertThat(RuleConditionBuilder.build(List.of(rule)))
                .isEqualTo("originCountry == \"AE\"");
    }
}
