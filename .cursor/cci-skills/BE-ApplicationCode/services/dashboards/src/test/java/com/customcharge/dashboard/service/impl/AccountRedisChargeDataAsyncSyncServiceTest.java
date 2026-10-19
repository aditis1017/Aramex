package com.customcharge.dashboard.service.impl;

import com.customcharge.dashboard.dto.AccountRedisSyncJobStatusResponse;
import com.customcharge.persistencelayer.enums.AccountNature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountRedisChargeDataAsyncSyncServiceTest {

    @Mock
    private AccountRedisChargeDataSyncRunner syncRunner;

    @InjectMocks
    private AccountRedisChargeDataAsyncSyncService service;

    @Test
    void startSync_shouldReturnRunningJobAndDelegateToRunner() {
        AccountRedisSyncJobStatusResponse status =
                service.startSync(AccountNature.ENTITY_SETTINGS, "RUH", null, null);

        assertThat(status.getJobId()).isNotBlank();
        assertThat(status.getStatus()).isEqualTo("RUNNING");
        verify(syncRunner).runSync(
                eq(status.getJobId()),
                eq(AccountNature.ENTITY_SETTINGS),
                eq("RUH"),
                eq(null),
                eq(null),
                org.mockito.ArgumentMatchers.<Map<String, AccountRedisSyncJobStatusResponse>>any());
    }

    @Test
    void getStatus_shouldReturnTrackedJob() {
        AccountRedisSyncJobStatusResponse started = service.startSync(null, null, null, null);

        assertThat(service.getStatus(started.getJobId())).isSameAs(started);
        assertThat(service.getStatus("missing")).isNull();
    }
}
