package com.customcharge.dashboard.keywordrules.dto;

import com.customcharge.persistencelayer.entities.KeywordRule;
import com.customcharge.persistencelayer.enums.MatchType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeywordRuleResponseTest {

    @Test
    void from_mapsStartWithAndEndWithToDisplayLabels() {
        assertEquals("START WITH", responseFor(MatchType.STARTWITH).matchType());
        assertEquals("END WITH", responseFor(MatchType.ENDWITH).matchType());
    }

    @Test
    void from_keepsOtherMatchTypesAsEnumName() {
        assertEquals("CONTAINS", responseFor(MatchType.CONTAINS).matchType());
        assertEquals("EXACT", responseFor(MatchType.EXACT).matchType());
    }

    @Test
    void from_mapsEntityFields() {
        KeywordRule rule = new KeywordRule();
        rule.setKeyword("test-keyword");
        rule.setMatchType(MatchType.EXACT);
        rule.setActive(true);

        KeywordRuleResponse response = KeywordRuleResponse.from(rule);

        assertEquals("test-keyword", response.keyword());
        assertEquals("EXACT", response.matchType());
        assertTrue(response.active());
    }

    private static KeywordRuleResponse responseFor(MatchType matchType) {
        KeywordRule rule = new KeywordRule();
        rule.setKeyword("kw");
        rule.setMatchType(matchType);
        rule.setActive(true);
        return KeywordRuleResponse.from(rule);
    }
}
