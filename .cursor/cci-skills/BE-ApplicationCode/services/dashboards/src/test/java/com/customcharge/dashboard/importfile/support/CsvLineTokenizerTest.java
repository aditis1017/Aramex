package com.customcharge.dashboard.importfile.support;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CsvLineTokenizerTest {

    @Test
    void splitLine_shouldTrimTokensByDefault() {
        assertThat(CsvLineTokenizer.splitLine(" ab , cd ")).containsExactly("ab", "cd");
    }

    @Test
    void splitLine_shouldPreserveWhitespaceWhenTrimDisabled() {
        assertThat(CsvLineTokenizer.splitLine(" ab , cd ", false)).containsExactly(" ab ", " cd ");
    }
}
