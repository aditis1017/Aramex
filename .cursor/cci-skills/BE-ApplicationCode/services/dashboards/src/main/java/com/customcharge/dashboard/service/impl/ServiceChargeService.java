package com.customcharge.dashboard.service.impl;

import com.customcharge.dashboard.constants.CacheKeyConstants;
import com.customcharge.dashboard.constants.DashboardMessageConstants;
import com.customcharge.dashboard.constants.ServiceChargeMessageConstants;
import com.customcharge.dashboard.dto.ServiceChargeResponse;
import com.customcharge.dashboard.dto.ServiceChargeUpdateRequest;
import com.customcharge.dashboard.dto.ServiceCodeDropdownOption;
import com.customcharge.dashboard.service.AccountService;
import com.customcharge.dashboard.service.ServiceChargeRateSheetUsageValidator;
import com.customcharge.dashboard.util.ServiceCodeActiveSupport;
import com.customcharge.persistencelayer.dto.response.MessageResponse;
import com.customcharge.persistencelayer.entities.ARAccount;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.Entity;
import com.customcharge.persistencelayer.entities.RateSheet;
import com.customcharge.persistencelayer.entities.ServiceCharge;
import com.customcharge.persistencelayer.entities.ServiceCodes;
import com.customcharge.persistencelayer.enums.AccountNature;
import com.customcharge.persistencelayer.exception.BadRequestException;
import com.customcharge.persistencelayer.exception.ConflictException;
import com.customcharge.persistencelayer.exception.ResourceNotFoundException;
import com.customcharge.persistencelayer.repository.ARAccountRepository;
import com.customcharge.persistencelayer.repository.AccountRepository;
import com.customcharge.persistencelayer.repository.EntityRepository;
import com.customcharge.persistencelayer.repository.RateSheetRepository;
import com.customcharge.persistencelayer.repository.ServiceChargeRepository;
import com.customcharge.persistencelayer.util.AccountLoaders;
import com.customcharge.persistencelayer.util.AccountVersionSupport;
import com.customcharge.persistencelayer.util.ArAccountMasterSupport;
import com.customcharge.persistencelayer.util.ServiceChargeDefaultAccountNumberSupport;
import com.customcharge.persistencelayer.util.ServiceChargeTaxResolutionSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.customcharge.dashboard.cache.DashboardCacheNames;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServiceChargeService {

    private final AccountRepository accountRepository;
    private final ARAccountRepository arAccountRepository;
    private final EntityRepository entityRepository;
    private final RateSheetRepository rateSheetRepository;
    private final ServiceChargeRepository serviceChargeRepository;
    private final ServiceCodesService serviceCodesService;
    private final ServiceChargeRateSheetUsageValidator rateSheetUsageValidator;
    private final AccountService accountService;

    @Transactional(readOnly = false)
    @Caching(evict = {
            @CacheEvict(cacheNames = DashboardCacheNames.SERVICE_CHARGES, allEntries = true),
            @CacheEvict(cacheNames = DashboardCacheNames.RATE_SHEET_DROPDOWN, allEntries = true),
            @CacheEvict(cacheNames = DashboardCacheNames.ACCOUNT_DETAILS, allEntries = true)
    })
    public MessageResponse update(List<ServiceChargeUpdateRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new BadRequestException(ServiceChargeMessageConstants.AT_LEAST_ONE_UPDATE_REQUIRED);
        }
        requests.forEach(this::validateUpsertRequest);

        Set<String> entityRedisKeys = new LinkedHashSet<>();
        Set<String> arRedisKeys = new LinkedHashSet<>();
        for (ServiceChargeUpdateRequest request : requests) {
            Account account = resolveAccount(request);
            updateOne(request);
            recordRedisSyncTargetForAccount(account, entityRedisKeys, arRedisKeys);
        }
        syncRedisTargets(entityRedisKeys, arRedisKeys);

        return new MessageResponse(DashboardMessageConstants.SAVED_SUCCESSFULLY);
    }

    private void validateUpsertRequest(ServiceChargeUpdateRequest request) {
        if (request.getAccountId() != null) {
            if (request.getId() == null) {
                ServiceCodesService.requireEntityCode(request.getEntityCode());
                ServiceCodesService.requireChargeType(request.getChargeType());
                if (!StringUtils.hasText(request.getServiceCode())) {
                    throw new BadRequestException(ServiceChargeMessageConstants.SERVICE_CODE_REQUIRED_ON_CREATE);
                }
            }
            return;
        }

        if (request.getAccountNature() == null) {
            throw new BadRequestException(ServiceChargeMessageConstants.ACCOUNT_NATURE_REQUIRED_WITHOUT_ID);
        }
        if (!AccountNature.AR.equals(request.getAccountNature())
                && !AccountNature.ENTITY_SETTINGS.equals(request.getAccountNature())) {
            throw new BadRequestException(ServiceChargeMessageConstants.ACCOUNT_NATURE_MUST_BE_AR_OR_ENTITY_SETTINGS);
        }
        ServiceCodesService.requireEntityCode(request.getEntityCode());
        ServiceCodesService.requireChargeType(request.getChargeType());
        if (AccountNature.AR.equals(request.getAccountNature())
                && !StringUtils.hasText(request.getAccountNumber())) {
            throw new BadRequestException(ServiceChargeMessageConstants.ACCOUNT_NUMBER_REQUIRED_FOR_AR);
        }
        if (request.getId() == null && !StringUtils.hasText(request.getServiceCode())) {
            throw new BadRequestException(ServiceChargeMessageConstants.SERVICE_CODE_REQUIRED_ON_CREATE);
        }
    }

    private void updateOne(ServiceChargeUpdateRequest request) {
        Account account = resolveAccount(request);

        if (request.getId() == null) {
            createOne(request, account);
            return;
        }

        ServiceCharge charge = serviceChargeRepository.findByIdWithActiveServiceCodes(request.getId())
                .orElseThrow(() -> new ResourceNotFoundException(DashboardMessageConstants.SERVICE_CHARGE_NOT_FOUND));

        if (!Objects.equals(charge.getAccount().getId(), account.getId())) {
            throw new BadRequestException(ServiceChargeMessageConstants.SERVICE_CHARGE_ACCOUNT_MISMATCH);
        }

        if (Boolean.FALSE.equals(request.getActive()) && !Boolean.FALSE.equals(charge.getActive())) {
            String serviceCode = charge.getServiceCodes() != null
                    ? charge.getServiceCodes().getServiceCode()
                    : null;
            rateSheetUsageValidator.assertNotUsedOnRateSheet(account.getId(), serviceCode);
        }

        applyChargeFields(charge, request);
        serviceCodesService.updateEntitySettingsActiveFlag(
                account,
                charge.getServiceCodes(),
                resolveAccountChargeType(account, request),
                request.getActive());
        serviceChargeRepository.save(charge);
    }

    private Account resolveAccount(ServiceChargeUpdateRequest request) {
        if (request.getAccountId() != null) {
            return accountRepository.findById(request.getAccountId())
                    .orElseThrow(() -> new ResourceNotFoundException(DashboardMessageConstants.ACCOUNT_NOT_FOUND));
        }
        return resolveAccountWithoutId(request);
    }

    private Account resolveAccountWithoutId(ServiceChargeUpdateRequest request) {
        AccountNature nature = request.getAccountNature();
        String entityCode = ServiceCodesService.requireEntityCode(request.getEntityCode());
        String chargeType = ServiceCodesService.requireChargeType(request.getChargeType());

        Account account =
                switch (nature) {
                    case ENTITY_SETTINGS -> resolveOrCreateEntitySettingsAccount(entityCode, chargeType);
                    case AR -> resolveArChargeConfigAccount(entityCode, chargeType, request.getAccountNumber());
                    default -> throw new BadRequestException(
                            ServiceChargeMessageConstants.ACCOUNT_NATURE_MUST_BE_AR_OR_ENTITY_SETTINGS);
                };

        if (!nature.equals(account.getAccountNature())) {
            throw new BadRequestException(ServiceChargeMessageConstants.RESOLVED_ACCOUNT_NATURE_MISMATCH);
        }
        return account;
    }

    private Account resolveOrCreateEntitySettingsAccount(String entityCode, String chargeType) {
        Entity entityRef = requireActiveEntityByCode(entityCode);
        Account account =
                AccountLoaders.findEntitySettingsConfig(accountRepository, entityCode, chargeType)
                        .orElseGet(
                                () -> {
                                    Account created = new Account();
                                    created.setEntity(entityRef);
                                    created.setAccountNature(AccountNature.ENTITY_SETTINGS);
                                    created.setChargeType(chargeType);
                                    AccountVersionSupport.ensureInitialized(created);
                                    return accountRepository.save(created);
                                });
        ensureRateSheet(account);
        return account;
    }

    private Account resolveArChargeConfigAccount(
            String entityCode, String chargeType, String accountNumber) {
        if (!StringUtils.hasText(accountNumber)) {
            throw new BadRequestException(ServiceChargeMessageConstants.ACCOUNT_NUMBER_REQUIRED_FOR_AR);
        }
        Entity entityRef = requireActiveEntityByCode(entityCode);
        ARAccount master =
                AccountLoaders.findArMaster(arAccountRepository, accountNumber, entityCode)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                ServiceChargeMessageConstants.AR_ACCOUNT_MASTER_NOT_FOUND));
        Account account =
                AccountLoaders.findArChargeConfig(accountRepository, master, chargeType)
                        .orElseGet(
                                () ->
                                        ArAccountMasterSupport.findOrCreateChargeConfig(
                                                accountRepository,
                                                entityRepository,
                                                master,
                                                entityRef,
                                                chargeType));
        ensureRateSheet(account);
        return account;
    }

    private Entity requireActiveEntityByCode(String entityCode) {
        if (entityCode == null || entityCode.isBlank()) {
            throw new BadRequestException(ServiceChargeMessageConstants.ENTITY_CODE_REQUIRED);
        }
        return entityRepository.findByCodeIgnoreCase(entityCode.trim())
                .filter(entity -> Boolean.TRUE.equals(entity.getActive()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        ServiceChargeMessageConstants.ENTITY_NOT_FOUND_OR_INACTIVE_PREFIX + entityCode));
    }

    private void ensureRateSheet(Account account) {
        if (account.getId() == null) {
            return;
        }
        if (rateSheetRepository.findByAccount_Id(account.getId()).isPresent()) {
            return;
        }
        RateSheet rateSheet = new RateSheet();
        rateSheet.setAccount(account);
        rateSheetRepository.save(rateSheet);
    }

    private void recordRedisSyncTargetForAccount(
            Account account, Set<String> entityRedisKeys, Set<String> arRedisKeys) {
        if (account == null || account.getAccountNature() == null || !StringUtils.hasText(account.getEntityCode())) {
            return;
        }
        String entityCode = account.getEntityCode().trim();
        if (AccountNature.ENTITY_SETTINGS.equals(account.getAccountNature())) {
            entityRedisKeys.add(entityCode);
            return;
        }
        if (AccountNature.AR.equals(account.getAccountNature()) && StringUtils.hasText(account.getAccountNumber())) {
            arRedisKeys.add(account.getAccountNumber().trim() + CacheKeyConstants.AR_REDIS_KEY_SEPARATOR + entityCode);
        }
    }

    private void syncAccountRedisForAccount(Account account) {
        if (account == null || account.getAccountNature() == null || !StringUtils.hasText(account.getEntityCode())) {
            return;
        }
        if (AccountNature.ENTITY_SETTINGS.equals(account.getAccountNature())) {
            accountService.syncAccountToRedis(null, AccountNature.ENTITY_SETTINGS, account.getEntityCode().trim(), null);
            return;
        }
        if (AccountNature.AR.equals(account.getAccountNature()) && StringUtils.hasText(account.getAccountNumber())) {
            accountService.syncAccountToRedis(
                    account.getAccountNumber().trim(), AccountNature.AR, account.getEntityCode().trim(), null);
        }
    }

    private void syncRedisTargets(Set<String> entityRedisKeys, Set<String> arRedisKeys) {
        for (String entityCode : entityRedisKeys) {
            accountService.syncAccountToRedis(null, AccountNature.ENTITY_SETTINGS, entityCode, null);
        }
        for (String arKey : arRedisKeys) {
            int separator = arKey.indexOf(CacheKeyConstants.AR_REDIS_KEY_SEPARATOR);
            String accountNumber = arKey.substring(0, separator);
            String entityCode = arKey.substring(separator + 1);
            accountService.syncAccountToRedis(accountNumber, AccountNature.AR, entityCode, null);
        }
    }

    private void createOne(ServiceChargeUpdateRequest request, Account account) {
        String entityCode = ServiceCodesService.requireEntityCode(request.getEntityCode());
        String requestedChargeType = ServiceCodesService.requireChargeType(request.getChargeType());
        String serviceCode = request.getServiceCode() == null ? "" : request.getServiceCode().trim();
        if (!StringUtils.hasText(serviceCode)) {
            throw new BadRequestException(ServiceChargeMessageConstants.SERVICE_CODE_REQUIRED_ON_CREATE);
        }

        requireAccountMatchingEntityAndChargeType(account.getId(), entityCode, requestedChargeType);

        List<String> masterChargeTypes = List.of(requestedChargeType);
        ServiceCodes master = serviceCodesService.requireExistingMaster(
                entityCode, requestedChargeType, serviceCode);

        ensureNoExistingChargeForServiceCode(account, entityCode, requestedChargeType, serviceCode);

        if (Boolean.FALSE.equals(request.getActive())) {
            rateSheetUsageValidator.assertNotUsedOnRateSheet(account.getId(), serviceCode);
        }

        ServiceCharge charge = new ServiceCharge();
        charge.setAccount(account);
        charge.setServiceCodes(master);
        charge.setDescription(master.getServiceDescription());
        applyChargeFields(charge, request);
        if (charge.getActive() == null) {
            charge.setActive(true);
        }
        if (charge.getIsBillable() == null) {
            charge.setIsBillable(true);
        }
        serviceCodesService.updateEntitySettingsActiveFlag(
                account, master, requestedChargeType, request.getActive());
        serviceChargeRepository.save(charge);
    }

    private void ensureNoExistingChargeForServiceCode(
            Account account,
            String entityCode,
            String requestedChargeType,
            String serviceCode) {
        boolean alreadyExists = loadAccountChargesByMasterId(account, entityCode, requestedChargeType).values().stream()
                .anyMatch(charge -> charge.getServiceCodes() != null
                        && serviceCode.equalsIgnoreCase(charge.getServiceCodes().getServiceCode()));
        if (alreadyExists) {
            throw new ConflictException(ServiceChargeMessageConstants.SERVICE_CHARGE_ALREADY_EXISTS);
        }
    }

    private void applyChargeFields(ServiceCharge charge, ServiceChargeUpdateRequest request) {
        if (request.getActive() != null) {
            charge.setActive(request.getActive());
        }
        if (request.getDefaultAccountNumber() != null) {
            charge.setDefaultAccountNumber(
                    StringUtils.hasText(request.getDefaultAccountNumber())
                            ? request.getDefaultAccountNumber().trim()
                            : null);
        }
        if (request.getIsBillable() != null) {
            charge.setIsBillable(request.getIsBillable());
        }
        if (request.getTaxValue() != null) {
            validateTaxValue(request.getTaxValue());
            charge.setTaxValue(request.getTaxValue());
        }
    }

    private static void validateTaxValue(Double taxValue) {
        if (taxValue < 0) {
            throw new BadRequestException(ServiceChargeMessageConstants.TAX_VALUE_NON_NEGATIVE);
        }
    }

    @Transactional(readOnly = false)
    @Caching(evict = {
            @CacheEvict(cacheNames = DashboardCacheNames.SERVICE_CHARGES, allEntries = true),
            @CacheEvict(cacheNames = DashboardCacheNames.RATE_SHEET_DROPDOWN, allEntries = true),
            @CacheEvict(cacheNames = DashboardCacheNames.ACCOUNT_DETAILS, allEntries = true)
    })
    public MessageResponse delete(UUID id) {
        ServiceCharge charge = serviceChargeRepository.findByIdWithActiveServiceCodes(id)
                .orElseThrow(() -> new ResourceNotFoundException(DashboardMessageConstants.SERVICE_CHARGE_NOT_FOUND));

        Account account = charge.getAccount();
        serviceChargeRepository.delete(charge);
        syncAccountRedisForAccount(account);

        return new MessageResponse(DashboardMessageConstants.DELETED_SUCCESSFULLY);
    }

    @Cacheable(
            cacheNames = DashboardCacheNames.SERVICE_CHARGES,
            key = "{#accountId ?: 'NA', #entityCode, #chargeType}")
    public List<ServiceChargeResponse> getServiceCharges(String accountId, String entityCode, String chargeType) {
        String resolvedEntityCode = ServiceCodesService.requireEntityCode(entityCode);
        String resolvedChargeType = ServiceCodesService.requireChargeType(chargeType);
        Double entityTaxValue = resolveEntityTaxValue(resolvedEntityCode);

        List<ServiceCodes> activeMasters = loadMastersForFetch(resolvedEntityCode, resolvedChargeType);
        if (!hasAccountId(accountId)) {
            return activeMasters.stream()
                    .map(master -> mapResponse(null, master, null, entityTaxValue))
                    .toList();
        }

        UUID resolvedAccountId = parseAccountId(accountId);

        Account account =
                requireAccountMatchingEntityAndChargeType(resolvedAccountId, entityCode, chargeType);
        Map<UUID, ServiceCharge> chargeByMasterId =
                loadAccountChargesByMasterId(account, resolvedEntityCode, resolvedChargeType);

        return activeMasters.stream()
                .map(master -> mapResponse(
                        account,
                        master,
                        chargeByMasterId.get(master.getId()),
                        entityTaxValue))
                .toList();
    }

    /**
     * Active service codes for dropdowns (e.g. rate sheet), using the same master/charge resolution as
     * {@link #getServiceCharges}.
     */
    public List<ServiceCodeDropdownOption> getActiveServiceCodesForDropdown(
            String accountId, String entityCode, String chargeType) {
        return getActiveServiceCodesForDropdown(accountId, null, entityCode, chargeType);
    }

    public List<ServiceCodeDropdownOption> getActiveServiceCodesForDropdown(
            String accountId, AccountNature accountNature, String entityCode, String chargeType) {
        if (!StringUtils.hasText(entityCode) || !StringUtils.hasText(chargeType)) {
            return List.of();
        }
        String resolvedEntityCode = ServiceCodesService.requireEntityCode(entityCode);
        String resolvedChargeType = ServiceCodesService.requireChargeType(chargeType);
        List<ServiceCodes> masters = loadMastersForFetch(resolvedEntityCode, resolvedChargeType);

        Account account = null;
        Map<UUID, ServiceCharge> chargeByMasterId = Map.of();
        if (hasAccountId(accountId)) {
            UUID resolvedAccountId = parseAccountId(accountId);
            account = requireAccountMatchingEntityAndChargeType(resolvedAccountId, entityCode, chargeType);
            chargeByMasterId = loadAccountChargesByMasterId(account, resolvedEntityCode, resolvedChargeType);
        }

        AccountNature natureForFilter = account != null ? account.getAccountNature() : accountNature;
        Map<UUID, ServiceCharge> chargesByMasterId = chargeByMasterId;

        return masters.stream()
                .filter(master -> ServiceCodeActiveSupport.resolveActive(
                        natureForFilter,
                        master,
                        chargesByMasterId.get(master.getId())))
                .filter(master -> StringUtils.hasText(master.getServiceCode()))
                .collect(Collectors.toMap(
                        master -> master.getServiceCode().trim().toUpperCase(),
                        master -> toServiceCodeDropdownOption(master, chargesByMasterId.get(master.getId())),
                        (left, right) -> left,
                        LinkedHashMap::new))
                .values()
                .stream()
                .sorted(Comparator.comparing(
                        ServiceCodeDropdownOption::getServiceCode, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private static ServiceCodeDropdownOption toServiceCodeDropdownOption(
            ServiceCodes master, ServiceCharge charge) {
        String description = charge != null && StringUtils.hasText(charge.getDescription())
                ? charge.getDescription().trim()
                : master.getServiceDescription();
        return new ServiceCodeDropdownOption(master.getServiceCode().trim(), description);
    }

    private static String resolveAccountChargeType(Account account, ServiceChargeUpdateRequest request) {
        if (request.getChargeType() != null && !request.getChargeType().isBlank()) {
            return request.getChargeType();
        }
        return account.getChargeType();
    }

    /** True when {@code accountId} is omitted, blank, or a literal {@code null} placeholder from clients. */
    private static boolean hasAccountId(String accountId) {
        if (!StringUtils.hasText(accountId)) {
            return false;
        }
        return !ServiceChargeMessageConstants.NULL_ACCOUNT_ID_PLACEHOLDER.equalsIgnoreCase(accountId.trim());
    }

    private static UUID parseAccountId(String accountId) {
        try {
            return UUID.fromString(accountId.trim());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ServiceChargeMessageConstants.INVALID_ACCOUNT_ID_PREFIX + accountId);
        }
    }

    private List<ServiceCodes> loadMastersForFetch(String entityCode, String requestedChargeType) {
        return serviceCodesService.findByEntityCodeAndChargeType(entityCode, requestedChargeType);
    }

    private Map<UUID, ServiceCharge> loadAccountChargesByMasterId(
            Account account, String entityCode, String requestedChargeType) {
        List<ServiceCharge> charges = serviceChargeRepository.findByAccountWithActiveServiceCodes(
                account, entityCode, requestedChargeType);
        return indexChargesByMasterId(charges);
    }

    private Map<UUID, ServiceCharge> indexChargesByMasterId(List<ServiceCharge> charges) {
        return charges.stream()
                .filter(charge -> charge.getServiceCodes() != null)
                .collect(Collectors.toMap(
                        charge -> charge.getServiceCodes().getId(),
                        Function.identity(),
                        (existing, duplicate) -> existing,
                        LinkedHashMap::new));
    }

    private Account requireAccountMatchingEntityAndChargeType(
            UUID accountId, String entityCode, String chargeType) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(DashboardMessageConstants.ACCOUNT_NOT_FOUND));

        String resolvedEntityCode = ServiceCodesService.requireEntityCode(entityCode);
        String resolvedChargeType = ServiceCodesService.requireChargeType(chargeType);

        if (account.getEntityCode() == null
                || !account.getEntityCode().equalsIgnoreCase(resolvedEntityCode)) {
            throw new BadRequestException(
                    ServiceChargeMessageConstants.ENTITY_CODE_MISMATCH_PREFIX + account.getEntityCode());
        }
        if (account.getChargeType() != null
                && !account.getChargeType().isBlank()
                && !account.getChargeType().equalsIgnoreCase(resolvedChargeType)) {
            throw new BadRequestException(
                    ServiceChargeMessageConstants.CHARGE_TYPE_MISMATCH_PREFIX + account.getChargeType());
        }
        return account;
    }

    private Double resolveEntityTaxValue(String entityCode) {
        return entityRepository.findByCodeIgnoreCase(entityCode)
                .map(Entity::getTaxValue)
                .orElse(null);
    }

    private ServiceChargeResponse mapResponse(
            Account account,
            ServiceCodes master,
            ServiceCharge charge,
            Double entityTaxValue) {
        ServiceChargeTaxResolutionSupport.ResolvedTax tax =
                ServiceChargeTaxResolutionSupport.resolve(charge, master, entityTaxValue);
        return new ServiceChargeResponse(
                charge != null ? charge.getId() : null,
                master.getServiceCode(),
                charge != null ? charge.getDescription() : master.getServiceDescription(),
                master.getServiceDescription(),
                tax.taxValue(),
                tax.taxCode(),
                ServiceCodeActiveSupport.resolveActive(
                        account != null ? account.getAccountNature() : null,
                        master,
                        charge),
                ServiceChargeDefaultAccountNumberSupport.resolve(account, charge),
                charge == null || charge.getIsBillable() == null || charge.getIsBillable(),
                "1",
                "",
                "",
                ServiceCodeActiveSupport.resolveEditable(
                        account != null ? account.getAccountNature() : null,
                        master));
    }
}
