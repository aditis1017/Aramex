package com.customcharge.global_settings.rulesmanager;

import com.customcharge.persistencelayer.entities.GlobalRuleCriteria;
import com.customcharge.persistencelayer.enums.Operator;
import com.customcharge.persistencelayer.exception.BadRequestException;
import com.customcharge.persistencelayer.exception.ValidationException;

import java.util.ArrayList;
import java.util.List;

public class RuleConditionBuilder {
    private RuleConditionBuilder(){}
    public static String build(List<GlobalRuleCriteria> rules) {

        if (rules == null || rules.isEmpty()) {
            return "";
        }

        List<String> conditions = new ArrayList<>();

        for (GlobalRuleCriteria rule : rules) {

            if (rule == null) {
                continue;
            }

            String field = rule.getFieldName();
            Operator operator = rule.getOperator();
            String value = extractSingleValue(rule);

            if (field == null || field.isBlank()) {
                throw new ValidationException("Each rule must have a non-empty field name.");
            }
            if (operator == null) {
                throw new ValidationException("Each rule must have an operator.");
            }

            switch (operator) {

                case EQUALS:
                    conditions.add(field + " == \"" + value + "\"");
                    break;

                case NOT_EQUALS:
                    conditions.add(field + " != \"" + value + "\"");
                    break;

                case CONTAINS:
                    conditions.add(field + ".contains(\"" + value + "\")");
                    break;

                case IN:
                    conditions.add(buildMembershipCondition(rule, field, false));
                    break;

                case NOT_IN:
                    conditions.add(buildMembershipCondition(rule, field, true));
                    break;

                case GREATERTHAN:
                    conditions.add(field + " > " + parseNumericValue(value, operator));
                    break;

                case LESSTHAN:
                    conditions.add(field + " < " + parseNumericValue(value, operator));
                    break;

                case GREATERTHANEQUALS:
                    conditions.add(field + " >= " + parseNumericValue(value, operator));
                    break;

                case LESSTHANEQUALS:
                    conditions.add(field + " <= " + parseNumericValue(value, operator));
                    break;

                default:
                    throw new BadRequestException("Unsupported operator: " + operator);
            }
        }

        return String.join(" && ", conditions);
    }

    private static String buildMembershipCondition(GlobalRuleCriteria rule, String field, boolean negate) {
        String operatorLabel = negate ? "NOT IN" : "IN";
        if (rule.getValues() == null || rule.getValues().isBlank()) {
            throw new ValidationException("Rules using the " + operatorLabel + " operator must include a value list.");
        }
        String[] rawValues = rule.getValues().split(",");
        StringBuilder values = new StringBuilder();
        for (String rawValue : rawValues) {
            if (rawValue == null || rawValue.isBlank()) {
                continue;
            }
            if (!values.isEmpty()) {
                values.append(",");
            }
            values.append("\"").append(rawValue.trim()).append("\"");
        }
        if (values.isEmpty()) {
            throw new ValidationException("Rules using the " + operatorLabel + " operator must include a value list.");
        }
        String droolsOperator = negate ? "not in" : "in";
        return field + " " + droolsOperator + " (" + values + ")";
    }

    private static String extractSingleValue(GlobalRuleCriteria rule) {
        if (rule.getValues() == null || rule.getValues().isBlank()) {
            return rule.getExactValue();
        }
        String[] split = rule.getValues().split(",");
        return split.length == 0 || split[0] == null || split[0].isBlank() ? rule.getExactValue() : split[0].trim();
    }

    private static String parseNumericValue(String value, Operator operator) {
        if (value == null || value.isBlank()) {
            throw new ValidationException("Rules using " + operator + " must include a numeric value.");
        }
        try {
            Double.parseDouble(value.trim());
            return value.trim();
        } catch (NumberFormatException exception) {
            throw new ValidationException("Rules using " + operator + " must include a valid numeric value.");
        }
    }

}
