package com.customcharge.dashboard.keywordrules.dto;


import com.customcharge.persistencelayer.enums.MatchType;

public record ParsedKeywordRuleRow(String keyword, MatchType matchType, boolean isActive) {
}
