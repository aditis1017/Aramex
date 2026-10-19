package com.customcharge.persistencelayer.util;

import com.customcharge.persistencelayer.accountRedis.AccountRedisAccountLookupService;
import com.customcharge.persistencelayer.entities.ARAccount;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.repository.ARAccountRepository;
import com.customcharge.persistencelayer.repository.AccountRepository;
import com.customcharge.persistencelayer.repository.EntityRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Optional;

/**
 * Resolves revenue charge {@link Account} rows for DTC and DTA. Party/default AR configs are
 * resolved or created when the AR master exists; otherwise the entity-settings account is returned.
 * The same account is used for service charges, rate sheets, and billing line attachment.
 */
@Component
public class RevenueBillingAccountResolver {

    public static final String CHARGE_TYPE_DTC = "DTC";
    public static final String CHARGE_TYPE_DTA = "DTA";

    private final AccountRepository accountRepository;
    private final ARAccountRepository arAccountRepository;
    private final EntityRepository entityRepository;
    private final ObjectProvider<AccountRedisAccountLookupService> accountRedisAccountLookupService;

    public RevenueBillingAccountResolver(
            AccountRepository accountRepository,
            ARAccountRepository arAccountRepository,
            EntityRepository entityRepository,
            ObjectProvider<AccountRedisAccountLookupService> accountRedisAccountLookupService) {
        this.accountRepository = accountRepository;
        this.arAccountRepository = arAccountRepository;
        this.entityRepository = entityRepository;
        this.accountRedisAccountLookupService = accountRedisAccountLookupService;
    }

    /**
     * Resolves the billing account for a revenue charge. Returns an AR charge-config when a master
     * can be matched; otherwise returns the entity-settings account for the charge type.
     */
    @Transactional
    public Optional<Account> resolveBillingAccount(
            String entityCode, String chargeType, String partyAccountNumber) {
        if (!StringUtils.hasText(entityCode) || !StringUtils.hasText(chargeType)) {
            return Optional.empty();
        }
        String normalizedEntityCode = entityCode.strip();
        String normalizedChargeType = normalizeChargeType(chargeType);

        return switch (normalizedChargeType) {
            case CHARGE_TYPE_DTC -> resolveDtcChargeAccount(normalizedEntityCode, partyAccountNumber);
            case CHARGE_TYPE_DTA -> resolveDtaChargeAccount(normalizedEntityCode);
            default -> Optional.empty();
        };
    }

    private Optional<Account> resolveDtcChargeAccount(String entityCode, String partyAccountNumber) {
        Optional<Account> entitySettings =
                loadEntitySettingsConfig(entityCode, CHARGE_TYPE_DTC);
        if (entitySettings.isEmpty()) {
            return Optional.empty();
        }

        if (StringUtils.hasText(partyAccountNumber)) {
            Optional<Account> partyAccount = resolveOrCreateArChargeConfig(
                    entityCode, CHARGE_TYPE_DTC, partyAccountNumber.strip(), entitySettings.get());
            if (partyAccount.isPresent()) {
                return partyAccount;
            }
        }

        String defaultCashAccount = entitySettings.get().getDefaultCashAccount();
        if (StringUtils.hasText(defaultCashAccount)) {
            Optional<Account> defaultCashBillingAccount = resolveOrCreateArChargeConfig(
                    entityCode, CHARGE_TYPE_DTC, defaultCashAccount.strip(), entitySettings.get());
            if (defaultCashBillingAccount.isPresent()) {
                return defaultCashBillingAccount;
            }
        }

        return entitySettings;
    }

    private Optional<Account> resolveDtaChargeAccount(String entityCode) {
        Optional<Account> entitySettings =
                loadEntitySettingsConfig(entityCode, CHARGE_TYPE_DTA);
        if (entitySettings.isEmpty()) {
            return Optional.empty();
        }

        String defaultArAccountNumber = entitySettings.get().getDefaultArAccountNumber();
        if (StringUtils.hasText(defaultArAccountNumber)) {
            Optional<Account> defaultArAccount = resolveOrCreateArChargeConfigByArAccountNumber(
                    entityCode, CHARGE_TYPE_DTA, defaultArAccountNumber.strip(), entitySettings.get());
            if (defaultArAccount.isPresent()) {
                return defaultArAccount;
            }
        }

        return entitySettings;
    }

    private Optional<Account> resolveOrCreateArChargeConfig(
            String entityCode, String chargeType, String masterAccountNumber, Account entitySettings) {
        Optional<Account> fromRedis = tryResolveArChargeAccount(masterAccountNumber, entityCode, chargeType);
        if (fromRedis.isPresent()) {
            return fromRedis;
        }

        Optional<ARAccount> arMaster =
                AccountLoaders.findArMaster(arAccountRepository, masterAccountNumber, entityCode);
        if (arMaster.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(ArAccountMasterSupport.findOrCreateChargeConfig(
                accountRepository, entityRepository, arMaster.get(), entitySettings.getEntity(), chargeType));
    }

    private Optional<Account> resolveOrCreateArChargeConfigByArAccountNumber(
            String entityCode, String chargeType, String arAccountNumber, Account entitySettings) {
        Optional<ARAccount> arMaster = arAccountRepository.findByArAccountNumberAndEntityCode(
                arAccountNumber.strip(), entityCode.strip());
        if (arMaster.isEmpty()) {
            return Optional.empty();
        }
        Optional<Account> fromRedis = tryResolveArChargeAccount(arMaster.get().getAccountNumber(), entityCode, chargeType);
        if (fromRedis.isPresent()) {
            return fromRedis;
        }
        return Optional.of(ArAccountMasterSupport.findOrCreateChargeConfig(
                accountRepository, entityRepository, arMaster.get(), entitySettings.getEntity(), chargeType));
    }

    private Optional<Account> tryResolveEntitySettings(String entityCode, String chargeType) {
        AccountRedisAccountLookupService lookup = accountRedisAccountLookupService.getIfAvailable();
        if (lookup == null) {
            return Optional.empty();
        }
        return lookup.tryResolveEntitySettings(entityCode, chargeType);
    }

    private Optional<Account> tryResolveArChargeAccount(
            String accountNumber, String entityCode, String chargeType) {
        AccountRedisAccountLookupService lookup = accountRedisAccountLookupService.getIfAvailable();
        if (lookup == null) {
            return Optional.empty();
        }
        return lookup.tryResolveArChargeAccount(accountNumber, entityCode, chargeType);
    }

    private Optional<Account> loadEntitySettingsConfig(String entityCode, String chargeType) {
        return tryResolveEntitySettings(entityCode, chargeType)
                .or(() -> AccountLoaders.findEntitySettingsConfig(accountRepository, entityCode, chargeType));
    }

    private static String normalizeChargeType(String chargeType) {
        return chargeType.strip().toUpperCase(Locale.ROOT);
    }
}
