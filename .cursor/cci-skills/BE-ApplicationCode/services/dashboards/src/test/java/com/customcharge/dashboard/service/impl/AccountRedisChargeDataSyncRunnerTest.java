package com.customcharge.dashboard.service.impl;

import com.customcharge.persistencelayer.enums.AccountNature;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccountRedisChargeDataSyncRunnerTest {

    @Test
    void isBulkSyncRequest_shouldBeTrue_whenAllFiltersMissing() {
        assertThat(AccountRedisChargeDataSyncRunner.isBulkSyncRequest(null, null, null, null)).isTrue();
        assertThat(AccountRedisChargeDataSyncRunner.isBulkSyncRequest(" ", null, " ", " ")).isTrue();
    }

    @Test
    void isBulkSyncRequest_shouldBeFalse_whenAnyFilterProvided() {
        assertThat(AccountRedisChargeDataSyncRunner.isBulkSyncRequest(null, AccountNature.AR, null, null))
                .isFalse();
        assertThat(AccountRedisChargeDataSyncRunner.isBulkSyncRequest(null, null, "RUH", null))
                .isFalse();
        assertThat(AccountRedisChargeDataSyncRunner.isBulkSyncRequest("238296", null, null, null))
                .isFalse();
        assertThat(AccountRedisChargeDataSyncRunner.isBulkSyncRequest(null, null, null, "DTC"))
                .isFalse();
    }
}
