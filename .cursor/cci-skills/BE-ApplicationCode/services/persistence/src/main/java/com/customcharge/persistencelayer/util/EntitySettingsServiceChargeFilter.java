package com.customcharge.persistencelayer.util;

import com.customcharge.persistencelayer.accountRedis.AccountRedisChargeDataService;
import com.customcharge.persistencelayer.dto.ServiceChargeQueryCriteria;
import com.customcharge.persistencelayer.dto.ServiceChargeResponse;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.ServiceCharge;
import com.customcharge.persistencelayer.entities.ServiceCodes;
import com.customcharge.persistencelayer.enums.AccountNature;
import com.customcharge.persistencelayer.repository.AccountRepository;
import com.customcharge.persistencelayer.repository.ServiceChargeRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * For AR billing accounts, removes service codes that are explicitly inactive on the matching
 * entity-settings account (Redis or DB). Missing entity-settings rows do not remove AR codes.
 */
@Component
public class EntitySettingsServiceChargeFilter {

    private final AccountRepository accountRepository;
    private final ServiceChargeRepository serviceChargeRepository;
    private final ObjectProvider<AccountRedisChargeDataService> accountRedisChargeDataService;

    public EntitySettingsServiceChargeFilter(
            AccountRepository accountRepository,
            ServiceChargeRepository serviceChargeRepository,
            ObjectProvider<AccountRedisChargeDataService> accountRedisChargeDataService) {
        this.accountRepository = accountRepository;
        this.serviceChargeRepository = serviceChargeRepository;
        this.accountRedisChargeDataService = accountRedisChargeDataService;
    }

    public List<ServiceChargeResponse> filterArResponsesByEntitySettings(
            ServiceChargeQueryCriteria criteria, List<ServiceChargeResponse> responses) {
        if (criteria == null || responses == null || responses.isEmpty()) {
            return responses == null ? List.of() : responses;
        }
        Account account = criteria.getAccount();
        if (account == null || !AccountNature.AR.equals(account.getAccountNature())) {
            return responses;
        }
        if (!StringUtils.hasText(criteria.getEntityCode())) {
            return responses;
        }

        String entityCode = criteria.getEntityCode().strip();
        List<String> chargeTypes = resolveChargeTypes(criteria, account);
        if (chargeTypes.isEmpty()) {
            return responses;
        }

        Set<String> inactiveOnEntity = new LinkedHashSet<>();
        for (String chargeType : chargeTypes) {
            inactiveOnEntity.addAll(inactiveServiceCodesOnEntitySettings(entityCode, chargeType));
        }
        if (inactiveOnEntity.isEmpty()) {
            return responses;
        }

        return responses.stream()
                .filter(response -> {
                    if (response == null || !StringUtils.hasText(response.getServiceCode())) {
                        return true;
                    }
                    return !inactiveOnEntity.contains(normalizeServiceCode(response.getServiceCode()));
                })
                .toList();
    }

    private Set<String> inactiveServiceCodesOnEntitySettings(String entityCode, String chargeType) {
        String normalizedChargeType = normalizeChargeType(chargeType);
        Optional<Account> entitySettings = accountRepository
                .findByAccountNumberIsNullAndEntityCodeAndAccountNatureAndChargeType(
                        entityCode, AccountNature.ENTITY_SETTINGS, normalizedChargeType);
        if (entitySettings.isEmpty()) {
            return Set.of();
        }

        Set<String> inactive = new LinkedHashSet<>();
        for (ServiceCharge charge : loadEntitySettingsCharges(entitySettings.get(), entityCode, normalizedChargeType)
                .values()) {
            if (charge == null || !Boolean.FALSE.equals(charge.getActive())) {
                continue;
            }
            ServiceCodes master = charge.getServiceCodes();
            if (master != null && StringUtils.hasText(master.getServiceCode())) {
                inactive.add(normalizeServiceCode(master.getServiceCode()));
            }
        }
        return inactive;
    }

    private Map<UUID, ServiceCharge> loadEntitySettingsCharges(
            Account entitySettings, String entityCode, String chargeType) {
        AccountRedisChargeDataService redis = accountRedisChargeDataService.getIfAvailable();
        if (redis != null) {
            Map<UUID, ServiceCharge> fromRedis =
                    redis.loadServiceChargesByMasterId(entitySettings, entityCode, chargeType);
            if (!fromRedis.isEmpty()
                    || redis.readEntitySettingsChargeTypeData(entityCode, chargeType)
                            .map(AccountRedisChargeDataService::isChargeDataSynced)
                            .orElse(false)) {
                return fromRedis;
            }
        }
        return loadEntitySettingsChargesFromDb(entitySettings, entityCode, chargeType);
    }

    private Map<UUID, ServiceCharge> loadEntitySettingsChargesFromDb(
            Account entitySettings, String entityCode, String chargeType) {
        List<ServiceCharge> charges = serviceChargeRepository.findByAccountWithActiveServiceCodes(
                entitySettings, entityCode, chargeType);
        Map<UUID, ServiceCharge> indexed = new LinkedHashMap<>();
        for (ServiceCharge charge : charges) {
            if (charge.getServiceCodes() != null && charge.getServiceCodes().getId() != null) {
                indexed.put(charge.getServiceCodes().getId(), charge);
            }
        }
        return indexed;
    }

    private static List<String> resolveChargeTypes(ServiceChargeQueryCriteria criteria, Account arAccount) {
        List<String> fromCriteria = normalizeChargeTypesList(criteria.getChargeTypes());
        if (!fromCriteria.isEmpty()) {
            return fromCriteria;
        }
        if (arAccount != null && StringUtils.hasText(arAccount.getChargeType())) {
            return List.of(normalizeChargeType(arAccount.getChargeType()));
        }
        return List.of();
    }

    private static List<String> normalizeChargeTypesList(List<String> chargeTypes) {
        if (chargeTypes == null || chargeTypes.isEmpty()) {
            return List.of();
        }
        return chargeTypes.stream()
                .filter(StringUtils::hasText)
                .map(EntitySettingsServiceChargeFilter::normalizeChargeType)
                .distinct()
                .toList();
    }

    private static String normalizeChargeType(String chargeType) {
        return chargeType == null ? "" : chargeType.strip().toUpperCase(Locale.ROOT);
    }

    private static String normalizeServiceCode(String serviceCode) {
        return serviceCode == null ? "" : serviceCode.strip().toUpperCase(Locale.ROOT);
    }
}
