package com.customcharge.dashboard.keywordrules.dto;

import com.customcharge.persistencelayer.entities.KeywordRule;

import java.util.UUID;

public record KeywordRuleResponse(
        UUID id,
        String keyword,
        String matchType,
        boolean active
) {

    public static KeywordRuleResponse from(KeywordRule rule) {
        return new KeywordRuleResponse(
                rule.getId(),
                rule.getKeyword(),
                rule.getMatchType().displayLabel(),
                rule.isActive()
        );
    }
}
