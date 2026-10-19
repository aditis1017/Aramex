package com.customcharge.persistencelayer.util;

import com.customcharge.persistencelayer.entities.APAccount;
import com.customcharge.persistencelayer.entities.ARAccount;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.enums.AccountNature;
import com.customcharge.persistencelayer.exception.BadRequestException;
import com.customcharge.persistencelayer.exception.ResourceNotFoundException;
import com.customcharge.persistencelayer.repository.APAccountRepository;
import com.customcharge.persistencelayer.repository.ARAccountRepository;
import com.customcharge.persistencelayer.repository.AccountRepository;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import static com.customcharge.persistencelayer.enums.AccountNature.ENTITY_SETTINGS;

/**
 * Resolves {@link Account} rows when {@code entity} may be omitted (single match only).
 */
public final class AccountLoaders {

    private AccountLoaders() {}

    /**
     * Charge calculation uses a single account; when multiple configs match, keep the first.
     */
    public static List<Account> selectFirstAccount(List<Account> accounts) {
        if (accounts == null || accounts.isEmpty()) {
            return List.of();
        }
        return List.of(accounts.get(0));
    }

    public static Account loadAccountOrThrow(
            AccountRepository accountRepository,
            String accountNumber,
            AccountNature accountNature,
            String entityCode,
            String chargeType) {

        if (Objects.isNull(accountNature)) {
            throw new BadRequestException("accountNature is required");
        }
        String trimmedChargeType = chargeType == null || chargeType.isBlank() ? null : chargeType.trim();
        String trimmedNumber = Objects.requireNonNullElse(accountNumber, "").trim();

        if (!ENTITY_SETTINGS.equals(accountNature) && trimmedNumber.isEmpty()) {
            throw new BadRequestException("accountNumber is required");
        }

        if (ENTITY_SETTINGS.equals(accountNature) && trimmedNumber.isEmpty()) {
            if (entityCode == null || entityCode.isBlank()) {
                throw new BadRequestException("entity is required for ENTITY_SETTINGS when accountNumber is absent");
            }
            String e = entityCode.trim();
            return resolveEntitySettings(accountRepository, e, accountNature, trimmedChargeType);
        }

        if (entityCode != null && !entityCode.isBlank()) {
            String e = entityCode.trim();
            return resolveWithEntity(accountRepository, trimmedNumber, e, accountNature, trimmedChargeType);
        }

        List<Account> matches =
                trimmedChargeType == null
                        ? accountRepository.findByAccountNumberAndAccountNature(trimmedNumber, accountNature)
                        : accountRepository.findByAccountNumberAndAccountNatureAndChargeType(
                                trimmedNumber, accountNature, trimmedChargeType);
        if (matches.isEmpty()) {
            throw new ResourceNotFoundException("Account not found");
        }
        if (matches.size() > 1) {
            throw new BadRequestException(
                    "entity is required when multiple accounts exist for the same account number and nature");
        }
        return matches.get(0);
    }

    public static Optional<ARAccount> findArMaster(
            ARAccountRepository arAccountRepository, String accountNumber, String entityCode) {
        if (!hasText(accountNumber) || !hasText(entityCode)) {
            return Optional.empty();
        }
        return arAccountRepository.findByAccountNumberAndEntityCode(accountNumber.trim(), entityCode.trim());
    }

    public static Optional<APAccount> findApMaster(
            APAccountRepository apAccountRepository, String accountNumber, String entityCode) {
        if (!hasText(accountNumber) || !hasText(entityCode)) {
            return Optional.empty();
        }
        return apAccountRepository.findByAccountNumberAndEntityCode(accountNumber.trim(), entityCode.trim());
    }

    public static Optional<Account> findArChargeConfig(
            AccountRepository accountRepository,
            ARAccount master,
            String chargeType) {
        if (master == null || master.getId() == null) {
            return Optional.empty();
        }
        String normalized = normalizeChargeType(chargeType);
        return accountRepository.findByArAccountIdAndChargeType(master.getId(), normalized);
    }

    public static Optional<Account> findApChargeConfig(
            AccountRepository accountRepository,
            APAccount master,
            String chargeType) {
        if (master == null || master.getId() == null) {
            return Optional.empty();
        }
        String normalized = normalizeChargeType(chargeType);
        return accountRepository.findByApAccountIdAndChargeType(master.getId(), normalized);
    }

    public static Optional<Account> findEntitySettingsConfig(
            AccountRepository accountRepository, String entityCode, String chargeType) {
        if (!hasText(entityCode)) {
            return Optional.empty();
        }
        String normalized = normalizeChargeType(chargeType);
        return accountRepository.findByAccountNumberIsNullAndEntityCodeAndAccountNatureAndChargeType(
                entityCode.trim(), AccountNature.ENTITY_SETTINGS, normalized);
    }

    private static Account resolveEntitySettings(
            AccountRepository accountRepository,
            String entityCode,
            AccountNature accountNature,
            String trimmedChargeType) {
        Optional<Account> row =
                trimmedChargeType == null
                        ? accountRepository.findByAccountNumberIsNullAndEntityCodeAndAccountNature(
                                entityCode, accountNature)
                        : accountRepository.findByAccountNumberIsNullAndEntityCodeAndAccountNatureAndChargeType(
                                entityCode, accountNature, trimmedChargeType);
        return row.orElseThrow(() -> new ResourceNotFoundException("Account not found"));
    }

    private static Account resolveWithEntity(
            AccountRepository accountRepository,
            String trimmedNumber,
            String entityCode,
            AccountNature accountNature,
            String trimmedChargeType) {
        Optional<Account> row =
                switch (accountNature) {
                    case AP -> accountRepository.findByAccountNumberAndEntityCodeAndAccountNature(
                            trimmedNumber, entityCode, AccountNature.AP, trimmedChargeType);
                    case AR -> accountRepository.findByAccountNumberAndEntityCodeAndAccountNatureAndChargeType(
                            trimmedNumber, entityCode, AccountNature.AR, trimmedChargeType);
                    default -> Optional.empty();
                };
        return row.orElseThrow(() -> new ResourceNotFoundException("Account not found"));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String normalizeChargeType(String chargeType) {
        if (chargeType == null || chargeType.isBlank()) {
            return null;
        }
        return chargeType.trim().toUpperCase(Locale.ROOT);
    }
}
