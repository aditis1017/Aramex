package com.customcharge.dashboard.controller;

import com.customcharge.dashboard.dto.AccountRedisSyncJobStatusResponse;
import com.customcharge.dashboard.service.AccountService;
import com.customcharge.dashboard.service.impl.AccountRedisChargeDataAsyncSyncService;
import com.customcharge.persistencelayer.dto.response.MessageResponse;
import com.customcharge.persistencelayer.enums.AccountNature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountControllerRedisSyncTest {

    @Mock
    private AccountService accountService;
    @Mock
    private AccountRedisChargeDataAsyncSyncService accountRedisChargeDataAsyncSyncService;

    @InjectMocks
    private AccountController controller;

    @Test
    void syncAccountToRedis_shouldReturnAcceptedForBulkSync() {
        AccountRedisSyncJobStatusResponse job = new AccountRedisSyncJobStatusResponse(
                "job-1", "RUNNING", 0, 0, 0, "started", Instant.now(), null);
        when(accountRedisChargeDataAsyncSyncService.startSync(null, null, null, null)).thenReturn(job);

        ResponseEntity<?> response = controller.syncAccountToRedis(null, null, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isSameAs(job);
        verifyNoInteractions(accountService);
    }

    @Test
    void syncAccountToRedis_shouldReturnOkForSingleTarget() {
        MessageResponse message = new MessageResponse("synced");
        when(accountService.syncAccountToRedis("238296", AccountNature.AR, "RUH", null)).thenReturn(message);

        ResponseEntity<?> response = controller.syncAccountToRedis(
                "238296", AccountNature.AR, "RUH", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(message);
        verify(accountService).syncAccountToRedis("238296", AccountNature.AR, "RUH", null);
        verifyNoInteractions(accountRedisChargeDataAsyncSyncService);
    }
}
