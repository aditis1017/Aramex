package com.customcharge.dashboard.service;

import com.customcharge.dashboard.constants.RateSheetMessageConstants;
import com.customcharge.dashboard.constants.ServiceCodesMessageConstants;
import com.customcharge.persistencelayer.entities.APAccount;
import com.customcharge.persistencelayer.entities.ARAccount;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.Entity;
import com.customcharge.persistencelayer.entities.RateSheet;
import com.customcharge.persistencelayer.enums.AccountNature;
import com.customcharge.persistencelayer.exception.BadRequestException;
import com.customcharge.persistencelayer.exception.ResourceNotFoundException;
import com.customcharge.persistencelayer.repository.APAccountRepository;
import com.customcharge.persistencelayer.repository.ARAccountRepository;
import com.customcharge.persistencelayer.repository.AccountRepository;
import com.customcharge.persistencelayer.repository.EntityRepository;
import com.customcharge.persistencelayer.repository.RateSheetRepository;
import com.customcharge.persistencelayer.util.AccountLoaders;
import com.customcharge.persistencelayer.util.AccountVersionSupport;
import com.customcharge.persistencelayer.util.ArAccountMasterSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Resolves the {@link Account} config row and {@link RateSheet} for rate-sheet CRUD using
 * account nature, entity, charge type, and optional account number.
 */
@Service
@RequiredArgsConstructor
public class AccountRateSheetResolver {

    private final AccountRepository accountRepository;
    private final ARAccountRepository arAccountRepository;
    private final APAccountRepository apAccountRepository;
    private final EntityRepository entityRepository;
    private final RateSheetRepository rateSheetRepository;

    @Transactional(readOnly = false)
    public RateSheet resolveOrCreateRateSheet(
            AccountNature accountNature,
            String accountNumber,
            String entityCode,
            String chargeType) {

        validateAccountContext(accountNature, accountNumber, entityCode, chargeType);

        Account config = resolveOrCreateChargeConfigAccount(accountNature, accountNumber, entityCode, chargeType);
        return rateSheetRepository
                .findByAccount_Id(config.getId())
                .orElseGet(() -> {
                    RateSheet rateSheet = new RateSheet();
                    rateSheet.setAccount(config);
                    return rateSheetRepository.save(rateSheet);
                });
    }

    /**
     * Validates account context when {@code rateSheetId} is omitted. Does not require the charge-config
     * {@link Account} row to exist (AR and entity settings may be created during resolution).
     */
    public void validateAccountContext(
            AccountNature accountNature,
            String accountNumber,
            String entityCode,
            String chargeType) {
        if (accountNature == null) {
            throw new BadRequestException(RateSheetMessageConstants.RATE_SHEET_ID_OR_ACCOUNT_NATURE_REQUIRED);
        }
        if (!StringUtils.hasText(entityCode)) {
            throw new BadRequestException(RateSheetMessageConstants.ENTITY_REQUIRED_WITHOUT_RATE_SHEET_ID);
        }
        if (!StringUtils.hasText(chargeType)) {
            throw new BadRequestException(RateSheetMessageConstants.CHARGE_TYPE_REQUIRED_WITHOUT_RATE_SHEET_ID);
        }
        if ((AccountNature.AR.equals(accountNature) || AccountNature.AP.equals(accountNature))
                && !StringUtils.hasText(accountNumber)) {
            throw new BadRequestException(RateSheetMessageConstants.ACCOUNT_NUMBER_REQUIRED_WITHOUT_RATE_SHEET_ID);
        }
    }

    /** @deprecated Use {@link #validateAccountContext}; account rows may be created during resolution. */
    @Deprecated
    public void requireAccountConfiguration(
            AccountNature accountNature,
            String accountNumber,
            String entityCode,
            String chargeType) {
        validateAccountContext(accountNature, accountNumber, entityCode, chargeType);
        if (!hasChargeConfigAccount(accountNature, accountNumber, entityCode, chargeType)) {
            throw new BadRequestException(RateSheetMessageConstants.ADD_ACCOUNT_CONFIGURATION_BEFORE_RATE_SHEET);
        }
    }

    public Account resolveChargeConfigAccount(
            AccountNature accountNature,
            String accountNumber,
            String entityCode,
            String chargeType) {
        return requireChargeConfigAccount(accountNature, accountNumber, entityCode, chargeType);
    }

    /**
     * Looks up an existing charge-config {@link Account} id without creating rows.
     * Used when {@code accountId} is omitted (e.g. rate-sheet dropdowns before account setup).
     */
    public Optional<UUID> findExistingChargeConfigAccountId(
            AccountNature accountNature,
            String accountNumber,
            String entityCode,
            String chargeType) {
        if (accountNature == null || !StringUtils.hasText(entityCode) || !StringUtils.hasText(chargeType)) {
            return Optional.empty();
        }
        String entity = entityCode.trim();
        String normalizedCharge = normalizeChargeType(chargeType);

        if (AccountNature.AR.equals(accountNature)) {
            if (!StringUtils.hasText(accountNumber)) {
                return Optional.empty();
            }
            return AccountLoaders.findArMaster(arAccountRepository, accountNumber, entity)
                    .flatMap(master -> AccountLoaders.findArChargeConfig(accountRepository, master, normalizedCharge))
                    .map(Account::getId);
        }
        if (AccountNature.ENTITY_SETTINGS.equals(accountNature)) {
            return AccountLoaders.findEntitySettingsConfig(accountRepository, entity, normalizedCharge)
                    .map(Account::getId);
        }
        if (AccountNature.AP.equals(accountNature)) {
            if (!StringUtils.hasText(accountNumber)) {
                return Optional.empty();
            }
            return AccountLoaders.findApMaster(apAccountRepository, accountNumber, entity)
                    .flatMap(master -> AccountLoaders.findApChargeConfig(accountRepository, master, normalizedCharge))
                    .map(Account::getId);
        }
        return Optional.empty();
    }

    private Account resolveOrCreateChargeConfigAccount(
            AccountNature accountNature,
            String accountNumber,
            String entityCode,
            String chargeType) {

        String entity = entityCode.trim();
        String normalizedCharge = normalizeChargeType(chargeType);

        if (AccountNature.AR.equals(accountNature)) {
            return resolveOrCreateArChargeConfigAccount(entity, normalizedCharge, accountNumber);
        }
        if (AccountNature.ENTITY_SETTINGS.equals(accountNature)) {
            return resolveOrCreateEntitySettingsAccount(entity, normalizedCharge);
        }
        return requireChargeConfigAccount(accountNature, accountNumber, entityCode, chargeType);
    }

    private Account resolveOrCreateEntitySettingsAccount(String entityCode, String chargeType) {
        Entity entityRef = requireActiveEntityByCode(entityCode);
        return AccountLoaders.findEntitySettingsConfig(accountRepository, entityCode, chargeType)
                .orElseGet(
                        () -> {
                            Account created = new Account();
                            created.setEntity(entityRef);
                            created.setAccountNature(AccountNature.ENTITY_SETTINGS);
                            created.setChargeType(chargeType);
                            AccountVersionSupport.ensureInitialized(created);
                            return accountRepository.save(created);
                        });
    }

    private Account resolveOrCreateArChargeConfigAccount(
            String entityCode, String chargeType, String accountNumber) {
        if (!StringUtils.hasText(accountNumber)) {
            throw new BadRequestException(RateSheetMessageConstants.ACCOUNT_NUMBER_REQUIRED_FOR_AR);
        }
        Entity entityRef = requireActiveEntityByCode(entityCode);
        ARAccount master =
                AccountLoaders.findArMaster(arAccountRepository, accountNumber, entityCode)
                        .orElseThrow(() -> new BadRequestException(RateSheetMessageConstants.ADD_ACCOUNT_INFO));
        return AccountLoaders.findArChargeConfig(accountRepository, master, chargeType)
                .orElseGet(
                        () ->
                                ArAccountMasterSupport.findOrCreateChargeConfig(
                                        accountRepository,
                                        entityRepository,
                                        master,
                                        entityRef,
                                        chargeType));
    }

    private Entity requireActiveEntityByCode(String entityCode) {
        if (entityCode == null || entityCode.isBlank()) {
            throw new BadRequestException(ServiceCodesMessageConstants.ENTITY_CODE_REQUIRED);
        }
        return entityRepository.findByCodeIgnoreCase(entityCode.trim())
                .filter(entity -> Boolean.TRUE.equals(entity.getActive()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        ServiceCodesMessageConstants.ENTITY_NOT_FOUND_OR_INACTIVE_PREFIX + entityCode));
    }

    private Account requireChargeConfigAccount(
            AccountNature accountNature,
            String accountNumber,
            String entityCode,
            String chargeType) {

        String entity = entityCode.trim();
        String normalizedCharge = normalizeChargeType(chargeType);

        if (AccountNature.AR.equals(accountNature)) {
            if (!StringUtils.hasText(accountNumber)) {
                throw new BadRequestException(RateSheetMessageConstants.ACCOUNT_NUMBER_REQUIRED_FOR_AR);
            }
            ARAccount master =
                    AccountLoaders.findArMaster(arAccountRepository, accountNumber, entity)
                            .orElseThrow(() -> new BadRequestException(RateSheetMessageConstants.ADD_ACCOUNT_INFO));
            return AccountLoaders.findArChargeConfig(accountRepository, master, normalizedCharge)
                    .orElseThrow(() -> new BadRequestException(
                            RateSheetMessageConstants.ADD_ACCOUNT_CONFIGURATION_FOR_CHARGE_TYPE_PREFIX
                                    + normalizedCharge + RateSheetMessageConstants.ADD_ACCOUNT_CONFIGURATION_FOR_CHARGE_TYPE_SUFFIX));
        }

        if (AccountNature.ENTITY_SETTINGS.equals(accountNature)) {
            return AccountLoaders.findEntitySettingsConfig(accountRepository, entity, normalizedCharge)
                    .orElseThrow(() -> new BadRequestException(
                            RateSheetMessageConstants.ADD_ENTITY_SETTINGS_FOR_CHARGE_TYPE_PREFIX
                                    + normalizedCharge + RateSheetMessageConstants.ADD_ENTITY_SETTINGS_FOR_CHARGE_TYPE_SUFFIX));
        }

        if (AccountNature.AP.equals(accountNature)) {
            if (!StringUtils.hasText(accountNumber)) {
                throw new BadRequestException(RateSheetMessageConstants.ACCOUNT_NUMBER_REQUIRED_FOR_AP);
            }
            APAccount master =
                    AccountLoaders.findApMaster(apAccountRepository, accountNumber, entity)
                            .orElseThrow(() -> new BadRequestException(RateSheetMessageConstants.ADD_ACCOUNT_INFO));
            return AccountLoaders.findApChargeConfig(accountRepository, master, normalizedCharge)
                    .orElseThrow(() -> new BadRequestException(
                            RateSheetMessageConstants.ADD_ACCOUNT_CONFIGURATION_FOR_CHARGE_TYPE_PREFIX
                                    + normalizedCharge + RateSheetMessageConstants.ADD_ACCOUNT_CONFIGURATION_FOR_CHARGE_TYPE_SUFFIX));
        }

        throw new BadRequestException(RateSheetMessageConstants.UNSUPPORTED_ACCOUNT_NATURE_PREFIX + accountNature);
    }

    private boolean hasChargeConfigAccount(
            AccountNature accountNature,
            String accountNumber,
            String entityCode,
            String chargeType) {
        String entity = entityCode.trim();
        String normalizedCharge = normalizeChargeType(chargeType);

        if (AccountNature.AR.equals(accountNature)) {
            if (!StringUtils.hasText(accountNumber)) {
                return false;
            }
            return AccountLoaders.findArMaster(arAccountRepository, accountNumber, entity)
                    .flatMap(master -> AccountLoaders.findArChargeConfig(accountRepository, master, normalizedCharge))
                    .isPresent();
        }
        if (AccountNature.ENTITY_SETTINGS.equals(accountNature)) {
            return AccountLoaders.findEntitySettingsConfig(accountRepository, entity, normalizedCharge).isPresent();
        }
        if (AccountNature.AP.equals(accountNature)) {
            if (!StringUtils.hasText(accountNumber)) {
                return false;
            }
            return AccountLoaders.findApMaster(apAccountRepository, accountNumber, entity)
                    .flatMap(master -> AccountLoaders.findApChargeConfig(accountRepository, master, normalizedCharge))
                    .isPresent();
        }
        return false;
    }

    private static String normalizeChargeType(String chargeType) {
        if (chargeType == null || chargeType.isBlank()) {
            return null;
        }
        return chargeType.trim().toUpperCase(Locale.ROOT);
    }
}
