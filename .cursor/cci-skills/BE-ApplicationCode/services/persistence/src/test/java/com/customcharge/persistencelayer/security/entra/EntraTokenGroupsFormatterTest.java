package com.customcharge.persistencelayer.security.entra;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EntraTokenGroupsFormatterTest {

    @Test
    void formatsNonEmptyListAsJsonArray() {
        assertThat(EntraTokenGroupsFormatter.toUserGroupsColumn(Arrays.asList("a", " b ", "", null)))
                .isEqualTo("[\"a\",\"b\"]");
    }

    @Test
    void returnsNullForEmptyOrNullInput() {
        assertThat(EntraTokenGroupsFormatter.toUserGroupsColumn(null)).isNull();
        assertThat(EntraTokenGroupsFormatter.toUserGroupsColumn(List.of())).isNull();
        assertThat(EntraTokenGroupsFormatter.toUserGroupsColumn(Arrays.asList("", "  ", null))).isNull();
    }

    @Test
    void appliesGroupClaimMappingBeforeSerializing() {
        assertThat(
                        EntraTokenGroupsFormatter.toUserGroupsColumn(
                                List.of("a", "b"), g -> g.equals("a") ? "A1" : g))
                .isEqualTo("[\"A1\",\"b\"]");
    }
}
