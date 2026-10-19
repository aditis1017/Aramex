package com.customcharge.shipment.service.charge;

import com.customcharge.persistencelayer.accountRedis.AccountRedisAccountLookupService;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.enums.AccountNature;
import com.customcharge.persistencelayer.repository.AccountRepository;
import com.customcharge.persistencelayer.util.ArAccountMasterSupport;
import com.customcharge.persistencelayer.util.RevenueBillingAccountResolver;
import com.customcharge.shipment.constants.ShipmentChargeTypeSupport;
import com.customcharge.shipment.service.charge.processing.ShipmentProcessingContext;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * Resolves accounts for service-charge, rate-sheet, and billing lookups. DTC and DTA share one
 * resolver ({@link RevenueBillingAccountResolver}) so the same account drives service codes,
 * rate sheets, and charge line attachment. Cost AP charges (ECD, ECS, EC3) use entity settings only.
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ServiceChargeAccountResolver {

    AccountRepository accountRepository;
    ObjectProvider<AccountRedisAccountLookupService> accountRedisAccountLookupService;
    DtaDefaultArAccountResolver dtaDefaultArAccountResolver;
    RevenueBillingAccountResolver revenueBillingAccountResolver;
    ObjectProvider<PartyArAccountResolver> partyArAccountResolver;

    public Optional<Account> resolve(Shipment shipment, String entityCode, String chargeCode) {
        if (!StringUtils.hasText(entityCode) || !StringUtils.hasText(chargeCode)) {
            return Optional.empty();
        }
        String resolvedEntityCode = entityCode.strip();
        String resolvedChargeCode = ShipmentChargeTypeSupport.normalize(chargeCode);

        if (ShipmentChargeTypeSupport.DTC.equals(resolvedChargeCode)) {
            return cachedRevenueChargeAccount(shipment, resolvedEntityCode, resolvedChargeCode);
        }
        if (DtaDefaultArAccountResolver.DTA.equals(resolvedChargeCode)) {
            return cachedDtaChargeAccount(resolvedEntityCode);
        }
        if (ShipmentChargeTypeSupport.usesEntitySettingsAccountOnly(resolvedChargeCode)) {
            return cachedEntitySettings(resolvedEntityCode, resolvedChargeCode);
        }

        Optional<String> partyAccountNumber =
                ShipmentPartyAccountSupport.resolvePartyAccountNumber(shipment, resolvedChargeCode);
        if (partyAccountNumber.isPresent()) {
            Optional<Account> arAccount = cachedArAccount(
                    partyAccountNumber.get(), resolvedEntityCode, resolvedChargeCode);
            if (arAccount.isPresent()) {
                return arAccount;
            }
        }

        return cachedEntitySettings(resolvedEntityCode, resolvedChargeCode);
    }

    /**
     * Billing account for charge line attachment. DTC and DTA use the same unified resolver as
     * {@link #resolve(Shipment, String, String)}. DTS/DT3 use party AR with entity-settings fallback.
     */
    public Optional<Account> resolveBillingAccount(
            Shipment shipment, String entityCode, String chargeCode, Account fallback) {
        if (!StringUtils.hasText(entityCode) || !StringUtils.hasText(chargeCode)) {
            return Optional.empty();
        }
        String resolvedEntityCode = entityCode.strip();
        String resolvedChargeCode = ShipmentChargeTypeSupport.normalize(chargeCode);

        if (DtaDefaultArAccountResolver.DTA.equals(resolvedChargeCode)) {
            return cachedDtaChargeAccount(resolvedEntityCode);
        }
        if (ShipmentChargeTypeSupport.DTC.equals(resolvedChargeCode)) {
            return cachedRevenueChargeAccount(shipment, resolvedEntityCode, resolvedChargeCode);
        }
        if (ShipmentChargeTypeSupport.DTS.equals(resolvedChargeCode)
                || ShipmentChargeTypeSupport.DT3.equals(resolvedChargeCode)) {
            Optional<Account> partyAccount =
                    partyArAccountResolver.getObject().resolveOrCreate(shipment, resolvedEntityCode, resolvedChargeCode);
            return partyAccount.isPresent() ? partyAccount : Optional.ofNullable(fallback);
        }
        return Optional.ofNullable(fallback);
    }

    /**
     * AR party billing account only (no entity-settings fallback). Used for revenue tax lookup.
     */
    public Optional<Account> resolveArBillingAccount(Shipment shipment, String entityCode, String chargeCode) {
        if (!StringUtils.hasText(entityCode) || !StringUtils.hasText(chargeCode)) {
            return Optional.empty();
        }
        String resolvedChargeCode = ShipmentChargeTypeSupport.normalize(chargeCode);
        if (DtaDefaultArAccountResolver.DTA.equalsIgnoreCase(resolvedChargeCode)) {
            return cachedDtaChargeAccount(entityCode.strip())
                    .filter(ArAccountMasterSupport::isArChargeConfig);
        }
        if (ShipmentChargeTypeSupport.DTC.equals(resolvedChargeCode)) {
            return cachedRevenueChargeAccount(shipment, entityCode.strip(), resolvedChargeCode)
                    .filter(ArAccountMasterSupport::isArChargeConfig);
        }
        if (ShipmentChargeTypeSupport.DTS.equals(resolvedChargeCode)
                || ShipmentChargeTypeSupport.DT3.equals(resolvedChargeCode)) {
            return partyArAccountResolver.getObject().resolveOrCreate(shipment, entityCode.strip(), resolvedChargeCode);
        }
        Optional<String> partyAccountNumber =
                ShipmentPartyAccountSupport.resolvePartyAccountNumber(shipment, resolvedChargeCode);
        if (partyAccountNumber.isEmpty()) {
            return Optional.empty();
        }
        return cachedArAccount(partyAccountNumber.get(), entityCode.strip(), resolvedChargeCode);
    }

    public Optional<Account> resolveEntitySettings(String entityCode, String chargeCode) {
        if (!StringUtils.hasText(entityCode) || !StringUtils.hasText(chargeCode)) {
            return Optional.empty();
        }
        return cachedEntitySettings(entityCode.strip(), ShipmentChargeTypeSupport.normalize(chargeCode));
    }

    public Optional<Account> resolveArChargeAccount(String accountNumber, String entityCode, String chargeCode) {
        if (!StringUtils.hasText(accountNumber) || !StringUtils.hasText(entityCode) || !StringUtils.hasText(chargeCode)) {
            return Optional.empty();
        }
        return cachedArAccount(
                accountNumber.strip(),
                entityCode.strip(),
                ShipmentChargeTypeSupport.normalize(chargeCode));
    }

    private Optional<Account> cachedRevenueChargeAccount(
            Shipment shipment, String entityCode, String chargeCode) {
        String partyAccountNumber = ShipmentPartyAccountSupport.resolvePartyAccountNumber(shipment, chargeCode)
                .orElse(null);
        String cacheKey = "REVENUE_CHARGE:"
                + entityCode + ':' + chargeCode + ':' + (partyAccountNumber == null ? "" : partyAccountNumber);
        return ShipmentProcessingContext.cachedAccount(
                cacheKey,
                () -> revenueBillingAccountResolver.resolveBillingAccount(
                        entityCode, chargeCode, partyAccountNumber));
    }

    private Optional<Account> cachedDtaChargeAccount(String entityCode) {
        return ShipmentProcessingContext.cachedAccount(
                "DTA_CHARGE:" + entityCode,
                () -> dtaDefaultArAccountResolver.resolveOrCreate(entityCode));
    }

    private Optional<Account> cachedEntitySettings(String entityCode, String chargeCode) {
        return ShipmentProcessingContext.cachedAccount(
                "ENTITY_SETTINGS:" + entityCode + ':' + chargeCode,
                () -> tryResolveEntitySettings(entityCode, chargeCode)
                        .or(() -> accountRepository.findByAccountNumberIsNullAndEntityCodeAndAccountNatureAndChargeType(
                                entityCode, AccountNature.ENTITY_SETTINGS, chargeCode)));
    }

    private Optional<Account> cachedArAccount(String accountNumber, String entityCode, String chargeCode) {
        return ShipmentProcessingContext.cachedAccount(
                "AR:" + accountNumber + ':' + entityCode + ':' + chargeCode,
                () -> tryResolveArChargeAccount(accountNumber, entityCode, chargeCode)
                        .or(() -> accountRepository.findByAccountNumberAndEntityCodeAndAccountNatureAndChargeType(
                                accountNumber, entityCode, AccountNature.AR, chargeCode)));
    }

    private Optional<Account> tryResolveEntitySettings(String entityCode, String chargeCode) {
        AccountRedisAccountLookupService lookup = accountRedisAccountLookupService.getIfAvailable();
        return lookup == null ? Optional.empty() : lookup.tryResolveEntitySettings(entityCode, chargeCode);
    }

    private Optional<Account> tryResolveArChargeAccount(
            String accountNumber, String entityCode, String chargeCode) {
        AccountRedisAccountLookupService lookup = accountRedisAccountLookupService.getIfAvailable();
        return lookup == null ? Optional.empty() : lookup.tryResolveArChargeAccount(accountNumber, entityCode, chargeCode);
    }
}
