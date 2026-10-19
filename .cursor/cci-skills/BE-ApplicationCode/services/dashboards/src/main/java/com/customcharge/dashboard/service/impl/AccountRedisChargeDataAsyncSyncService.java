package com.customcharge.dashboard.service.impl;

import com.customcharge.dashboard.constants.AccountMessageConstants;
import com.customcharge.dashboard.dto.AccountRedisSyncJobStatusResponse;
import com.customcharge.persistencelayer.enums.AccountNature;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AccountRedisChargeDataAsyncSyncService {

    private final AccountRedisChargeDataSyncRunner syncRunner;

    private final Map<String, AccountRedisSyncJobStatusResponse> jobs = new ConcurrentHashMap<>();

    public AccountRedisSyncJobStatusResponse startSync(
            AccountNature accountNature, String entity, String accountNumber, String chargeType) {
        String jobId = UUID.randomUUID().toString();
        AccountRedisSyncJobStatusResponse status = new AccountRedisSyncJobStatusResponse(
                jobId,
                "RUNNING",
                0,
                0,
                0,
                AccountMessageConstants.CHARGE_DATA_SYNC_STARTED,
                Instant.now(),
                null);
        jobs.put(jobId, status);
        syncRunner.runSync(jobId, accountNature, entity, accountNumber, chargeType, jobs);
        return status;
    }

    public AccountRedisSyncJobStatusResponse getStatus(String jobId) {
        return jobs.get(jobId);
    }
}
