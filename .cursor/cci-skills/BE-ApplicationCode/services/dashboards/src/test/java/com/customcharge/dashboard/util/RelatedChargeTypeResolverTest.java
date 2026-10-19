package com.customcharge.dashboard.util;

import com.customcharge.dashboard.constants.ChargeTypeConstants;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RelatedChargeTypeResolverTest {

    @Test
    void expandChargeTypes_shouldLinkEcdToDtcForDdu() {
        Set<String> expanded = RelatedChargeTypeResolver.expandChargeTypes("ECD", "DDU");
        assertThat(expanded).containsExactly("ECD", ChargeTypeConstants.DTC);
    }

    @Test
    void expandChargeTypes_shouldLinkEcdToDtaForDdp() {
        Set<String> expanded = RelatedChargeTypeResolver.expandChargeTypes("ECD", "DDP");
        assertThat(expanded).containsExactly("ECD", ChargeTypeConstants.DTA);
    }

    @Test
    void expandChargeTypes_shouldLinkDtaToEcd() {
        Set<String> expanded = RelatedChargeTypeResolver.expandChargeTypes("DTA", "DDP");
        assertThat(expanded).containsExactly("DTA", ChargeTypeConstants.ECD);
    }

    @Test
    void filterPresentChargeTypes_shouldKeepMatchingCodes() {
        Set<String> present = RelatedChargeTypeResolver.filterPresentChargeTypes(
                "DTA",
                "DDP",
                new LinkedHashSet<>(Set.of("ECD", "DTA", "DTC")));

        assertThat(present).containsExactlyInAnyOrder("ECD", "DTA");
    }

    @Test
    void normalize_shouldUppercaseChargeType() {
        assertThat(RelatedChargeTypeResolver.normalize(" dtc ")).isEqualTo("DTC");
    }
}
