package com.customcharge.persistencelayer.dto;

/**
 * Serializable rule metadata stored in {@code domain_snapshot} / {@code rule_snapshot} JSONB columns.
 */
public record RuleSnapshotEntry(
        String ruleId,
        int priority,
        String domainCode
) {
}
