package com.customcharge.dashboard.service.impl;

import com.customcharge.dashboard.constants.AccountMessageConstants;
import com.customcharge.dashboard.dto.AccountRedisSyncJobStatusResponse;
import com.customcharge.dashboard.service.AccountService;
import com.customcharge.persistencelayer.entities.ARAccount;
import com.customcharge.persistencelayer.entities.APAccount;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.enums.AccountNature;
import com.customcharge.persistencelayer.repository.APAccountRepository;
import com.customcharge.persistencelayer.repository.ARAccountRepository;
import com.customcharge.persistencelayer.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountRedisChargeDataSyncRunner {

    private final AccountService accountService;
    private final ARAccountRepository arAccountRepository;
    private final APAccountRepository apAccountRepository;
    private final AccountRepository accountRepository;

    @Async("accountRedisSyncExecutor")
    public void runSync(
            String jobId,
            AccountNature accountNature,
            String entity,
            String accountNumber,
            String chargeType,
            Map<String, AccountRedisSyncJobStatusResponse> jobs) {
        AccountRedisSyncJobStatusResponse status = jobs.get(jobId);
        if (status == null) {
            return;
        }
        try {
            SyncCounters counters = syncTargets(accountNature, entity, accountNumber, chargeType);
            status.setProcessed(counters.processed());
            status.setSucceeded(counters.succeeded());
            status.setFailed(counters.failed());
            status.setStatus("COMPLETED");
            status.setMessage(AccountMessageConstants.CHARGE_DATA_SYNC_COMPLETED_PREFIX
                    + counters.succeeded()
                    + AccountMessageConstants.SYNCED_AR_MASTERS_SUFFIX
                    + counters.failed()
                    + AccountMessageConstants.SYNC_FAILURES_COUNT_SUFFIX);
            status.setCompletedAt(Instant.now());
        } catch (Exception ex) {
            log.error("Account Redis charge-data sync job {} failed", jobId, ex);
            status.setStatus("FAILED");
            status.setMessage(ex.getMessage());
            status.setCompletedAt(Instant.now());
        }
    }

    private SyncCounters syncTargets(
            AccountNature accountNature, String entity, String accountNumber, String chargeType) {
        if (isSpecificTarget(accountNature, entity, accountNumber, chargeType)) {
            return syncOne(accountNature, entity, accountNumber, chargeType);
        }
        return syncAll();
    }

    private SyncCounters syncAll() {
        int processed = 0;
        int succeeded = 0;
        int failed = 0;

        for (ARAccount master : arAccountRepository.findAll()) {
            processed++;
            if (syncSafely(AccountNature.AR, master.getEntityCode(), master.getAccountNumber(), null)) {
                succeeded++;
            } else {
                failed++;
            }
        }
        for (APAccount master : apAccountRepository.findAll()) {
            processed++;
            if (syncSafely(AccountNature.AP, master.getEntityCode(), master.getAccountNumber(), null)) {
                succeeded++;
            } else {
                failed++;
            }
        }
        Set<String> entityCodes = new LinkedHashSet<>();
        for (Account account : accountRepository.findByAccountNature(AccountNature.ENTITY_SETTINGS)) {
            if (StringUtils.hasText(account.getEntityCode())) {
                entityCodes.add(account.getEntityCode().trim());
            }
        }
        for (String entityCode : entityCodes) {
            processed++;
            if (syncSafely(AccountNature.ENTITY_SETTINGS, entityCode, null, null)) {
                succeeded++;
            } else {
                failed++;
            }
        }
        return new SyncCounters(processed, succeeded, failed);
    }

    private SyncCounters syncOne(
            AccountNature accountNature, String entity, String accountNumber, String chargeType) {
        boolean success = syncSafely(accountNature, entity, accountNumber, chargeType);
        return new SyncCounters(1, success ? 1 : 0, success ? 0 : 1);
    }

    private boolean syncSafely(
            AccountNature accountNature, String entity, String accountNumber, String chargeType) {
        try {
            accountService.syncAccountToRedis(accountNumber, accountNature, entity, chargeType);
            return true;
        } catch (Exception ex) {
            log.warn(
                    "Failed to sync account Redis charge data for nature={} entity={} accountNumber={} chargeType={}: {}",
                    accountNature,
                    entity,
                    accountNumber,
                    chargeType,
                    ex.getMessage());
            return false;
        }
    }

    public static boolean isSpecificTarget(
            AccountNature accountNature, String entity, String accountNumber, String chargeType) {
        return accountNature != null
                || StringUtils.hasText(entity)
                || StringUtils.hasText(accountNumber)
                || StringUtils.hasText(chargeType);
    }

    public static boolean isBulkSyncRequest(
            String accountNumber, AccountNature accountNature, String entity, String chargeType) {
        return !isSpecificTarget(accountNature, entity, accountNumber, chargeType);
    }

    private record SyncCounters(int processed, int succeeded, int failed) {}
}
