package com.customcharge.dashboard.importfile.impl;

import com.customcharge.dashboard.constants.ExportHeaderConstants;
import com.customcharge.dashboard.constants.ImportConstants;
import com.customcharge.dashboard.keywordrules.dto.ParsedKeywordRuleRow;
import com.customcharge.persistencelayer.enums.MatchType;
import com.customcharge.persistencelayer.exception.ValidationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KeywordRuleSpreadsheetMapperTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @ParameterizedTest
    @ValueSource(strings = {"CONTAINS", "EXACT", "START WITH", "STARTWITH", "END WITH", "ENDWITH"})
    void mapRows_shouldAcceptAllowedMatchTypes(String matchType) {
        List<ParsedKeywordRuleRow> rows = KeywordRuleSpreadsheetMapper.mapRows(List.of(row("ab", matchType)));

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().matchType()).isNotNull();
    }

    @Test
    void mapRows_shouldMapStartWithAndEndWithToEnumValues() {
        List<ParsedKeywordRuleRow> rows = KeywordRuleSpreadsheetMapper.mapRows(List.of(
                row("ab", "START WITH"),
                row("cd", "END WITH")));

        assertThat(rows.get(0).matchType()).isEqualTo(MatchType.STARTWITH);
        assertThat(rows.get(1).matchType()).isEqualTo(MatchType.ENDWITH);
    }

    @Test
    void mapRows_shouldAcceptKeywordWithPeriod() {
        List<ParsedKeywordRuleRow> rows = KeywordRuleSpreadsheetMapper.mapRows(List.of(row("ab.co", "EXACT")));

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().keyword()).isEqualTo("ab.co");
    }

    @Test
    void mapRows_shouldPreserveLeadingAndTrailingWhitespaceInKeyword() {
        List<ParsedKeywordRuleRow> rows = KeywordRuleSpreadsheetMapper.mapRows(List.of(row(" ab ", "EXACT")));

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().keyword()).isEqualTo(" ab ");
    }

    @Test
    void mapRows_shouldRejectWordMatchType() {
        assertThatThrownBy(() -> KeywordRuleSpreadsheetMapper.mapRows(List.of(row("ab", "WORD"))))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid Match Type")
                .hasMessageContaining(ImportConstants.MATCH_TYPE_EXACT)
                .hasMessageNotContaining("WORD");
    }

    @Test
    void mapRows_shouldRejectInvalidMatchType() {
        assertThatThrownBy(() -> KeywordRuleSpreadsheetMapper.mapRows(List.of(row("ab", "LIKE"))))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid Match Type")
                .hasMessageContaining(ImportConstants.MATCH_TYPE_STARTWITH)
                .hasMessageContaining(ImportConstants.MATCH_TYPE_ENDWITH);
    }

    private static ObjectNode row(String keyword, String matchType) {
        return MAPPER.createObjectNode()
                .put(ExportHeaderConstants.KEYWORD, keyword)
                .put(ExportHeaderConstants.MATCH_TYPE, matchType)
                .put(ExportHeaderConstants.ACTIVE, "true");
    }
}
