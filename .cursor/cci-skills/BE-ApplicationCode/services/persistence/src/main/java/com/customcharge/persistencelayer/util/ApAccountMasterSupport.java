package com.customcharge.persistencelayer.util;

import com.customcharge.persistencelayer.entities.APAccount;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.Entity;
import com.customcharge.persistencelayer.enums.AccountNature;
import com.customcharge.persistencelayer.repository.APAccountRepository;
import com.customcharge.persistencelayer.repository.AccountRepository;

/**
 * Creates or resolves the AP master and per-charge-type {@link Account} config rows.
 */
public final class ApAccountMasterSupport {

    private ApAccountMasterSupport() {}

    public static APAccount findOrCreateMaster(
            APAccountRepository apAccountRepository,
            String entityCode,
            String accountNumber,
            String accountName) {

        String trimmedEntity = entityCode == null ? "" : entityCode.trim();
        String trimmedNumber = accountNumber == null ? "" : accountNumber.trim();
        return apAccountRepository
                .findByAccountNumberAndEntityCode(trimmedNumber, trimmedEntity)
                .orElseGet(() -> {
                    APAccount master = new APAccount();
                    master.setEntityCode(trimmedEntity);
                    master.setAccountNumber(trimmedNumber);
                    master.setAccountName(accountName == null ? "" : accountName.trim());
                    return apAccountRepository.save(master);
                });
    }

    public static Account findOrCreateChargeConfig(
            AccountRepository accountRepository,
            APAccount master,
            Entity entity,
            String chargeType) {

        String normalizedCharge = chargeType == null || chargeType.isBlank() ? null : chargeType.trim();
        return accountRepository
                .findByApAccountIdAndChargeType(master.getId(), normalizedCharge)
                .orElseGet(() -> accountRepository.saveChargeConfigOrFetchExisting(
                        newApChargeConfig(entity, master, normalizedCharge),
                        () -> accountRepository.findByApAccountIdAndChargeType(master.getId(), normalizedCharge)));
    }

    private static Account newApChargeConfig(Entity entity, APAccount master, String chargeType) {
        Account config = new Account();
        config.setEntity(entity);
        config.setAccountNature(AccountNature.AP);
        config.setApAccount(master);
        config.setChargeType(chargeType);
        AccountVersionSupport.ensureInitialized(config);
        return config;
    }

    public static void syncMasterIdentity(APAccount master, String accountName) {
        if (accountName != null && !accountName.isBlank()) {
            master.setAccountName(accountName.trim());
        }
    }

    public static boolean isApChargeConfig(Account account) {
        return account != null
                && AccountNature.AP.equals(account.getAccountNature())
                && account.getApAccount() != null;
    }

    public static String masterAccountNumber(Account account) {
        if (!isApChargeConfig(account)) {
            return null;
        }
        return account.getApAccount().getAccountNumber();
    }

    public static String masterEntityCode(Account account) {
        if (!isApChargeConfig(account)) {
            return account == null ? null : account.getEntityCode();
        }
        APAccount master = account.getApAccount();
        return master.getEntityCode() != null ? master.getEntityCode() : account.getEntityCode();
    }
}
