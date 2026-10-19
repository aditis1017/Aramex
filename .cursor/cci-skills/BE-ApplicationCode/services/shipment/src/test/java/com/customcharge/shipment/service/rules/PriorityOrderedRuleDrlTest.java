package com.customcharge.shipment.service.rules;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PriorityOrderedRuleDrlTest {

    @Test
    void orderedRuleEntries_shouldSortByPriority() {
        Map<String, Object> drlMap = Map.of(
                "rule-b", Map.of("priority", 2, "rule", "rule B"),
                "rule-a", Map.of("priority", 1, "rule", "rule A"));

        List<PriorityOrderedRuleDrl.RuleEntry> entries = PriorityOrderedRuleDrl.orderedRuleEntries(drlMap);

        assertThat(entries).hasSize(2);
        assertThat(entries.getFirst().settingId()).isEqualTo("rule-a");
        assertThat(entries.getFirst().drl()).isEqualTo("rule A");
    }

    @Test
    void orderedDrlStrings_shouldReturnDrlOnly() {
        Map<String, Object> drlMap = Map.of(
                "rule-a", Map.of("priority", 1, "rule", "when then"));

        assertThat(PriorityOrderedRuleDrl.orderedDrlStrings(drlMap)).containsExactly("when then");
    }

    @Test
    void mergedOrderedRuleEntries_shouldMergeCategories() {
        Map<String, Map<String, ?>> rulesByCategory = new LinkedHashMap<>();
        rulesByCategory.put("CAT1", Map.of("r1", Map.of("priority", 2, "rule", "drl1")));
        rulesByCategory.put("CAT2", Map.of("r2", Map.of("priority", 1, "rule", "drl2")));

        List<PriorityOrderedRuleDrl.RuleEntry> merged =
                PriorityOrderedRuleDrl.mergedOrderedRuleEntries(rulesByCategory);

        assertThat(merged).hasSize(2);
        assertThat(merged.getFirst().domainCode()).isEqualTo("CAT2");
    }

    @Test
    void orderedRuleEntries_shouldIgnoreInvalidRows() {
        Map<String, Object> drlMap = Map.of(
                "invalid", "not-a-map",
                "blank", Map.of("priority", 1, "rule", "  "));

        assertThat(PriorityOrderedRuleDrl.orderedRuleEntries(drlMap)).isEmpty();
    }
}
