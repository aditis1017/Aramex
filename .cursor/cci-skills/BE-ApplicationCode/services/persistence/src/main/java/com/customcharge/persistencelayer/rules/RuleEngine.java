package com.customcharge.persistencelayer.rules;

import java.util.List;
import java.util.function.Function;

public interface RuleEngine {
    <T> T execute(T fact, String rules);

    /**
     * Executes rules against the fact and returns how many rules fired (0 = no match).
     */
    <T> int countRulesFired(T fact, String rules);

    /**
     * Executes DRL strings in order until {@code stopCondition} returns true.
     *
     * @return number of rules executed (including the rule that satisfied the stop condition)
     */
    <T> int executeUntil(
            T fact,
            List<String> drls,
            Function<T, Boolean> stopCondition,
            Function<String, String> drlAdapter);
}
