package com.customcharge.dashboard.search;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EntityChargeViewSearchSpecificationFactoryTest {

    @Test
    void mapFilterAccountTypesToDb_shouldMapCashAndAccountTokens() {
        assertThat(EntityChargeViewSearchSpecificationFactory.mapFilterAccountTypesToDb(
                List.of("cash", "account")))
                .containsExactly("CASH", "ACCOUNT");
    }

    @Test
    void mapFilterAccountTypesToDb_shouldAcceptDisplayCaseFromUi() {
        assertThat(EntityChargeViewSearchSpecificationFactory.mapFilterAccountTypesToDb(
                List.of("ACCOUNT")))
                .containsExactly("ACCOUNT");
    }

    @Test
    void boeNumberMatchesAnyLineItem_shouldReturnConjunction_whenInputEmpty() {
        assertThat(EntityChargeViewSearchSpecificationFactory.boeNumberMatchesAnyLineItem(List.of()))
                .isNotNull();
    }

    @Test
    void mapFilterAccountTypesToDb_shouldReturnEmpty_whenInputEmpty() {
        assertThat(EntityChargeViewSearchSpecificationFactory.mapFilterAccountTypesToDb(List.of()))
                .isEmpty();
    }
}
