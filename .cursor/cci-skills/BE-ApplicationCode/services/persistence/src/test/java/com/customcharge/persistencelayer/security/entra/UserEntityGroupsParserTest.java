package com.customcharge.persistencelayer.security.entra;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserEntityGroupsParserTest {

    @Test
    void parseEntityCodes_shouldParseJsonArray() {
        assertThat(UserEntityGroupsParser.parseEntityCodes("[\"dxb\",\"NIC\"]"))
                .containsExactly("DXB", "NIC");
    }

    @Test
    void parseEntityCodes_shouldParseCommaSeparatedFallback() {
        assertThat(UserEntityGroupsParser.parseEntityCodes("dxb, nic"))
                .containsExactly("DXB", "NIC");
    }

    @Test
    void parseEntityCodes_shouldReturnEmptyForBlank() {
        assertThat(UserEntityGroupsParser.parseEntityCodes(null)).isEmpty();
        assertThat(UserEntityGroupsParser.parseEntityCodes("  ")).isEmpty();
    }
}
