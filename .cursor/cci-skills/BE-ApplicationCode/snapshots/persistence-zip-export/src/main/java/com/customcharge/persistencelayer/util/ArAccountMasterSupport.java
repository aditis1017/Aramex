package com.customcharge.persistencelayer.util;

import com.customcharge.persistencelayer.entities.ARAccount;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.Entity;
import com.customcharge.persistencelayer.enums.AccountNature;
import com.customcharge.persistencelayer.exception.BadRequestException;
import com.customcharge.persistencelayer.exception.ResourceNotFoundException;
import com.customcharge.persistencelayer.repository.ARAccountRepository;
import com.customcharge.persistencelayer.repository.AccountRepository;
import com.customcharge.persistencelayer.repository.EntityRepository;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * Creates or resolves the AR master and per-charge-type {@link Account} config rows.
 */
public final class ArAccountMasterSupport {

    private ArAccountMasterSupport() {}

    public static ARAccount findOrCreateMaster(
            ARAccountRepository arAccountRepository,
            String entityCode,
            String accountNumber,
            String accountName,
            String countryCode) {

        String trimmedEntity = entityCode == null ? "" : entityCode.trim();
        String trimmedNumber = accountNumber == null ? "" : accountNumber.trim();
        return arAccountRepository
                .findByAccountNumberAndEntityCode(trimmedNumber, trimmedEntity)
                .orElseGet(() -> {
                    ARAccount master = new ARAccount();
                    master.setEntityCode(trimmedEntity);
                    master.setAccountNumber(trimmedNumber);
                    master.setAccountName(accountName == null ? "" : accountName.trim());
                    master.setCountryCode(countryCode);
                    return arAccountRepository.save(master);
                });
    }

    public static Account findOrCreateChargeConfig(
            AccountRepository accountRepository,
            EntityRepository entityRepository,
            ARAccount master,
            Entity entity,
            String chargeType) {

        String normalizedCharge = chargeType == null || chargeType.isBlank() ? null : chargeType.trim();
        return accountRepository
                .findByArAccountIdAndChargeType(master.getId(), normalizedCharge)
                .orElseGet(() -> {
                    Entity managedEntity = resolveManagedEntity(entityRepository, entity, master);
                    return accountRepository.saveChargeConfigOrFetchExisting(
                            newArChargeConfig(managedEntity, master, normalizedCharge),
                            () -> accountRepository.findByArAccountIdAndChargeType(
                                    master.getId(), normalizedCharge));
                });
    }

    /**
     * Loads a persistence-managed {@link Entity} for account-config saves. Entity-settings rows
     * resolved from Redis carry a transient entity stub; this ensures Hibernate receives a managed
     * reference before {@link AccountRepository#save(Object)}.
     */
    static Entity resolveManagedEntity(EntityRepository entityRepository, Entity entity, ARAccount master) {
        String entityCode = resolveEntityCode(entity, master);
        return entityRepository.findByCodeIgnoreCase(entityCode)
                .orElseThrow(() -> new ResourceNotFoundException("Entity not found: " + entityCode));
    }

    private static String resolveEntityCode(Entity entity, ARAccount master) {
        if (entity != null && StringUtils.hasText(entity.getCode())) {
            return entity.getCode().strip();
        }
        if (master != null && StringUtils.hasText(master.getEntityCode())) {
            return master.getEntityCode().strip();
        }
        throw new BadRequestException("entityCode is required");
    }

    private static Account newArChargeConfig(Entity entity, ARAccount master, String chargeType) {
        Account config = new Account();
        config.setEntity(entity);
        config.setAccountNature(AccountNature.AR);
        config.setArAccount(master);
        config.setChargeType(chargeType);
        AccountVersionSupport.ensureInitialized(config);
        return config;
    }

    public static void syncMasterIdentity(ARAccount master, String accountName, String countryCode) {
        if (accountName != null && !accountName.isBlank()) {
            master.setAccountName(accountName.trim());
        }
        if (countryCode != null && !countryCode.isBlank()) {
            master.setCountryCode(countryCode.trim());
        }
    }

    public static boolean isArChargeConfig(Account account) {
        return account != null
                && AccountNature.AR.equals(account.getAccountNature())
                && account.getArAccount() != null;
    }

    public static String masterAccountNumber(Account account) {
        if (!isArChargeConfig(account)) {
            return null;
        }
        return account.getArAccount().getAccountNumber();
    }

    public static String masterEntityCode(Account account) {
        if (!isArChargeConfig(account)) {
            return account == null ? null : account.getEntityCode();
        }
        ARAccount master = account.getArAccount();
        return master.getEntityCode() != null ? master.getEntityCode() : account.getEntityCode();
    }

    public static boolean chargeTypesEqual(String left, String right) {
        if (Objects.equals(left, right)) {
            return true;
        }
        boolean leftBlank = left == null || left.isBlank();
        boolean rightBlank = right == null || right.isBlank();
        return leftBlank && rightBlank;
    }

    /** AR master currency; charge-config account is not used for AR. */
    public static String resolveRateCurrency(ARAccount master) {
        if (master != null && hasText(master.getRateCurrency())) {
            return master.getRateCurrency().trim();
        }
        return null;
    }

    /** AR master currency; charge-config account is not used for AR. */
    public static String resolveBillingCurrency(ARAccount master) {
        if (master != null && hasText(master.getBillingCurrency())) {
            return master.getBillingCurrency().trim();
        }
        return null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
