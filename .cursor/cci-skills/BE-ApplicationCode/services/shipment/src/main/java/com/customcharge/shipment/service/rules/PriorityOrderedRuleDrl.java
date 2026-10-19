package com.customcharge.shipment.service.rules;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class PriorityOrderedRuleDrl {

    public record RuleEntry(String settingId, int priority, String drl, String domainCode) {

        public RuleEntry(String settingId, int priority, String drl) {
            this(settingId, priority, drl, null);
        }
    }

    private PriorityOrderedRuleDrl() {
    }

    public static List<String> orderedDrlStrings(Map<String, ?> drlMap) {
        return orderedRuleEntries(drlMap).stream().map(RuleEntry::drl).toList();
    }

    public static List<RuleEntry> orderedRuleEntries(Map<String, ?> drlMap) {
        return parseRuleRows(drlMap).stream()
                .sorted(Comparator
                        .comparingInt(RuleEntry::priority)
                        .thenComparing(RuleEntry::settingId, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    public static List<RuleEntry> mergedOrderedRuleEntries(Map<String, Map<String, ?>> rulesByCategoryTypeCode) {
        if (rulesByCategoryTypeCode == null || rulesByCategoryTypeCode.isEmpty()) {
            return List.of();
        }
        List<KeyedRuleRow> merged = new ArrayList<>();
        for (Map.Entry<String, Map<String, ?>> entry : rulesByCategoryTypeCode.entrySet()) {
            String categoryTypeCode = entry.getKey();
            for (RuleEntry row : parseRuleRows(entry.getValue())) {
                merged.add(new KeyedRuleRow(row.priority(), categoryTypeCode, row.settingId(), row.drl()));
            }
        }
        merged.sort(Comparator
                .comparingInt(KeyedRuleRow::priority)
                .thenComparing(KeyedRuleRow::categoryTypeCode, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(KeyedRuleRow::settingId, Comparator.nullsLast(String::compareTo)));
        return merged.stream()
                .map(r -> new RuleEntry(r.settingId(), r.priority(), r.drl(), r.categoryTypeCode()))
                .toList();
    }

    private static List<RuleEntry> parseRuleRows(Map<String, ?> drlMap) {
        if (drlMap == null || drlMap.isEmpty()) {
            return List.of();
        }
        return drlMap.entrySet().stream()
                .map(PriorityOrderedRuleDrl::toRow)
                .filter(Objects::nonNull)
                .toList();
    }

    private static RuleEntry toRow(Map.Entry<String, ?> entry) {
        if (!(entry.getValue() instanceof Map<?, ?> map)) {
            return null;
        }
        Object priorityObj = map.get("priority");
        if (!(priorityObj instanceof Number)) {
            return null;
        }
        Object ruleObj = map.get("rule");
        if (ruleObj == null) {
            return null;
        }
        String drl = ruleObj.toString();
        if (drl.isBlank()) {
            return null;
        }
        return new RuleEntry(entry.getKey(), ((Number) priorityObj).intValue(), drl, null);
    }

    private record KeyedRuleRow(int priority, String categoryTypeCode, String settingId, String drl) {
    }
}
