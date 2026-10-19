package com.customcharge.dashboard.service.impl;

import com.customcharge.dashboard.constants.AccountConstants;
import com.customcharge.dashboard.constants.AccountMessageConstants;
import com.customcharge.dashboard.constants.ChargeTypeConstants;
import com.customcharge.dashboard.constants.DashboardLogMessages;
import com.customcharge.dashboard.constants.DashboardMessageConstants;
import com.customcharge.dashboard.constants.MasterCategoryConstants;
import com.customcharge.dashboard.dto.*;
import com.customcharge.dashboard.service.AccountService;
import com.customcharge.dashboard.util.ArShipmentBusinessTypeResolver;
import com.customcharge.persistencelayer.accountRedis.AccountRedisKeySupport;
import com.customcharge.persistencelayer.accountRedis.AccountRedisStringCache;
import com.customcharge.persistencelayer.accountRedis.ApAccountRedisCacheBuilder;
import com.customcharge.persistencelayer.accountRedis.ArAccountRedisCacheBuilder;
import com.customcharge.persistencelayer.accountRedis.ArAccountRedisChargeTypeData;
import com.customcharge.persistencelayer.accountRedis.ArAccountRedisPayload;
import com.customcharge.persistencelayer.accountRedis.EntitySettingsRedisCacheBuilder;
import com.customcharge.persistencelayer.util.ApAccountMasterSupport;
import com.customcharge.persistencelayer.util.ArAccountMasterSupport;
import com.customcharge.persistencelayer.util.ActiveCurrencyLookupService;
import com.customcharge.persistencelayer.cache.CountryEntityLookupService;
import com.customcharge.persistencelayer.cache.MasterCategoryValueLookupService;
import com.customcharge.persistencelayer.cache.dto.MasterCategoryValueRow;
import com.customcharge.persistencelayer.dto.CountryWithEntitiesDropdownDto;
import com.customcharge.persistencelayer.dto.ActiveCurrencyDto;
import com.customcharge.persistencelayer.dto.response.MessageResponse;
import com.customcharge.persistencelayer.entities.*;
import com.customcharge.persistencelayer.enums.*;
import com.customcharge.persistencelayer.exception.BadRequestException;
import com.customcharge.persistencelayer.exception.ResourceNotFoundException;
import com.customcharge.persistencelayer.repository.*;
import com.customcharge.persistencelayer.util.AccountLoaders;
import com.customcharge.persistencelayer.util.AccountVersionSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.customcharge.dashboard.cache.DashboardCacheNames;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final ARAccountRepository arAccountRepository;
    private final APAccountRepository apAccountRepository;
    private final RateSheetRepository rateSheetRepository;
    private final ArAccountRedisCacheBuilder arAccountRedisCacheBuilder;
    private final ApAccountRedisCacheBuilder apAccountRedisCacheBuilder;
    private final EntitySettingsRedisCacheBuilder entitySettingsRedisCacheBuilder;
    private final MasterCategoryValueLookupService masterCategoryValueLookupService;
    private final ActiveCurrencyLookupService activeCurrencyLookupService;
    private final CountryEntityLookupService countryEntityLookupService;
    private final CountryRepository countryRepository;
    private final EntityRepository entityRepository;
    private final ServiceCodesService serviceCodesService;
    private final ObjectProvider<AccountRedisStringCache> accountRedisStringCache;
    private final ObjectMapper objectMapper;
    private final AccountSearchCacheService accountSearchCacheService;

    @Override
    @Cacheable(cacheNames = DashboardCacheNames.COUNTRIES_WITH_ENTITIES)
    public List<CountryWithEntitiesDropdownDto> getCountryWithEntitiesDropdown() {
        return countryEntityLookupService.listCountriesWithActiveEntities();
    }

    @Override
    @Cacheable(cacheNames = DashboardCacheNames.ACTIVE_CURRENCIES, key = "#country ?: 'ALL'")
    public List<ActiveCurrencyDto> getActiveCurrencies(String country) {
        if (StringUtils.hasText(country)) {
            return activeCurrencyLookupService.findActiveCurrencyByCountry(country)
                    .map(List::of)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            AccountMessageConstants.ACTIVE_CURRENCY_NOT_FOUND_FOR_COUNTRY));
        }
        return activeCurrencyLookupService.listActiveCurrencies();
    }

    /**
     * Validates against master country/entity data from {@link CountryEntityLookupService#listCountriesWithActiveEntities()}
     * (prefers Redis {@code Entity} tree); no direct country/entity DB reads here.
     */
    private CountryWithEntitiesDropdownDto requireActiveCountryAndEntityFromMasterLookup(
            String countryCode, String entityCode) {
        for (CountryWithEntitiesDropdownDto row : countryEntityLookupService.listCountriesWithActiveEntities()) {
            if (row.countryCode() == null || !row.countryCode().equalsIgnoreCase(countryCode)) {
                continue;
            }
            boolean linked = row.entities().stream()
                    .anyMatch(e -> e.code() != null && e.code().equalsIgnoreCase(entityCode));
            if (!linked) {
                throw new BadRequestException(
                        AccountMessageConstants.ENTITY_NOT_LINKED_TO_COUNTRY);
            }
            return row;
        }
        throw new BadRequestException(AccountMessageConstants.COUNTRY_NOT_FOUND_OR_INACTIVE);
    }

    @Override
    @Cacheable(cacheNames = DashboardCacheNames.ACCOUNT_DROPDOWNS)
    public AccountDropdownResponse getAccountDropdowns() {

        List<DropdownDTO> searchFields = mapFromCategory(MasterCategoryConstants.ACCOUNT_SEARCH_FIELD);
        List<DropdownDTO> searchTypes = mapFromCategory(MasterCategoryConstants.ACCOUNT_SEARCH_TYPE);
        List<DropdownDTO> entities = mapFromEntity();

        return new AccountDropdownResponse(searchFields, searchTypes, entities);
    }

    private List<DropdownDTO> mapFromCategory(String categoryCode) {
        return masterCategoryValueLookupService.listForCategory(categoryCode).stream()
                .map(this::toDropdown)
                .toList();
    }

    private DropdownDTO toDropdown(MasterCategoryValueRow row) {
        return new DropdownDTO(row.id(), row.code(), row.name());
    }

    private List<DropdownDTO> mapFromEntity() {
        return entityRepository.findByActiveTrue()
                .stream()
                .map(e -> new DropdownDTO(e.getId(), e.getCode(), e.getName()))
                .toList();
    }

    @Override
    public Page<AccountSearchResponseDTO> search(AccountSearchRequestDTO request) {
        return accountSearchCacheService.getSearch(request).toPage();
    }

    @Override
    @Cacheable(cacheNames = DashboardCacheNames.AP_ACCOUNTS_BY_ENTITY, key = "#entityCode")
    public List<ApAccountSummaryDTO> getApAccountsByEntityCode(String entityCode) {
        if (entityCode == null || entityCode.isBlank()) {
            throw new BadRequestException(AccountMessageConstants.ENTITY_CODE_REQUIRED);
        }
        return apAccountRepository.findByEntityCode(entityCode.strip())
                .stream()
                .map(master -> new ApAccountSummaryDTO(master.getAccountName(), master.getAccountNumber()))
                .toList();
    }

    @Override
    @Transactional(readOnly = false)
    @Caching(evict = {
            @CacheEvict(cacheNames = DashboardCacheNames.ACCOUNT_SEARCH, allEntries = true),
            @CacheEvict(cacheNames = DashboardCacheNames.ACCOUNT_DETAILS, allEntries = true),
            @CacheEvict(cacheNames = DashboardCacheNames.AP_ACCOUNTS_BY_ENTITY, allEntries = true)
    })
    public void loadSampleAccountsToRedis() {
        putSampleArMaster(AccountConstants.SAMPLE_ACCOUNT_NUMBER_PRIMARY, AccountConstants.SAMPLE_ENTITY_RUH, AccountConstants.SAMPLE_AR_ACCOUNT_NAME_8001_RUH);
        putSampleArMaster(AccountConstants.SAMPLE_ACCOUNT_NUMBER_SECONDARY, AccountConstants.SAMPLE_ENTITY_DHA, AccountConstants.SAMPLE_AR_ACCOUNT_NAME_8002_DHA);
        putSampleArMaster(AccountConstants.SAMPLE_ACCOUNT_NUMBER_TERTIARY, AccountConstants.SAMPLE_ENTITY_JED, AccountConstants.SAMPLE_AR_ACCOUNT_NAME_8003_JED);
        putSampleArMaster(AccountConstants.SAMPLE_ACCOUNT_NUMBER_QUATERNARY, AccountConstants.SAMPLE_ENTITY_RUH, AccountConstants.SAMPLE_AR_ACCOUNT_NAME_8004_RUH);
        putSampleArMaster(AccountConstants.SAMPLE_ACCOUNT_NUMBER_QUINARY, AccountConstants.SAMPLE_ENTITY_DHA, AccountConstants.SAMPLE_AR_ACCOUNT_NAME_8005_DHA);
    }

    private void putSampleArMaster(String accountNumber, String entityCode, String accountName) {
        String entity = entityCode.trim();
        String acct = accountNumber.trim();
        ARAccount master =
                arAccountRepository
                        .findByAccountNumberAndEntityCode(acct, entity)
                        .orElseGet(
                                () -> {
                                    ARAccount created = new ARAccount();
                                    created.setEntityCode(entity);
                                    created.setAccountNumber(acct);
                                    created.setAccountName(accountName);
                                    created.setStatus(AccountConstants.SAMPLE_ACCOUNT_STATUS_ACTIVE);
                                    created.setCreatedDate(Instant.now());
                                    created.setCreatedBy(AccountConstants.SAMPLE_LOADER_CREATED_BY);
                                    return created;
                                });
        ArAccountMasterSupport.syncMasterIdentity(master, accountName, null);
        arAccountRepository.save(master);
//        writeArMasterCache(master);
    }

    private void putSampleAccount(
            String accountNumber,
            String entityCode,
            AccountNature nature,
            String chargeType,
            String accountName) {

        String entity = entityCode.trim();
        String chargeTypeForDb = normalizeChargeType(chargeType);
        boolean entitySettings = AccountNature.ENTITY_SETTINGS.equals(nature);
        String acctForDb = entitySettings || accountNumber == null || accountNumber.isBlank()
                ? null
                : accountNumber.trim();

        Optional<Account> existingAccount =
                findExistingSampleAccount(acctForDb, entity, nature, chargeTypeForDb);
        if (existingAccount.isPresent()) {
            Account account = existingAccount.get();
            ensureSampleServiceCharges(account);
            refreshSampleAccountCache(account, nature);
            log.debug(
                    DashboardLogMessages.SAMPLE_ACCOUNT_EXISTS,
                    acctForDb, entity, nature, chargeTypeForDb);
            return;
        }

        Entity entityRef = requireActiveEntityByCode(entity);
        Account account;
        if (AccountNature.AR.equals(nature)) {
            ARAccount master = ArAccountMasterSupport.findOrCreateMaster(
                    arAccountRepository,
                    entity,
                    acctForDb,
                    accountName == null ? "" : accountName.trim(),
                    null);
            ArAccountMasterSupport.syncMasterIdentity(
                    master, accountName == null ? "" : accountName.trim(), null);
            arAccountRepository.save(master);
            account = ArAccountMasterSupport.findOrCreateChargeConfig(
                    accountRepository, entityRepository, master, entityRef, chargeTypeForDb);
        } else {
            account = new Account();
            assignEntityByCode(account, entity);
            account.setChargeType(chargeTypeForDb);
            account.setAccountNature(nature);
            if (AccountNature.AP.equals(nature)) {
                APAccount master = ApAccountMasterSupport.findOrCreateMaster(
                        apAccountRepository,
                        entity,
                        acctForDb,
                        accountName == null ? "" : accountName.trim());
                ApAccountMasterSupport.syncMasterIdentity(
                        master, accountName == null ? "" : accountName.trim());
                apAccountRepository.save(master);
                account = ApAccountMasterSupport.findOrCreateChargeConfig(
                        accountRepository, master, entityRef, chargeTypeForDb);
            }
            AccountVersionSupport.ensureInitialized(account);
            accountRepository.save(account);
        }
        refreshSampleAccountCache(account, nature);
        ensureSampleServiceCharges(account);
    }

    /**
     * Resolves an existing sample account in the database.
     * AP accounts are matched by account number, entity, and nature only (no charge type).
     */
    private Optional<Account> findExistingSampleAccount(
            String accountNumber,
            String entityCode,
            AccountNature nature,
            String chargeType) {
        if (AccountNature.ENTITY_SETTINGS.equals(nature)) {
            return accountRepository.findByAccountNumberIsNullAndEntityCodeAndAccountNatureAndChargeType(
                    entityCode, nature, chargeType);
        }
        if (AccountNature.AP.equals(nature)) {
            return apAccountRepository
                    .findByAccountNumberAndEntityCode(accountNumber, entityCode)
                    .flatMap(master -> accountRepository.findByApAccountIdAndChargeType(master.getId(), chargeType));
        }
        return accountRepository.findByAccountNumberAndEntityCodeAndAccountNatureAndChargeType(
                accountNumber, entityCode, nature, chargeType);
    }

    private static String normalizeChargeType(String chargeType) {
        if (chargeType == null || chargeType.isBlank()) {
            return null;
        }
        return chargeType.trim();
    }

    private void assignEntityByCode(Account account, String entityCode) {
        account.setEntity(requireActiveEntityByCode(entityCode));
    }

    private Entity requireActiveEntityByCode(String entityCode) {
        if (entityCode == null || entityCode.isBlank()) {
            throw new BadRequestException(AccountMessageConstants.ENTITY_CODE_IS_REQUIRED);
        }
        return entityRepository.findByCodeIgnoreCase(entityCode.trim())
                .filter(e -> Boolean.TRUE.equals(e.getActive()))
                .orElseThrow(() -> new ResourceNotFoundException(AccountMessageConstants.ENTITY_NOT_FOUND_OR_INACTIVE_PREFIX + entityCode));
    }

    private void refreshSampleAccountCache(Account account, AccountNature nature) {
        AccountDetailsResponseDTO dto = mapToResponse(account, nature);
        writeAccountCache(account, dto);
    }


    /**
     * Ensures master {@link ServiceCodes} exist for the account entity and charge type.
     * Does not create {@link ServiceCharge} rows (optional account-level config is added separately).
     */
    private void ensureSampleServiceCharges(Account account) {
        String entityCode = account.getEntityCode();
        if (!StringUtils.hasText(entityCode)) {
            return;
        }

        String chargeType = normalizeChargeType(account.getChargeType());
        if (StringUtils.hasText(chargeType)) {
            ensureServiceCodesForEntityAndChargeType(entityCode.trim(), chargeType);
            return;
        }

        // AP accounts have no charge type — seed all sample charge types for this entity
        for (String sampleChargeType : ChargeTypeConstants.ALL_SAMPLE_CHARGE_TYPES) {
            ensureServiceCodesForEntityAndChargeType(entityCode.trim(), sampleChargeType);
        }
    }

    private void ensureServiceCodesForEntityAndChargeType(String entityCode, String chargeType) {
        serviceCodesService.ensureSampleMastersForEntityAndChargeType(entityCode, chargeType);
    }

    @Override
    @Cacheable(
            cacheNames = DashboardCacheNames.ACCOUNT_DETAILS,
            key = "{#accountNumber ?: 'NA', #nature, #entity, #chargeType ?: 'NA'}")
    public AccountDetailsResponseDTO getAccountDetails(
            String accountNumber, AccountNature nature, String entity, String chargeType) {
        String entityTrimmed = entity == null ? "" : entity.trim();
        String chargeTypeNormalized =
                chargeType == null || chargeType.isBlank() ? null : ChargeTypeConstants.normalize(chargeType);

        if (AccountNature.AP.equals(nature)) {
            return getApAccountDetails(accountNumber, entityTrimmed, chargeTypeNormalized);
        }

        Optional<AccountDetailsResponseDTO> cached =
                readCachedAccount(accountNumber, entityTrimmed, nature, chargeTypeNormalized);
        if (cached.isPresent()) {
            return applyFreshDropdowns(cached.get(), nature, chargeTypeNormalized);
        }

        if (AccountNature.AR.equals(nature)) {
            return getArAccountDetails(accountNumber, entityTrimmed, chargeTypeNormalized);
        }
        if (AccountNature.ENTITY_SETTINGS.equals(nature)) {
            return getEntitySettingsAccountDetails(entityTrimmed, chargeTypeNormalized);
        }

        Account account =
                AccountLoaders.loadAccountOrThrow(
                        accountRepository, accountNumber, nature, entityTrimmed, chargeType);
        AccountDetailsResponseDTO dto = mapToResponse(account, nature);
        writeAccountCache(account, dto);
        return applyFreshDropdowns(dto, nature, chargeTypeNormalized);
    }

    private AccountDetailsResponseDTO getArAccountDetails(
            String accountNumber, String entity, String chargeType) {
        ARAccount master =
                AccountLoaders.findArMaster(arAccountRepository, accountNumber, entity)
                        .orElseThrow(() -> new ResourceNotFoundException(AccountMessageConstants.AR_ACCOUNT_NOT_FOUND));
        Optional<Account> config = AccountLoaders.findArChargeConfig(accountRepository, master, chargeType);
        if (config.isEmpty()) {
            AccountDetailsResponseDTO dto = mapArMasterOnly(master, chargeType);
            writeArMasterCache(master);
            return applyFreshDropdowns(dto, AccountNature.AR, chargeType);
        }
        AccountDetailsResponseDTO dto = mapToResponse(config.get(), AccountNature.AR);
        writeArAccountCache(config.get());
        return applyFreshDropdowns(dto, AccountNature.AR, chargeType);
    }

    private AccountDetailsResponseDTO getApAccountDetails(
            String accountNumber, String entity, String chargeType) {
        APAccount master =
                AccountLoaders.findApMaster(apAccountRepository, accountNumber, entity)
                        .orElseThrow(() -> new ResourceNotFoundException(AccountMessageConstants.AP_ACCOUNT_NOT_FOUND));
        List<Account> configs = accountRepository.findAllByApAccountId(master.getId());
        if (configs.isEmpty()) {
            AccountDetailsResponseDTO dto = mapApMasterOnly(master, chargeType);
            return applyFreshDropdowns(dto, AccountNature.AP, chargeType);
        }
        if (configs.size() > 1) {
            throw new BadRequestException(AccountMessageConstants.MULTIPLE_AP_CONFIGS_FOUND);
        }
        AccountDetailsResponseDTO dto = mapToResponse(configs.get(0), AccountNature.AP);
        return applyFreshDropdowns(dto, AccountNature.AP, chargeType);
    }

    private AccountDetailsResponseDTO getEntitySettingsAccountDetails(String entity, String chargeType) {
        Optional<Account> config =
                AccountLoaders.findEntitySettingsConfig(accountRepository, entity, chargeType);
        if (config.isEmpty()) {
            Entity entityRef = requireActiveEntityByCode(entity);
            AccountDetailsResponseDTO dto = mapEntitySettingsStub(entityRef, chargeType);
            return applyFreshDropdowns(dto, AccountNature.ENTITY_SETTINGS, chargeType);
        }
        AccountDetailsResponseDTO dto = mapToResponse(config.get(), AccountNature.ENTITY_SETTINGS);
        writeEntitySettingsCache(entity);
        return applyFreshDropdowns(dto, AccountNature.ENTITY_SETTINGS, chargeType);
    }

    private Optional<AccountDetailsResponseDTO> readCachedAccount(
            String accountNumber, String entity, AccountNature nature, String chargeType) {
        AccountRedisStringCache cache = accountRedisStringCache.getIfAvailable();
        if (cache == null) {
            return Optional.empty();
        }
        try {
            if (AccountNature.AR.equals(nature)) {
                return readCachedArAccount(accountNumber, entity, chargeType, cache);
            }
            if (AccountNature.ENTITY_SETTINGS.equals(nature)) {
                return readCachedEntitySettings(entity, chargeType, cache);
            }
            String key = AccountRedisKeySupport.redisKeyFor(accountNumber, entity, nature, chargeType);
            Optional<String> json = cache.get(key);
            if (json.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json.get(), AccountDetailsResponseDTO.class));
        } catch (Exception ex) {
            log.warn(DashboardLogMessages.ACCOUNT_CACHE_READ_FAILED, ex.getMessage());
            return Optional.empty();
        }
    }

    private Optional<AccountDetailsResponseDTO> readCachedArAccount(
            String accountNumber,
            String entity,
            String chargeType,
            AccountRedisStringCache cache) throws Exception {
        String key = AccountRedisKeySupport.redisKeyForAccount(accountNumber, entity);
        Optional<String> json = cache.get(key);
        if (json.isEmpty()) {
            return Optional.empty();
        }
        ArAccountRedisPayload payload = objectMapper.readValue(json.get(), ArAccountRedisPayload.class);
        if (payload.getId() == null) {
            log.info(DashboardLogMessages.AR_ACCOUNT_CACHE_THIRD_PARTY_OVERWRITE, key);
            return Optional.empty();
        }
        ArAccountRedisChargeTypeData chargeData = resolveRedisChargeEntry(payload.getCharges(), chargeType);
        if (chargeData == null) {
            return Optional.of(mapArMasterFromRedisPayload(payload, chargeType));
        }
        return Optional.of(mapArFromRedisPayload(payload, chargeType, chargeData));
    }

    private Optional<AccountDetailsResponseDTO> readCachedEntitySettings(
            String entity, String chargeType, AccountRedisStringCache cache) throws Exception {
        String key = AccountRedisKeySupport.redisKeyForEntity(entity);
        Optional<String> json = cache.get(key);
        if (json.isEmpty()) {
            return Optional.empty();
        }
        ArAccountRedisPayload payload = objectMapper.readValue(json.get(), ArAccountRedisPayload.class);
        ArAccountRedisChargeTypeData chargeData = resolveRedisChargeEntry(payload.getCharges(), chargeType);
        return Optional.of(mapEntitySettingsFromRedisPayload(payload, entity, chargeType, chargeData));
    }

    /**
     * Resolves charge config for the requested type only (e.g. DTA must not return DTC).
     */
    private static ArAccountRedisChargeTypeData resolveRedisChargeEntry(
            Map<String, ArAccountRedisChargeTypeData> charges, String chargeType) {
        if (charges == null || chargeType == null || chargeType.isBlank()) {
            return null;
        }
        String chargeKey = ChargeTypeConstants.normalize(chargeType);
        ArAccountRedisChargeTypeData exact = charges.get(chargeKey);
        if (exact != null) {
            return exact;
        }
        for (Map.Entry<String, ArAccountRedisChargeTypeData> entry : charges.entrySet()) {
            if (chargeKey.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static AccountDetailsResponseDTO mapArMasterFromRedisPayload(
            ArAccountRedisPayload payload, String chargeType) {
        AccountDetailsResponseDTO dto = new AccountDetailsResponseDTO();
        dto.setChargeType(chargeType);
        applyArMasterPayloadToDto(dto, payload);
        return dto;
    }

    private static AccountDetailsResponseDTO mapArFromRedisPayload(
            ArAccountRedisPayload payload, String chargeType, ArAccountRedisChargeTypeData chargeData) {
        AccountDetailsResponseDTO dto = mapArMasterFromRedisPayload(payload, chargeType);
        applyChargeTypeDataToArDto(dto, chargeData);
        return dto;
    }

    private static AccountDetailsResponseDTO mapEntitySettingsFromRedisPayload(
            ArAccountRedisPayload payload,
            String entityCode,
            String chargeType,
            ArAccountRedisChargeTypeData chargeData) {
        AccountDetailsResponseDTO dto = new AccountDetailsResponseDTO();
        dto.setChargeType(chargeType);
        dto.setEntity(payload.getEntity() != null ? payload.getEntity() : entityCode);
        if (payload.getCountry() != null) {
            dto.setCountry(payload.getCountry());
        }
        if (chargeData != null) {
            applyEntitySettingsChargeDataToDto(dto, chargeData);
        }
        return dto;
    }

    private static void applyChargeTypeDataToArDto(
            AccountDetailsResponseDTO dto, ArAccountRedisChargeTypeData data) {
        if (data.getAccountId() != null) {
            dto.setId(data.getAccountId());
            dto.setArAccountNumber(data.getArAccountNo());
        }
        if (data.getInvoiceFrequency() != null) {
            dto.setInvoiceFrequency(data.getInvoiceFrequency());
        }
        if (data.getPotentialDelay() != null) {
            dto.setPotentialDelay(data.getPotentialDelay());
        }
        if (data.getInvoiceAmountLimit() != null) {
            dto.setInvoiceLimit(data.getInvoiceAmountLimit());
        }
        if (data.getInvoiceShipmentLimit() != null) {
            dto.setInvoiceShipmentLimit(data.getInvoiceShipmentLimit());
        }
        if (data.getInvoiceBreakBy() != null) {
            dto.setInvoiceBreakBy(data.getInvoiceBreakBy());
        }
        if (data.getCustomType() != null) {
            dto.setCustomsType(data.getCustomType());
        }
        if (data.getEligibleForBilling() != null) {
            dto.setEligibleForBilling(data.getEligibleForBilling());
        }
        if (data.getInvoiceTrigger() != null) {
            dto.setInvoiceTrigger(data.getInvoiceTrigger());
        }
    }

    private static void applyEntitySettingsChargeDataToDto(
            AccountDetailsResponseDTO dto, ArAccountRedisChargeTypeData data) {
        if (data.getAccountId() != null) {
            dto.setId(data.getAccountId());
        }
        if (data.getSplitEntry() != null) {
            dto.setSplitEntry(data.getSplitEntry());
        }
        if (data.getSplitInvoice() != null) {
            dto.setSplitInvoice(data.getSplitInvoice());
        }
        if (data.getInvoiceLimit() != null) {
            dto.setInvoiceLimit(data.getInvoiceLimit());
        }
        if (data.getInvoiceFrequency() != null) {
            dto.setInvoiceFrequency(data.getInvoiceFrequency());
        }
        if (data.getEligibleForBilling() != null) {
            dto.setEligibleForBilling(data.getEligibleForBilling());
        }
        if (data.getInvoiceTrigger() != null) {
            dto.setInvoiceTrigger(data.getInvoiceTrigger());
        }
        if (data.getSapCountryCode() != null) {
            dto.setSapCompanyCode(data.getSapCountryCode());
        }
        if (data.getEntryLimit() != null) {
            dto.setEntryLimit(data.getEntryLimit());
        }
        if (data.getDefaultCashAccount() != null) {
            dto.setDefaultCashAccount(data.getDefaultCashAccount());
        }
        if (data.getAccount() != null) {
            dto.setAccount(data.getAccount());
        }
        if (data.getDefaultAccountNumber() != null) {
            dto.setDefaultAccountNumber(data.getDefaultAccountNumber());
        }
        if (data.getBillingCurrency() != null) {
            dto.setBillingCurrency(data.getBillingCurrency());
        }
    }

    /** Master AR fields from Redis (no per-charge-type billing/invoice config). */
    private static void applyArMasterPayloadToDto(AccountDetailsResponseDTO dto, ArAccountRedisPayload payload) {
        if (payload.getAccountName() != null) {
            dto.setAccountName(payload.getAccountName());
        }
        if (payload.getCountry() != null) {
            dto.setCountry(payload.getCountry());
        }
        if (payload.getArAccountNo() != null) {
            dto.setArAccountNumber(payload.getArAccountNo());
        }
        if (payload.getAccountNo() != null) {
            dto.setAccountNumber(payload.getAccountNo());
        }
        if (payload.getEntity() != null) {
            dto.setEntity(payload.getEntity());
        }
        if (payload.getAccountType() != null) {
            dto.setAccountType(payload.getAccountType());
        }
        if (payload.getStatus() != null) {
            dto.setStatus(payload.getStatus());
        }
        applyArBusinessFields(dto, payload.getBusinessType(), payload.getBusinessSegment());
        if (payload.getRateCurrency() != null) {
            dto.setRateCurrency(payload.getRateCurrency());
        }
        if (payload.getBillingCurrency() != null) {
            dto.setBillingCurrency(payload.getBillingCurrency());
        }
        if (payload.getCustomType() != null) {
            dto.setCustomsType(payload.getCustomType());
        }
    }

    private void writeAccountCache(Account account, AccountDetailsResponseDTO dto) {
        if (AccountNature.AR.equals(account.getAccountNature())) {
            writeArAccountCache(account);
            return;
        }
        if (AccountNature.AP.equals(account.getAccountNature())) {
            writeApAccountCache(account);
            return;
        }
        if (AccountNature.ENTITY_SETTINGS.equals(account.getAccountNature())) {
            writeEntitySettingsCache(account.getEntityCode());
            return;
        }
        AccountRedisStringCache cache = accountRedisStringCache.getIfAvailable();
        if (cache == null) {
            return;
        }

        try {
            String key = AccountRedisKeySupport.redisKeyFor(
                    account.getAccountNumber(),
                    account.getEntityCode(),
                    account.getAccountNature(),
                    account.getChargeType());

            ObjectNode payload = (ObjectNode) objectMapper.valueToTree(dto);
            payload.remove(AccountConstants.CACHE_PAYLOAD_DROPDOWN_VALUES_KEY);
            String json = objectMapper.writeValueAsString(payload);
            cache.put(key, json);

            log.debug(DashboardLogMessages.ACCOUNT_CACHE_DEBUG, key, json.length());
            log.info(DashboardLogMessages.ACCOUNT_CACHE_WRITE_SUCCESS, key);
        } catch (Exception ex) {
            log.error(
                    DashboardLogMessages.ACCOUNT_CACHE_WRITE_FAILED,
                    account.getId(),
                    account.getEntityCode(),
                    account.getAccountNature(),
                    account.getChargeType(),
                    ex);
        }
    }

    private void writeApAccountCache(Account account) {
        if (account.getApAccount() != null) {
            writeApMasterCache(account.getApAccount());
        }
    }

    private boolean writeApMasterCache(APAccount master) {
        AccountRedisStringCache cache = accountRedisStringCache.getIfAvailable();
        if (cache == null) {
            return false;
        }
        try {
            ArAccountRedisPayload payload = apAccountRedisCacheBuilder.build(master);
            String key =
                    AccountRedisKeySupport.redisKeyForAccount(
                            master.getAccountNumber(), master.getEntityCode());
            cache.put(key, objectMapper.writeValueAsString(payload));
            log.info(DashboardLogMessages.ACCOUNT_CACHE_WRITE_SUCCESS, key);
            return true;
        } catch (Exception ex) {
            log.error(DashboardLogMessages.ACCOUNT_CACHE_WRITE_FAILED, master.getId(), master.getEntityCode(),
                    AccountNature.AP, null, ex);
            return false;
        }
    }

    private void writeArAccountCache(Account account) {
        if (account.getArAccount() != null) {
            writeArMasterCache(account.getArAccount());
        }
    }

    private void writeEntitySettingsCache(String entityCode) {
        if (!StringUtils.hasText(entityCode)) {
            return;
        }
        AccountRedisStringCache cache = accountRedisStringCache.getIfAvailable();
        if (cache == null) {
            return;
        }
        try {
            ArAccountRedisPayload payload = entitySettingsRedisCacheBuilder.build(entityCode.trim());
            String key = AccountRedisKeySupport.redisKeyForEntity(entityCode.trim());
            cache.put(key, objectMapper.writeValueAsString(payload));
            log.info(DashboardLogMessages.ACCOUNT_CACHE_WRITE_SUCCESS, key);
        } catch (Exception ex) {
            log.error(DashboardLogMessages.ACCOUNT_CACHE_WRITE_FAILED, null, entityCode,
                    AccountNature.ENTITY_SETTINGS, null, ex);
        }
    }

    @Override
    @CacheEvict(cacheNames = DashboardCacheNames.ACCOUNT_DETAILS, allEntries = true)
    public MessageResponse syncAccountToRedis(
            String accountNumber, AccountNature accountNature, String entity, String chargeType) {
        if (isSyncAllRequest(accountNumber, accountNature, entity, chargeType)) {
            return syncAllAccountsToRedis();
        }
        if (accountNature == null) {
            throw new BadRequestException(AccountMessageConstants.ACCOUNT_NATURE_REQUIRED_FOR_SYNC);
        }
        if (isSyncAllByNatureRequest(accountNumber, accountNature, entity, chargeType)) {
            return AccountNature.AR.equals(accountNature)
                    ? syncAllArAccountsToRedis()
                    : syncAllApAccountsToRedis();
        }
        String entityTrimmed = entity == null ? "" : entity.trim();
        if (!StringUtils.hasText(entityTrimmed) && !AccountNature.AR.equals(accountNature)) {
            throw new BadRequestException(AccountMessageConstants.ENTITY_REQUIRED_FOR_SYNC);
        }
        if (AccountNature.AR.equals(accountNature)) {
            if (!StringUtils.hasText(accountNumber) || !StringUtils.hasText(entityTrimmed)) {
                throw new BadRequestException(AccountMessageConstants.AR_ACCOUNT_NUMBER_AND_ENTITY_REQUIRED_FOR_SYNC);
            }
            ARAccount master =
                    arAccountRepository
                            .findByAccountNumberAndEntityCodeWithConfigs(
                                    accountNumber == null ? "" : accountNumber.trim(), entityTrimmed)
                            .orElseThrow(() -> new ResourceNotFoundException(AccountMessageConstants.AR_ACCOUNT_MASTER_NOT_FOUND));
            writeArMasterCache(master);
            return new MessageResponse(AccountMessageConstants.AR_ACCOUNT_SYNCED_TO_REDIS_PREFIX + master.getAccountNumber() + ":"
                    + master.getEntityCode());
        }
        if (AccountNature.ENTITY_SETTINGS.equals(accountNature)) {
            writeEntitySettingsCache(entityTrimmed);
            return new MessageResponse(AccountMessageConstants.ENTITY_SETTINGS_SYNCED_TO_REDIS_PREFIX + entityTrimmed);
        }
        if (AccountNature.AP.equals(accountNature)) {
            if (!StringUtils.hasText(accountNumber) || !StringUtils.hasText(entityTrimmed)) {
                throw new BadRequestException(AccountMessageConstants.AP_ACCOUNT_NUMBER_AND_ENTITY_REQUIRED_FOR_SYNC);
            }
            APAccount master =
                    apAccountRepository
                            .findByAccountNumberAndEntityCodeWithConfigs(
                                    accountNumber == null ? "" : accountNumber.trim(), entityTrimmed)
                            .orElseThrow(() -> new ResourceNotFoundException(AccountMessageConstants.AP_ACCOUNT_MASTER_NOT_FOUND));
            writeApMasterCache(master);
            return new MessageResponse(AccountMessageConstants.AP_ACCOUNT_SYNCED_TO_REDIS_PREFIX + master.getAccountNumber() + ":"
                    + master.getEntityCode());
        }
        Account account =
                AccountLoaders.loadAccountOrThrow(
                        accountRepository,
                        accountNumber,
                        accountNature,
                        entityTrimmed,
                        AccountNature.AP.equals(accountNature) ? null : chargeType);
        AccountDetailsResponseDTO dto = mapToResponse(account, accountNature);
        writeAccountCache(account, dto);
        String key =
                AccountRedisKeySupport.redisKeyFor(
                        account.getAccountNumber(), account.getEntityCode(), accountNature, account.getChargeType());
        return new MessageResponse(AccountMessageConstants.ACCOUNT_SYNCED_TO_REDIS_PREFIX + key);
    }

    private static boolean isSyncAllRequest(
            String accountNumber, AccountNature accountNature, String entity, String chargeType) {
        return !StringUtils.hasText(accountNumber)
                && accountNature == null
                && !StringUtils.hasText(entity)
                && !StringUtils.hasText(chargeType);
    }

    private static boolean isSyncAllByNatureRequest(
            String accountNumber, AccountNature accountNature, String entity, String chargeType) {
        return !StringUtils.hasText(accountNumber)
                && !StringUtils.hasText(entity)
                && !StringUtils.hasText(chargeType)
                && (AccountNature.AR.equals(accountNature) || AccountNature.AP.equals(accountNature));
    }

    private MessageResponse syncAllArAccountsToRedis() {
        AccountRedisStringCache cache = accountRedisStringCache.getIfAvailable();
        if (cache == null) {
            return new MessageResponse(AccountMessageConstants.REDIS_CACHE_NOT_ENABLED);
        }

        int synced = 0;
        int failures = 0;
        for (ARAccount master : arAccountRepository.findAll()) {
            if (writeArMasterCache(master)) {
                synced++;
            } else {
                failures++;
            }
        }

        return new MessageResponse(
                AccountMessageConstants.SYNCED_ALL_AR_ACCOUNTS_TO_REDIS_PREFIX
                        + synced
                        + AccountMessageConstants.SYNCED_NATURE_ACCOUNTS_SUFFIX
                        + (failures > 0
                                ? AccountMessageConstants.SYNC_FAILURES_SUFFIX
                                        + failures
                                        + AccountMessageConstants.SYNC_FAILURES_COUNT_SUFFIX
                                : ""));
    }

    private MessageResponse syncAllApAccountsToRedis() {
        AccountRedisStringCache cache = accountRedisStringCache.getIfAvailable();
        if (cache == null) {
            return new MessageResponse(AccountMessageConstants.REDIS_CACHE_NOT_ENABLED);
        }

        int synced = 0;
        int failures = 0;
        for (APAccount master : apAccountRepository.findAll()) {
            if (writeApMasterCache(master)) {
                synced++;
            } else {
                failures++;
            }
        }

        return new MessageResponse(
                AccountMessageConstants.SYNCED_ALL_AP_ACCOUNTS_TO_REDIS_PREFIX
                        + synced
                        + AccountMessageConstants.SYNCED_NATURE_ACCOUNTS_SUFFIX
                        + (failures > 0
                                ? AccountMessageConstants.SYNC_FAILURES_SUFFIX
                                        + failures
                                        + AccountMessageConstants.SYNC_FAILURES_COUNT_SUFFIX
                                : ""));
    }

    private MessageResponse syncAllAccountsToRedis() {
        AccountRedisStringCache cache = accountRedisStringCache.getIfAvailable();
        if (cache == null) {
            return new MessageResponse(AccountMessageConstants.REDIS_CACHE_NOT_ENABLED);
        }

        int arMasters = 0;
        int apAccounts = 0;
        int entitySettings = 0;
        int failures = 0;

        for (ARAccount master : arAccountRepository.findAll()) {
            if (writeArMasterCache(master)) {
                arMasters++;
            } else {
                failures++;
            }
        }

        for (APAccount master : apAccountRepository.findAll()) {
            if (writeApMasterCache(master)) {
                apAccounts++;
            } else {
                failures++;
            }
        }

        java.util.Set<String> entityCodes = new java.util.LinkedHashSet<>();
        for (Account account : accountRepository.findByAccountNature(AccountNature.ENTITY_SETTINGS)) {
            if (StringUtils.hasText(account.getEntityCode())) {
                entityCodes.add(account.getEntityCode());
            }
        }
        for (String entityCode : entityCodes) {
            try {
                writeEntitySettingsCache(entityCode);
                entitySettings++;
            } catch (Exception ex) {
                failures++;
            }
        }

        return new MessageResponse(
                AccountMessageConstants.SYNCED_ALL_ACCOUNTS_TO_REDIS_PREFIX
                        + arMasters
                        + AccountMessageConstants.SYNCED_AR_MASTERS_SUFFIX
                        + apAccounts
                        + AccountMessageConstants.SYNCED_AP_SUFFIX
                        + entitySettings
                        + AccountMessageConstants.SYNCED_ENTITY_SETTINGS_SUFFIX
                        + (failures > 0 ? AccountMessageConstants.SYNC_FAILURES_SUFFIX + failures + AccountMessageConstants.SYNC_FAILURES_COUNT_SUFFIX : ""));
    }

    private boolean syncSingleAccountRowToRedis(Account account) {
        try {
            AccountDetailsResponseDTO dto = mapToResponse(account, account.getAccountNature());
            writeAccountCache(account, dto);
            return true;
        } catch (Exception ex) {
            log.warn(
                    DashboardLogMessages.ACCOUNT_SYNC_FAILED,
                    account.getId(),
                    account.getAccountNature(),
                    account.getEntityCode(),
                    account.getChargeType(),
                    ex.getMessage());
            return false;
        }
    }

    private boolean writeArMasterCache(ARAccount master) {
        AccountRedisStringCache cache = accountRedisStringCache.getIfAvailable();
        if (cache == null) {
            return false;
        }
        try {
            ArAccountRedisPayload payload = arAccountRedisCacheBuilder.build(master);
            String key =
                    AccountRedisKeySupport.redisKeyForAccount(
                            master.getAccountNumber(), master.getEntityCode());
            cache.put(key, objectMapper.writeValueAsString(payload));
            log.info(DashboardLogMessages.ACCOUNT_CACHE_WRITE_SUCCESS, key);
            return true;
        } catch (Exception ex) {
            log.error(DashboardLogMessages.ACCOUNT_CACHE_WRITE_FAILED, master.getId(), master.getEntityCode(),
                    AccountNature.AR, null, ex);
            return false;
        }
    }

    private AccountDetailsResponseDTO applyFreshDropdowns(AccountDetailsResponseDTO dto, AccountNature nature, String chargeType) {
        rateSheetRepository.findByAccount_Id(dto.getId()).ifPresent(rs -> dto.setRateSheetId(rs.getId()));
        if (nature == AccountNature.AP) {
            dto.setDropdownValues(apDropdownValues());
        } else if (nature == AccountNature.AR) {
            dto.setDropdownValues(arDropdownValues());
        } else if (nature == AccountNature.ENTITY_SETTINGS
                && ChargeTypeConstants.ENTITY_SETTINGS_REVENUE.contains(chargeType)) {
            dto.setDropdownValues(entitySettingsDropdownValuesRevenue());
        } else if (nature == AccountNature.ENTITY_SETTINGS
                && ChargeTypeConstants.ENTITY_SETTINGS_COST.contains(chargeType)) {
            dto.setDropdownValues(entitySettingsDropdownValuesCost());
        } else {
            dto.setDropdownValues(Map.of());
        }
        return dto;
    }

    @Override
    @Transactional(readOnly = false)
    @Caching(evict = {
            @CacheEvict(cacheNames = DashboardCacheNames.ACCOUNT_DROPDOWNS, allEntries = true),
            @CacheEvict(cacheNames = DashboardCacheNames.AP_ACCOUNTS_BY_ENTITY, allEntries = true),
            @CacheEvict(cacheNames = DashboardCacheNames.ACCOUNT_SEARCH, allEntries = true),
            @CacheEvict(cacheNames = DashboardCacheNames.ACCOUNT_DETAILS, allEntries = true),
            @CacheEvict(cacheNames = DashboardCacheNames.SERVICE_CHARGES, allEntries = true)
    })
    public AccountDetailsResponseDTO updateAccount(AccountUpdateRequestDTO request) {
        validateUpdateAccountRequest(request);

        AccountNature nature = resolveUpdateNature(request);
        if (AccountNature.AR.equals(nature)) {
            return updateArAccount(request);
        }
        if (AccountNature.AP.equals(nature)) {
            return updateApAccount(request);
        }
        if (AccountNature.ENTITY_SETTINGS.equals(nature)) {
            return updateEntitySettingsAccount(request);
        }

        Account account =
                accountRepository
                        .findById(request.getId())
                        .orElseThrow(() -> new ResourceNotFoundException(DashboardMessageConstants.ACCOUNT_NOT_FOUND));
        updateNatureSpecificFields(account, request);
        AccountVersionSupport.ensureInitialized(account);
        accountRepository.save(account);
        AccountDetailsResponseDTO dto = mapToResponse(account, account.getAccountNature());
        writeAccountCache(account, dto);
        return dto;
    }

    private AccountNature resolveUpdateNature(AccountUpdateRequestDTO request) {
        if (request.getAccountNature() != null) {
            return request.getAccountNature();
        }
        if (request.getId() != null) {
            return accountRepository
                    .findById(request.getId())
                    .map(Account::getAccountNature)
                    .orElseThrow(() -> new ResourceNotFoundException(DashboardMessageConstants.ACCOUNT_NOT_FOUND));
        }
        throw new BadRequestException(AccountMessageConstants.ACCOUNT_NATURE_REQUIRED);
    }

    private AccountDetailsResponseDTO updateArAccount(AccountUpdateRequestDTO request) {
        if (!StringUtils.hasText(request.getAccountNumber()) || !StringUtils.hasText(request.getEntity())) {
            throw new BadRequestException(AccountMessageConstants.AR_UPDATE_ACCOUNT_NUMBER_AND_ENTITY_REQUIRED);
        }
        String entity = request.getEntity().trim();
        String accountNumber = request.getAccountNumber().trim();
        String chargeType =
                request.getChargeType() == null || request.getChargeType().isBlank()
                        ? null
                        : request.getChargeType().trim();

        Entity entityRef = requireActiveEntityByCode(entity);
        ARAccount master =
                AccountLoaders.findArMaster(arAccountRepository, accountNumber, entity)
                        .orElseGet(
                                () ->
                                        ArAccountMasterSupport.findOrCreateMaster(
                                                arAccountRepository,
                                                entity,
                                                accountNumber,
                                                request.getAccountName(),
                                                null));

        updateCommonFields(accountFromMaster(master), request);
        applyOptionalPlainValue(request.getAccountType(), master::setAccountType, AccountConstants.FIELD_LABEL_ACCOUNT_TYPE);
        updateArMasterFields(master, request);

        Account config = null;
        if (StringUtils.hasText(chargeType) || request.getId() != null) {
            if (request.getId() != null) {
                config =
                        accountRepository
                                .findById(request.getId())
                                .orElseThrow(() -> new ResourceNotFoundException(DashboardMessageConstants.ACCOUNT_NOT_FOUND));
            } else {
                config =
                        ArAccountMasterSupport.findOrCreateChargeConfig(
                                accountRepository, entityRepository, master, entityRef, chargeType);
            }
            updateArFields(config, request);
            AccountVersionSupport.ensureInitialized(config);
            accountRepository.save(config);
        }
        arAccountRepository.save(master);

        writeArMasterCache(master);

        if (config != null) {
            AccountDetailsResponseDTO dto = mapToResponse(config, AccountNature.AR);
            return applyFreshDropdowns(dto, AccountNature.AR, chargeType);
        }
        return applyFreshDropdowns(mapArMasterOnly(master, chargeType), AccountNature.AR, chargeType);
    }

    private AccountDetailsResponseDTO updateApAccount(AccountUpdateRequestDTO request) {
        if (!StringUtils.hasText(request.getAccountNumber()) || !StringUtils.hasText(request.getEntity())) {
            throw new BadRequestException(AccountMessageConstants.AP_UPDATE_ACCOUNT_NUMBER_AND_ENTITY_REQUIRED);
        }
        String entity = request.getEntity().trim();
        String accountNumber = request.getAccountNumber().trim();
        String chargeType =
                request.getChargeType() == null || request.getChargeType().isBlank()
                        ? null
                        : request.getChargeType().trim();

        Entity entityRef = requireActiveEntityByCode(entity);
        APAccount master =
                AccountLoaders.findApMaster(apAccountRepository, accountNumber, entity)
                        .orElseGet(
                                () ->
                                        ApAccountMasterSupport.findOrCreateMaster(
                                                apAccountRepository,
                                                entity,
                                                accountNumber,
                                                request.getAccountName()));

        updateCommonFields(accountFromApMaster(master), request);
        applyOptionalPlainValue(request.getAccountType(), master::setAccountType, AccountConstants.FIELD_LABEL_ACCOUNT_TYPE);
        applyOptionalCategoryCode(request.getMarketStructure(), MasterCategoryConstants.MARKET_STRUCTURE,
                master::setMarketStructure, AccountMessageConstants.MARKET_STRUCTURE_NOT_FOUND);
        master.setStatus(nullIfBlank(request.getAccountStatus()));
        apAccountRepository.save(master);

        Account config = null;
        if (StringUtils.hasText(chargeType) || request.getId() != null) {
            if (request.getId() != null) {
                config =
                        accountRepository
                                .findById(request.getId())
                                .orElseThrow(() -> new ResourceNotFoundException(DashboardMessageConstants.ACCOUNT_NOT_FOUND));
            } else {
                config =
                        ApAccountMasterSupport.findOrCreateChargeConfig(
                                accountRepository, master, entityRef, chargeType);
            }
            updateApFields(config, request);
            AccountVersionSupport.ensureInitialized(config);
            accountRepository.save(config);
        }

        writeApMasterCache(master);

        if (config != null) {
            AccountDetailsResponseDTO dto = mapToResponse(config, AccountNature.AP);
            return applyFreshDropdowns(dto, AccountNature.AP, chargeType);
        }
        return applyFreshDropdowns(mapApMasterOnly(master, chargeType), AccountNature.AP, chargeType);
    }

    private AccountDetailsResponseDTO updateEntitySettingsAccount(AccountUpdateRequestDTO request) {
        if (!StringUtils.hasText(request.getEntity())) {
            throw new BadRequestException(AccountMessageConstants.ENTITY_SETTINGS_ENTITY_REQUIRED);
        }
        if (!StringUtils.hasText(request.getChargeType())) {
            throw new BadRequestException(AccountMessageConstants.ENTITY_SETTINGS_CHARGE_TYPE_REQUIRED);
        }
        String entity = request.getEntity().trim();
        String chargeType = request.getChargeType().trim();
        Entity entityRef = requireActiveEntityByCode(entity);

        Account account;
        if (request.getId() != null) {
            account =
                    accountRepository
                            .findById(request.getId())
                            .orElseThrow(() -> new ResourceNotFoundException(DashboardMessageConstants.ACCOUNT_NOT_FOUND));
        } else {
            account =
                    AccountLoaders.findEntitySettingsConfig(accountRepository, entity, chargeType)
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

        if (isEntitySettingsRevenue(account)) {
            updateEntitySettingsRevenueFields(account, request);
        } else if (isEntitySettingsCost(account)) {
            updateEntitySettingsCostFields(account, request);
        } else {
            rejectUnsupportedFields(account, request, allNatureSpecificUpdateFields());
        }
        account.setAccountName(nullIfBlank(request.getAccountName()));
        AccountVersionSupport.ensureInitialized(account);
        accountRepository.save(account);
        writeEntitySettingsCache(entity);

        AccountDetailsResponseDTO dto = mapToResponse(account, AccountNature.ENTITY_SETTINGS);
        return applyFreshDropdowns(dto, AccountNature.ENTITY_SETTINGS, chargeType);
    }

    /** Minimal {@link Account} wrapper so {@link #updateCommonFields} can update AR master name. */
    private Account accountFromMaster(ARAccount master) {
        Account proxy = new Account();
        proxy.setAccountNature(AccountNature.AR);
        proxy.setArAccount(master);
        return proxy;
    }

    /** Minimal {@link Account} wrapper so {@link #updateCommonFields} can update AP master name. */
    private Account accountFromApMaster(APAccount master) {
        Account proxy = new Account();
        proxy.setAccountNature(AccountNature.AP);
        proxy.setApAccount(master);
        return proxy;
    }

    private void updateCommonFields(Account account, AccountUpdateRequestDTO request) {
        String accountName = nullIfBlank(request.getAccountName());
        account.setAccountName(accountName);
        if (account.getArAccount() != null) {
            account.getArAccount().setAccountName(accountName);
        }
        if (account.getApAccount() != null) {
            account.getApAccount().setAccountName(accountName);
        }
    }

    private void updateNatureSpecificFields(Account account, AccountUpdateRequestDTO request) {
        if (account.getAccountNature() == AccountNature.AP) {
            updateApFields(account, request);
        } else if (account.getAccountNature() == AccountNature.AR) {
            updateArFields(account, request);
        } else if (isEntitySettingsRevenue(account)) {
            updateEntitySettingsRevenueFields(account, request);
        } else if (isEntitySettingsCost(account)) {
            updateEntitySettingsCostFields(account, request);
        } else {
            rejectUnsupportedFields(account, request, allNatureSpecificUpdateFields());
        }
    }

    private void updateApFields(Account account, AccountUpdateRequestDTO request) {
        applyOptionalCurrencyCode(request.getRateCurrency(), account::setRateCurrency, AccountMessageConstants.RATE_CURRENCY_NOT_FOUND);
        applyOptionalCurrencyCode(request.getBillingCurrency(), account::setBillingCurrency, AccountMessageConstants.BILLING_CURRENCY_NOT_FOUND);
    }

    private void updateArMasterFields(ARAccount master, AccountUpdateRequestDTO request) {
        applyOptionalCategoryCode(request.getBusinessSegment(), MasterCategoryConstants.BUSINESS_TYPE_AR,
                master::setBusinessSegment, AccountMessageConstants.BUSINESS_SEGMENT_NOT_FOUND);
        applyOptionalArExplicitBusinessType(request.getBusinessType(), master::setBusinessType);
        applyOptionalCurrencyCode(request.getRateCurrency(), master::setRateCurrency, AccountMessageConstants.RATE_CURRENCY_NOT_FOUND);
        applyOptionalCurrencyCode(request.getBillingCurrency(), master::setBillingCurrency, AccountMessageConstants.BILLING_CURRENCY_NOT_FOUND);
        applyOptionalCategoryCode(request.getCustomType(), MasterCategoryConstants.CUSTOMS_TYPE,
                master::setCustomType, AccountMessageConstants.CUSTOM_TYPE_NOT_FOUND);
    }

    private void updateArFields(Account account, AccountUpdateRequestDTO request) {
        applyOptionalArInternalAccountNumber(request.getArAccountNumber(), account::setArInternalAccountNumber);
        applyOptionalCategoryCode(request.getPotentialDelay(), MasterCategoryConstants.POTENTIAL_DELAY,
                account::setPotentialDelay, AccountMessageConstants.POTENTIAL_DELAY_NOT_FOUND);
        applyOptionalCategoryCode(request.getInvoiceAmountLimit(), MasterCategoryConstants.INVOICE_AMOUNT_LIMIT,
                account::setInvoiceAmountLimit, AccountMessageConstants.INVOICE_AMOUNT_LIMIT_NOT_FOUND);
        applyOptionalCategoryCode(request.getInvoiceShipmentLimit(), MasterCategoryConstants.INVOICE_SHIPMENT_LIMIT,
                account::setInvoiceShipmentLimit, AccountMessageConstants.INVOICE_SHIPMENT_LIMIT_NOT_FOUND);
        applyOptionalCategoryCode(request.getInvoiceBreakBy(), MasterCategoryConstants.INVOICE_BREAK_BY,
                account::setInvoiceBreakBy, AccountMessageConstants.INVOICE_BREAK_BY_NOT_FOUND);
        applyOptionalCategoryCode(request.getTriggerInvoice(), MasterCategoryConstants.INVOICE_TRIGGER,
                account::setTriggerInvoice, AccountMessageConstants.TRIGGER_INVOICE_NOT_FOUND);
        applyOptionalCategoryCode(request.getEligibleForBilling(), MasterCategoryConstants.ELIGIBLE_FOR_BILLING,
                account::setEligibleForBilling, AccountMessageConstants.ELIGIBLE_FOR_BILLING_NOT_FOUND);
        applyOptionalCategoryCode(request.getInvoiceFrequent(), MasterCategoryConstants.INVOICE_FREQUENCY,
                account::setInvoiceFrequent, AccountMessageConstants.INVOICE_FREQUENCY_NOT_FOUND);
    }

    private void updateEntitySettingsRevenueFields(Account account, AccountUpdateRequestDTO request) {

        applyOptionalCategoryCode(request.getSplitInvoice(), MasterCategoryConstants.SPLIT_INVOICE,
                account::setSplitInvoice, AccountMessageConstants.SPLIT_INVOICE_NOT_FOUND);

        applyOptionalCategoryCode(request.getTriggerInvoice(), MasterCategoryConstants.INVOICE_TRIGGER,
                account::setTriggerInvoice, AccountMessageConstants.TRIGGER_INVOICE_NOT_FOUND);

        applyOptionalCategoryCode(request.getInvoiceFrequent(), MasterCategoryConstants.INVOICE_FREQUENCY,
                account::setInvoiceFrequent, AccountMessageConstants.INVOICE_FREQUENCY_NOT_FOUND);

        applyOptionalCategoryCode(request.getEligibleForBilling(), MasterCategoryConstants.ELIGIBLE_FOR_BILLING,
                account::setEligibleForBilling, AccountMessageConstants.ELIGIBLE_FOR_BILLING_NOT_FOUND);

        applyOptionalPlainValue(request.getSapCountryCode(), account::setSapCountryCode, AccountConstants.FIELD_LABEL_SAP_COUNTRY_CODE);
        applyOptionalDefaultCashAccount(request.getDefaultCashAccount(), account::setDefaultCashAccount);
        applyOptionalPlainValue(request.getInvoiceLimit(), account::setInvoiceLimit, AccountConstants.FIELD_LABEL_INVOICE_LIMIT);
    }

    private void updateEntitySettingsCostFields(Account account, AccountUpdateRequestDTO request) {
        applyOptionalCategoryCode(request.getSplitEntry(), MasterCategoryConstants.SPLIT_ENTRY,
                account::setSplitEntry, AccountMessageConstants.SPLIT_ENTRY_NOT_FOUND);
        applyOptionalCategoryCode(request.getAccount(), MasterCategoryConstants.ACCOUNT,
                account::setAccount, DashboardMessageConstants.ACCOUNT_NOT_FOUND);
        applyOptionalPlainValue(request.getEntryLimit(), account::setEntryLimit, AccountConstants.FIELD_LABEL_ENTRY_LIMIT);
        applyOptionalPlainValue(request.getSapCountryCode(), account::setSapCountryCode, AccountConstants.FIELD_LABEL_SAP_COUNTRY_CODE);
        applyOptionalDefaultAccountNumber(request.getDefaultAccountNumber(), account::setDefaultAccountNumber);
    }

    private void rejectUnsupportedFields(
            Account account, AccountUpdateRequestDTO request, String... fieldNames) {
        for (String fieldName : fieldNames) {
            if (isUpdateFieldPresent(request, fieldName)) {
                rejectUnsupportedField(account, fieldName);
            }
        }
    }

    private String[] allNatureSpecificUpdateFields() {
        return new String[] {
                AccountConstants.UPDATE_FIELD_ACCOUNT_STATUS, AccountConstants.UPDATE_FIELD_ACCOUNT_TYPE, AccountConstants.UPDATE_FIELD_RATE_CURRENCY_CODE, AccountConstants.UPDATE_FIELD_BILLING_CURRENCY_CODE,
                AccountConstants.UPDATE_FIELD_MARKET_STRUCTURE_CODE, AccountConstants.UPDATE_FIELD_SPLIT_ENTRY_CODE, AccountConstants.UPDATE_FIELD_INVOICE_AMOUNT_LIMIT_CODE,
                AccountConstants.UPDATE_FIELD_POTENTIAL_DELAY_CODE, AccountConstants.UPDATE_FIELD_AMOUNT_CODE, AccountConstants.UPDATE_FIELD_INVOICE_SHIPMENT_LIMIT_CODE,
                AccountConstants.UPDATE_FIELD_INVOICE_BREAK_BY_CODE, AccountConstants.UPDATE_FIELD_ELIGIBLE_FOR_BILLING_CODE, AccountConstants.UPDATE_FIELD_TRIGGER_INVOICE_CODE,
                AccountConstants.UPDATE_FIELD_INVOICE_FREQUENT_CODE, AccountConstants.UPDATE_FIELD_INVOICE_LIMIT, AccountConstants.UPDATE_FIELD_SAP_COUNTRY_CODE, AccountConstants.UPDATE_FIELD_ENTRY_LIMIT,
                AccountConstants.UPDATE_FIELD_SPLIT_INVOICE
        };
    }

    private boolean isUpdateFieldPresent(AccountUpdateRequestDTO request, String fieldName) {
        return switch (fieldName) {
            case AccountConstants.UPDATE_FIELD_ACCOUNT_STATUS -> request.getAccountStatus() != null;
            case AccountConstants.UPDATE_FIELD_ACCOUNT_TYPE -> request.getAccountType() != null;
            case AccountConstants.UPDATE_FIELD_RATE_CURRENCY_CODE -> request.getRateCurrency() != null;
            case AccountConstants.UPDATE_FIELD_BILLING_CURRENCY_CODE -> request.getBillingCurrency() != null;
            case AccountConstants.UPDATE_FIELD_MARKET_STRUCTURE_CODE -> request.getMarketStructure() != null;
            case AccountConstants.UPDATE_FIELD_SPLIT_ENTRY_CODE -> request.getSplitEntry() != null;
            case AccountConstants.UPDATE_FIELD_INVOICE_AMOUNT_LIMIT_CODE -> request.getInvoiceAmountLimit() != null;
            case AccountConstants.UPDATE_FIELD_POTENTIAL_DELAY_CODE -> request.getPotentialDelay() != null;
            case AccountConstants.UPDATE_FIELD_AMOUNT_CODE -> request.getAccount() != null;
            case AccountConstants.UPDATE_FIELD_INVOICE_SHIPMENT_LIMIT_CODE -> request.getInvoiceShipmentLimit() != null;
            case AccountConstants.UPDATE_FIELD_INVOICE_BREAK_BY_CODE -> request.getInvoiceBreakBy() != null;
            case AccountConstants.UPDATE_FIELD_ELIGIBLE_FOR_BILLING_CODE -> request.getEligibleForBilling() != null;
            case AccountConstants.UPDATE_FIELD_TRIGGER_INVOICE_CODE -> request.getTriggerInvoice() != null;
            case AccountConstants.UPDATE_FIELD_INVOICE_FREQUENT_CODE -> request.getInvoiceFrequent() != null;
            case AccountConstants.UPDATE_FIELD_INVOICE_LIMIT -> request.getInvoiceLimit() != null;
            case AccountConstants.UPDATE_FIELD_SAP_COUNTRY_CODE -> request.getSapCountryCode() != null;
            case AccountConstants.UPDATE_FIELD_ENTRY_LIMIT -> request.getEntryLimit() != null;
            case AccountConstants.UPDATE_FIELD_SPLIT_INVOICE -> request.getSplitInvoice() != null;
            default -> false;
        };
    }

    private void applyRequiredCategoryCode(
            String code, String categoryCode, Consumer<String> setter, String notFoundMessage) {
        if (code != null) {
            setter.accept(requireValidatedCategoryCode(code, categoryCode, notFoundMessage));
        }
    }

    private void validateAddAccountRequest(AccountAddRequestDTO request) {
        validateAccountNumber(request.getAccountNumber());
        validateAccountName(request.getAccountName());
        requireNonBlank(request.getCountry(), AccountConstants.FIELD_LABEL_COUNTRY, AccountMessageConstants.COUNTRY_IS_REQUIRED);
        requireNonBlank(request.getEntity(), AccountConstants.FIELD_LABEL_ENTITY, AccountMessageConstants.ENTITY_IS_REQUIRED);
        requireNonBlank(request.getRateCurrency(), AccountConstants.FIELD_LABEL_RATE_CURRENCY, AccountMessageConstants.RATE_CURRENCY_IS_REQUIRED);
        requireNonBlank(request.getBillingCurrency(), AccountConstants.FIELD_LABEL_BILLING_CURRENCY, AccountMessageConstants.BILLING_CURRENCY_IS_REQUIRED);
        requireNonBlank(request.getStatus(), AccountConstants.FIELD_LABEL_STATUS, AccountMessageConstants.STATUS_IS_REQUIRED);
        requireNonBlank(request.getAccountType(), AccountConstants.FIELD_LABEL_ACCOUNT_TYPE, AccountMessageConstants.ACCOUNT_TYPE_IS_REQUIRED);
    }

    private void validateUpdateAccountRequest(AccountUpdateRequestDTO request) {
        if (request.getId() == null
                && (request.getAccountNature() == null
                        || (!AccountNature.AR.equals(request.getAccountNature())
                                && !AccountNature.AP.equals(request.getAccountNature())
                                && !AccountNature.ENTITY_SETTINGS.equals(request.getAccountNature())))) {
            throw new BadRequestException(
                    AccountMessageConstants.ID_OR_ACCOUNT_KEYS_REQUIRED);
        }
    }

    private boolean isEntitySettingsUpdate(AccountUpdateRequestDTO request) {
        if (AccountNature.ENTITY_SETTINGS.equals(request.getAccountNature())) {
            return true;
        }
        if (request.getId() != null) {
            return accountRepository
                    .findById(request.getId())
                    .map(account -> AccountNature.ENTITY_SETTINGS.equals(account.getAccountNature()))
                    .orElse(false);
        }
        return false;
    }

    private void validateAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new BadRequestException(AccountMessageConstants.ACCOUNT_NUMBER_REQUIRED);
        }
        int length = accountNumber.trim().length();
        if (length < AccountConstants.ACCOUNT_NUMBER_MIN_LENGTH
                || length > AccountConstants.ACCOUNT_NUMBER_MAX_LENGTH) {
            throw new BadRequestException(
                    AccountMessageConstants.ACCOUNT_NUMBER_LENGTH_PREFIX
                            + AccountConstants.ACCOUNT_NUMBER_MIN_LENGTH
                            + AccountMessageConstants.ACCOUNT_NUMBER_LENGTH_AND
                            + AccountConstants.ACCOUNT_NUMBER_MAX_LENGTH
                            + AccountMessageConstants.ACCOUNT_NUMBER_LENGTH_SUFFIX);
        }
    }

    private void validateAccountName(String accountName) {
        if (accountName == null || accountName.isBlank()) {
            throw new BadRequestException(AccountMessageConstants.ACCOUNT_NAME_REQUIRED);
        }
        int length = accountName.trim().length();
        if (length < AccountConstants.ACCOUNT_NAME_MIN_LENGTH
                || length > AccountConstants.ACCOUNT_NAME_MAX_LENGTH) {
            throw new BadRequestException(
                    AccountMessageConstants.ACCOUNT_NAME_LENGTH_PREFIX
                            + AccountConstants.ACCOUNT_NAME_MIN_LENGTH
                            + AccountMessageConstants.ACCOUNT_NUMBER_LENGTH_AND
                            + AccountConstants.ACCOUNT_NAME_MAX_LENGTH
                            + AccountMessageConstants.ACCOUNT_NUMBER_LENGTH_SUFFIX);
        }
    }

    private void requireNonBlank(String value, String fieldLabel, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(fieldLabel + AccountMessageConstants.FIELD_LABEL_SEPARATOR + message);
        }
    }

    private void applyDefaultAccountNumber(String value, Consumer<String> setter) {
        if (Objects.isNull(value) || value.isBlank()) {
            throw new BadRequestException(AccountMessageConstants.DEFAULT_ACCOUNT_NUMBER_BLANK);
        }

        String trimmedValue = value.trim();
        if (trimmedValue.length() > AccountConstants.DEFAULT_ACCOUNT_NUMBER_MAX_LENGTH) {
            throw new BadRequestException(AccountMessageConstants.DEFAULT_ACCOUNT_NUMBER_MAX_LENGTH);
        }

        setter.accept(trimmedValue);
    }

    private void applyOptionalDefaultAccountNumber(String value, Consumer<String> setter) {
        if (value == null || value.isBlank()) {
            setter.accept(null);
            return;
        }
        applyDefaultAccountNumber(value, setter);
    }

    private void applyOptionalDefaultCashAccount(String value, Consumer<String> setter) {
        if (value == null || value.isBlank()) {
            setter.accept(null);
            return;
        }
        applyPlainValue(value, setter, AccountConstants.FIELD_LABEL_DEFAULT_CASH_ACCOUNT);
    }

    private void applyOptionalPlainValue(String value, Consumer<String> setter, String fieldName) {
        if (value == null || value.isBlank()) {
            setter.accept(null);
            return;
        }
        applyPlainValue(value, setter, fieldName);
    }

    private void applyOptionalArInternalAccountNumber(String value, Consumer<String> setter) {
        if (value == null || value.isBlank()) {
            setter.accept(null);
            return;
        }
        applyPlainValue(value, setter, AccountConstants.FIELD_LABEL_AR_ACCOUNT_NUMBER);
    }

    private void applyPlainValue(String value,
                                 Consumer<String> setter,
                                 String fieldName) {

        if (Objects.isNull(value) || value.isBlank()) {
            throw new BadRequestException(fieldName + AccountMessageConstants.FIELD_MUST_NOT_BE_BLANK_SUFFIX);
        }

        String trimmedValue = value.trim();

        if (trimmedValue.length() > AccountConstants.PLAIN_TEXT_MAX_LENGTH) {
            throw new BadRequestException(
                    fieldName + AccountMessageConstants.FIELD_MAX_LENGTH_PREFIX
                            + AccountConstants.PLAIN_TEXT_MAX_LENGTH
                            + AccountMessageConstants.FIELD_MAX_LENGTH_SUFFIX);
        }

        if (!trimmedValue.matches(AccountConstants.PLAIN_TEXT_PATTERN)) {
            throw new BadRequestException(
                    fieldName + AccountMessageConstants.FIELD_INVALID_CHARACTERS_SUFFIX
            );
        }

        setter.accept(trimmedValue);
    }

    @Override
    @Transactional(readOnly = false)
    @Caching(evict = {
            @CacheEvict(cacheNames = DashboardCacheNames.ACCOUNT_SEARCH, allEntries = true),
            @CacheEvict(cacheNames = DashboardCacheNames.AP_ACCOUNTS_BY_ENTITY, key = "#request.entity.trim()"),
            @CacheEvict(
                    cacheNames = DashboardCacheNames.ACCOUNT_DETAILS,
                    key = "{#request.accountNumber.trim(), T(com.customcharge.persistencelayer.enums.AccountNature).AP, #request.entity.trim(), 'NA'}")
    })
    public MessageResponse addAccount(AccountAddRequestDTO request) {
        validateAddAccountRequest(request);

        requireActiveCountryAndEntityFromMasterLookup(
                request.getCountry().trim(), request.getEntity().trim());
        String accountNumber = request.getAccountNumber().trim();
        String entityCode = request.getEntity().trim();
        if (apAccountRepository.findByAccountNumberAndEntityCode(accountNumber, entityCode).isPresent()) {
            throw new BadRequestException(AccountMessageConstants.ACCOUNT_ALREADY_EXISTS);
        }

        Entity entityRef = requireActiveEntityByCode(entityCode);
        APAccount apMaster = ApAccountMasterSupport.findOrCreateMaster(
                apAccountRepository,
                entityCode,
                accountNumber,
                request.getAccountName().trim());
        apMaster.setAccountType(request.getAccountType());
        apMaster.setStatus(request.getStatus());
        if (StringUtils.hasText(request.getMarketStructure())) {
            apMaster.setMarketStructure(requireValidatedCategoryCode(
                    request.getMarketStructure(), MasterCategoryConstants.MARKET_STRUCTURE, AccountMessageConstants.MARKET_STRUCTURE_NOT_FOUND));
        }
        apAccountRepository.save(apMaster);

        Account config = ApAccountMasterSupport.findOrCreateChargeConfig(
                accountRepository, apMaster, entityRef, null);
        config.setRateCurrency(requireValidatedCurrencyCode(
                request.getRateCurrency(), AccountMessageConstants.RATE_CURRENCY_NOT_FOUND));
        config.setBillingCurrency(requireValidatedCurrencyCode(
                request.getBillingCurrency(), AccountMessageConstants.BILLING_CURRENCY_NOT_FOUND));
        AccountVersionSupport.ensureInitialized(config);
        accountRepository.save(config);

        writeApMasterCache(apMaster);
        return new MessageResponse(DashboardMessageConstants.SUCCESS);
    }

    private AccountDetailsResponseDTO mapToResponse(Account account, AccountNature nature) {

        AccountDetailsResponseDTO dto = new AccountDetailsResponseDTO();
        dto.setId(account.getId());
        dto.setChargeType(account.getChargeType());
        rateSheetRepository.findByAccount_Id(account.getId()).ifPresent(rs -> dto.setRateSheetId(rs.getId()));
        if (nature == AccountNature.AP) {
            mapApResponse(account, dto);
            dto.setDropdownValues(apDropdownValues());
        } else if (nature == AccountNature.AR) {
            mapArResponse(account, dto);
            dto.setDropdownValues(arDropdownValues());
        } else if (nature == AccountNature.ENTITY_SETTINGS
                && ChargeTypeConstants.ENTITY_SETTINGS_REVENUE.contains(account.getChargeType())) {
            mapEntitySettingsResponse(account, dto);
            dto.setDropdownValues(entitySettingsDropdownValuesRevenue());
        } else if (nature == AccountNature.ENTITY_SETTINGS
                && ChargeTypeConstants.ENTITY_SETTINGS_COST.contains(account.getChargeType())) {
            mapEntitySettingsResponse(account, dto);
            dto.setDropdownValues(entitySettingsDropdownValuesCost());
        }
        else {
            dto.setDropdownValues(Map.of());
        }
        return dto;
    }

    private void mapApResponse(Account account, AccountDetailsResponseDTO dto) {
        if (account.getApAccount() != null) {
            dto.setAccountNumber(account.getApAccount().getAccountNumber());
            dto.setAccountName(account.getApAccount().getAccountName());
            dto.setEntity(account.getApAccount().getEntityCode());
            dto.setAccountType(account.getApAccount().getAccountType());
            dto.setMarketStructure(account.getApAccount().getMarketStructure());
            dto.setStatus(account.getApAccount().getStatus());
            dto.setCountry(account.getCountryCode());
        }
        dto.setRateCurrency(account.getRateCurrency());
        dto.setBillingCurrency(account.getBillingCurrency());
    }

    private AccountDetailsResponseDTO mapApMasterOnly(APAccount master, String chargeType) {
        AccountDetailsResponseDTO dto = new AccountDetailsResponseDTO();
        dto.setChargeType(chargeType);
        dto.setAccountNumber(master.getAccountNumber());
        dto.setAccountName(master.getAccountName());
        dto.setEntity(master.getEntityCode());
        dto.setAccountType(master.getAccountType());
        dto.setMarketStructure(master.getMarketStructure());
        dto.setStatus(master.getStatus());
        return dto;
    }

    private AccountDetailsResponseDTO mapArMasterOnly(ARAccount master, String chargeType) {
        AccountDetailsResponseDTO dto = new AccountDetailsResponseDTO();
        dto.setAccountNumber(master.getAccountNumber());
        dto.setAccountName(master.getAccountName());
        dto.setEntity(master.getEntityCode());
        dto.setCountry(master.getCountryCode());
        dto.setAccountType(master.getAccountType());
        dto.setArAccountNumber(master.getArAccountNumber());
        dto.setStatus(master.getStatus());
        dto.setChargeType(chargeType);
        applyArBusinessFields(dto, master.getBusinessType(), master.getBusinessSegment());
        dto.setRateCurrency(master.getRateCurrency());
        dto.setBillingCurrency(master.getBillingCurrency());
        dto.setCustomsType(master.getCustomType());
        return dto;
    }

    private AccountDetailsResponseDTO mapEntitySettingsStub(Entity entity, String chargeType) {
        AccountDetailsResponseDTO dto = new AccountDetailsResponseDTO();
        dto.setEntity(entity.getCode());
        dto.setCountry(
                entity.getCountry() == null ? null : entity.getCountry().getCode());
        dto.setChargeType(chargeType);
        return dto;
    }

    private void mapArResponse(Account account, AccountDetailsResponseDTO dto) {
        if (account.getArAccount() != null) {
            dto.setAccountNumber(account.getArAccount().getAccountNumber());
            dto.setAccountName(account.getArAccount().getAccountName());
            dto.setAccountType(account.getARAccountType());
            dto.setStatus(account.getArAccount().getStatus());
        } else {
            dto.setAccountNumber(account.getAccountNumber());
            dto.setAccountName(account.getAccountName());
        }
        dto.setEntity(account.getEntityCode());
        dto.setCountry(account.getCountryCode());
        ARAccount master = account.getArAccount();
        applyArBusinessFields(dto, master != null ? master.getBusinessType() : null,
                master != null ? master.getBusinessSegment() : null);
        dto.setArAccountNumber(account.getArInternalAccountNumber());
        dto.setRateCurrency(ArAccountMasterSupport.resolveRateCurrency(master));
        dto.setBillingCurrency(ArAccountMasterSupport.resolveBillingCurrency(master));
        dto.setInvoiceFrequency(account.getInvoiceFrequent());
        dto.setCustomsType(master != null ? master.getCustomType() : null);
        dto.setEligibleForBilling(account.getEligibleForBilling());
        dto.setInvoiceTrigger(account.getTriggerInvoice());
        dto.setInvoiceBreakBy(account.getInvoiceBreakBy());
        dto.setInvoiceShipmentLimit(account.getInvoiceShipmentLimit());
        dto.setInvoiceLimit(account.getInvoiceAmountLimit());
        dto.setPotentialDelay(account.getPotentialDelay());
        if (dto.getAccountType() == null) {
            dto.setAccountType(account.getARAccountType());
        }
    }

    private static void applyArBusinessFields(
            AccountDetailsResponseDTO dto, String storedBusinessType, String businessSegment) {
        if (StringUtils.hasText(businessSegment)) {
            dto.setBusinessSegment(businessSegment.trim());
        }
        dto.setBusinessType(ArShipmentBusinessTypeResolver.resolveEffective(storedBusinessType, businessSegment));
    }

    private void mapEntitySettingsResponse(Account account, AccountDetailsResponseDTO dto) {
        dto.setId(account.getId());
        dto.setChargeType(account.getChargeType());
        dto.setCountry(account.getCountryCode());
        dto.setSplitEntry(account.getSplitEntry());
        dto.setDefaultCashAccount(account.getDefaultCashAccount());
        dto.setInvoiceLimit(account.getInvoiceLimit());
        dto.setInvoiceFrequency(account.getInvoiceFrequent());
        dto.setAccount(account.getAccount());
        dto.setEntity(account.getEntityCode());
        dto.setEligibleForBilling(account.getEligibleForBilling());
        dto.setInvoiceTrigger(account.getInvoiceTrigger());
        dto.setSapCompanyCode(account.getSapCountryCode());
        dto.setEntryLimit(account.getEntryLimit());
        dto.setSplitInvoice(account.getSplitInvoice());
        dto.setDefaultAccountNumber(account.getDefaultAccountNumber());
    }

    @Override
    public List<String> masterCodes(String categoryCode) {
        return masterCategoryValueLookupService.listForCategory(categoryCode).stream()
                .map(MasterCategoryValueRow::name)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(code -> !code.isEmpty())
                .toList();
    }

    private Map<String, List<String>> apDropdownValues() {
        Map<String, List<String>> m = new LinkedHashMap<>();
        m.put(AccountConstants.DROPDOWN_KEY_RATE_CURRENCY, activeCurrencyCodes());
        m.put(AccountConstants.DROPDOWN_KEY_BILLING_CURRENCY, activeCurrencyCodes());
        m.put(AccountConstants.DROPDOWN_KEY_MARKET_STRUCTURE, masterCodes(MasterCategoryConstants.MARKET_STRUCTURE));
        m.put(AccountConstants.DROPDOWN_KEY_STATUS, masterCodes(MasterCategoryConstants.ACCOUNT_STATUS));
        return m;
    }

    private Map<String, List<String>> arDropdownValues() {
        Map<String, List<String>> m = new LinkedHashMap<>();
        m.put(AccountConstants.DROPDOWN_KEY_INVOICE_SHIPMENT_LIMIT, masterCodes(MasterCategoryConstants.INVOICE_SHIPMENT_LIMIT));
        m.put(AccountConstants.DROPDOWN_KEY_INVOICE_AMOUNT_LIMIT, masterCodes(MasterCategoryConstants.INVOICE_AMOUNT_LIMIT));
        m.put(AccountConstants.DROPDOWN_KEY_POTENTIAL_DELAY, masterCodes(MasterCategoryConstants.POTENTIAL_DELAY));
        m.put(AccountConstants.DROPDOWN_KEY_CUSTOMS_TYPE, masterCodes(MasterCategoryConstants.CUSTOMS_TYPE));
        m.put(AccountConstants.DROPDOWN_KEY_BUSINESS_TYPE, arBusinessTypeDropdownValues());
        m.put(AccountConstants.DROPDOWN_KEY_BUSINESS_SEGMENT, masterCodes(MasterCategoryConstants.BUSINESS_TYPE_AR));
        m.put(AccountConstants.DROPDOWN_KEY_ELIGIBLE_FOR_BILLING, masterCodes(MasterCategoryConstants.ELIGIBLE_FOR_BILLING));
        m.put(AccountConstants.DROPDOWN_KEY_INVOICE_TRIGGER, masterCodes(MasterCategoryConstants.INVOICE_TRIGGER));
        m.put(AccountConstants.DROPDOWN_KEY_INVOICE_BREAK_BY, masterCodes(MasterCategoryConstants.INVOICE_BREAK_BY));
        m.put(AccountConstants.DROPDOWN_KEY_RATE_CURRENCY, activeCurrencyCodes());
        m.put(AccountConstants.DROPDOWN_KEY_BILLING_CURRENCY, activeCurrencyCodes());
        m.put(AccountConstants.DROPDOWN_KEY_INVOICE_FREQUENCY, masterCodes(MasterCategoryConstants.INVOICE_FREQUENCY));
        return m;
    }

    private List<String> arBusinessTypeDropdownValues() {
        return masterCodes(MasterCategoryConstants.BUSINESS_TYPE).stream()
                .filter(ArShipmentBusinessTypeResolver::isExplicitBusinessType)
                .toList();
    }

    private Map<String, List<String>> entitySettingsDropdownValuesCost() {
        Map<String, List<String>> m = new LinkedHashMap<>();
        m.put(AccountConstants.DROPDOWN_KEY_SPLIT_ENTRY, masterCodes(MasterCategoryConstants.SPLIT_ENTRY));
        m.put(AccountConstants.DROPDOWN_KEY_ACCOUNT, masterCodes(MasterCategoryConstants.ACCOUNT));
        return m;
    }

    private Map<String, List<String>> entitySettingsDropdownValuesRevenue() {
        Map<String, List<String>> m = new LinkedHashMap<>();
        m.put(AccountConstants.DROPDOWN_KEY_SPLIT_INVOICE, masterCodes(MasterCategoryConstants.SPLIT_INVOICE));
        m.put(AccountConstants.DROPDOWN_KEY_INVOICE_FREQUENCY_SNAKE, masterCodes(MasterCategoryConstants.INVOICE_FREQUENCY));
        m.put(AccountConstants.DROPDOWN_KEY_ACCOUNT, masterCodes(MasterCategoryConstants.ACCOUNT));
        m.put(AccountConstants.DROPDOWN_KEY_TRIGGER_INVOICE, masterCodes(MasterCategoryConstants.INVOICE_TRIGGER));
        m.put(AccountConstants.DROPDOWN_KEY_ELIGIBLE_FOR_BILLING_SNAKE, masterCodes(MasterCategoryConstants.ELIGIBLE_FOR_BILLING));
        return m;
    }

    private boolean isEntitySettingsRevenue(Account account) {
        return account.getAccountNature() == AccountNature.ENTITY_SETTINGS
                && account.getChargeType() != null
                && ChargeTypeConstants.ENTITY_SETTINGS_REVENUE.contains(account.getChargeType());
    }

    private boolean isEntitySettingsCost(Account account) {
        return account.getAccountNature() == AccountNature.ENTITY_SETTINGS
                && account.getChargeType() != null
                && ChargeTypeConstants.ENTITY_SETTINGS_COST.contains(account.getChargeType());
    }

    private void rejectUnsupportedField(Account account, String fieldName) {
        String detail = account.getAccountNature() == AccountNature.ENTITY_SETTINGS
                ? AccountMessageConstants.FIELD_NOT_APPLICABLE_CHARGE_TYPE_PREFIX + account.getChargeType() + AccountMessageConstants.FIELD_NOT_APPLICABLE_CHARGE_TYPE_SUFFIX
                : "";
        throw new BadRequestException(fieldName + AccountMessageConstants.FIELD_NOT_APPLICABLE_SUFFIX + detail);
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    /**
     * Validates {@code code} against master data for {@code categoryCode} (Redis / DB), returns trimmed code to persist.
     */
    private String requireValidatedCategoryCode(String code, String categoryCode, String notFoundMessage) {
        if (!hasText(code)) {
            throw new BadRequestException(notFoundMessage);
        }
        String trimmed = code.trim();
        masterCategoryValueLookupService.requireActiveCodeInCategory(categoryCode, trimmed, notFoundMessage);
        return trimmed;
    }

    private void applyOptionalCategoryCode(
            String code,
            String categoryCode,
            Consumer<String> setter,
            String notFoundMessage) {
        if (code == null || code.isBlank()) {
            setter.accept(null);
            return;
        }
        String trimmed = code.trim();
        masterCategoryValueLookupService.requireActiveCodeInCategory(categoryCode, trimmed, notFoundMessage);
        setter.accept(trimmed);
    }

    private void applyOptionalArExplicitBusinessType(String code, Consumer<String> setter) {
        if (code == null || code.isBlank()) {
            setter.accept(null);
            return;
        }
        String validated =
                requireValidatedCategoryCode(
                        code, MasterCategoryConstants.BUSINESS_TYPE, AccountMessageConstants.BUSINESS_TYPE_NOT_FOUND);
        if (!ArShipmentBusinessTypeResolver.isExplicitBusinessType(validated)) {
            throw new BadRequestException(AccountMessageConstants.AR_BUSINESS_TYPE_INVALID);
        }
        setter.accept(validated);
    }

    private List<String> activeCurrencyCodes() {
        return activeCurrencyLookupService.listActiveCurrencyCodes();
    }

    private String requireValidatedCurrencyCode(String code, String notFoundMessage) {
        if (!hasText(code)) {
            throw new BadRequestException(notFoundMessage);
        }
        return activeCurrencyLookupService.requireActiveCurrencyCodeReturning(code, notFoundMessage);
    }

    private void applyRequiredCurrencyCode(String code, Consumer<String> setter, String notFoundMessage) {
        if (code != null) {
            setter.accept(requireValidatedCurrencyCode(code, notFoundMessage));
        }
    }

    private void applyOptionalCurrencyCode(String code, Consumer<String> setter, String notFoundMessage) {
        if (code == null || code.isBlank()) {
            setter.accept(null);
            return;
        }
        setter.accept(requireValidatedCurrencyCode(code, notFoundMessage));
    }

    private static String nullIfBlank(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

}
