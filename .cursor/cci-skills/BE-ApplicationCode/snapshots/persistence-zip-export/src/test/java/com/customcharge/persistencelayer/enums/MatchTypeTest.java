package com.customcharge.persistencelayer.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MatchTypeTest {

    @Test
    void displayLabel_returnsSpacedLabelsForStartAndEndWith() {
        assertEquals("START WITH", MatchType.STARTWITH.displayLabel());
        assertEquals("END WITH", MatchType.ENDWITH.displayLabel());
    }

    @Test
    void displayLabel_returnsEnumNameForOtherTypes() {
        assertEquals("CONTAINS", MatchType.CONTAINS.displayLabel());
        assertEquals("EXACT", MatchType.EXACT.displayLabel());
    }
}
