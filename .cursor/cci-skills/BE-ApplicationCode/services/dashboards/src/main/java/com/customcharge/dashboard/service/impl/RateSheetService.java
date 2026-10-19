package com.customcharge.dashboard.service.impl;

import com.customcharge.dashboard.constants.CacheKeyConstants;
import com.customcharge.dashboard.constants.DashboardMessageConstants;
import com.customcharge.dashboard.constants.MasterCategoryConstants;
import com.customcharge.dashboard.constants.RateSheetConstants;
import com.customcharge.dashboard.constants.RateSheetMessageConstants;
import com.customcharge.dashboard.constants.ServiceChargeMessageConstants;
import com.customcharge.dashboard.dto.*;
import com.customcharge.dashboard.service.AccountRateSheetResolver;
import com.customcharge.dashboard.service.AccountService;
import com.customcharge.dashboard.service.RateSheetItemDuplicateValidator;
import com.customcharge.dashboard.service.RateSheetItemFieldValidation;
import com.customcharge.dashboard.service.RateSheetRateValidation;
import com.customcharge.persistencelayer.dto.response.MessageResponse;
import com.customcharge.persistencelayer.entities.*;
import com.customcharge.persistencelayer.enums.AccountNature;
import com.customcharge.persistencelayer.enums.PercentageOf;
import com.customcharge.persistencelayer.enums.RateType;
import com.customcharge.persistencelayer.exception.BadRequestException;
import com.customcharge.persistencelayer.exception.ResourceNotFoundException;
import com.customcharge.persistencelayer.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.customcharge.dashboard.cache.DashboardCacheNames;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RateSheetService {

    private final RateSheetRepository rateSheetRepository;
    private final RateSheetItemRepository rateSheetItemRepository;
    private final RateSheetItemFieldValidation fieldValidation;
    private final RateSheetItemDuplicateValidator duplicateValidator;
    private final AccountService accountService;
    private final AccountRateSheetResolver accountRateSheetResolver;
    private final ServiceChargeService serviceChargeService;

    @Transactional(readOnly = false)
    @Caching(evict = {
            @CacheEvict(cacheNames = DashboardCacheNames.RATE_SHEET_ITEMS, allEntries = true),
            @CacheEvict(cacheNames = DashboardCacheNames.RATE_SHEET_ITEM_BY_ID, allEntries = true),
            @CacheEvict(cacheNames = DashboardCacheNames.RATE_SHEET_DROPDOWN, allEntries = true),
            @CacheEvict(cacheNames = DashboardCacheNames.ACCOUNT_DETAILS, allEntries = true)
    })
    public CreateRateSheetItemResponse createItem(CreateRateSheetItemRequest req) {
        RateSheet rateSheet = resolveRateSheet(req);
        Account account = requireAccount(rateSheet);

        RateSheetItemFieldValidation.ValidatedFields validated = fieldValidation.validateForCreate(
                rateSheet.getId(),
                account.getId().toString(),
                account.getEntityCode(),
                account.getChargeType(),
                req.getServiceCode(),
                req.getShipmentType(),
                req.getBusinessType(),
                req.getCategoryType());

        RateSheetItem item = new RateSheetItem();
        item.setRateSheet(rateSheet);
        mapItemFields(item, req, validated);
        syncRates(item, req.getRates());
        RateSheetItem saved = rateSheetItemRepository.save(item);
        syncAccountRedisForAccount(account);
        return toCreateItemResponse(saved);
    }

    @Transactional(readOnly = false)
    @Caching(evict = {
            @CacheEvict(cacheNames = DashboardCacheNames.RATE_SHEET_ITEMS, allEntries = true),
            @CacheEvict(cacheNames = DashboardCacheNames.RATE_SHEET_ITEM_BY_ID, allEntries = true),
            @CacheEvict(cacheNames = DashboardCacheNames.RATE_SHEET_DROPDOWN, allEntries = true),
            @CacheEvict(cacheNames = DashboardCacheNames.ACCOUNT_DETAILS, allEntries = true)
    })
    public List<CreateRateSheetItemResponse> updateItems(List<CreateRateSheetItemRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new BadRequestException(RateSheetMessageConstants.AT_LEAST_ONE_ITEM_REQUIRED);
        }
        Set<String> entityRedisKeys = new LinkedHashSet<>();
        Set<String> arRedisKeys = new LinkedHashSet<>();
        List<ResolvedUpdateRequest> resolvedRequests = new ArrayList<>(requests.size());
        for (CreateRateSheetItemRequest req : requests) {
            RateSheet rateSheet = resolveRateSheet(req);
            Account account = requireAccount(rateSheet);
            recordRedisSyncTargetForAccount(account, entityRedisKeys, arRedisKeys);
            RateSheetItemFieldValidation.ValidatedFields validated = fieldValidation.normalizeFields(
                    account.getId().toString(),
                    account.getEntityCode(),
                    account.getChargeType(),
                    req.getServiceCode(),
                    req.getShipmentType(),
                    req.getBusinessType(),
                    req.getCategoryType(),
                    null);
            req.setRateSheetId(rateSheet.getId());
            resolvedRequests.add(new ResolvedUpdateRequest(req, validated));
        }
        duplicateValidator.assertNoDuplicatesInBatch(resolvedRequests.stream()
                .map(r -> new RateSheetItemDuplicateValidator.ItemKey(
                        r.validated().serviceCode(),
                        r.validated().shipmentType(),
                        r.validated().businessType(),
                        r.validated().categoryType()))
                .toList());
        List<CreateRateSheetItemResponse> responses = new ArrayList<>(resolvedRequests.size());
        for (ResolvedUpdateRequest resolved : resolvedRequests) {
            responses.add(updateItem(resolved.request(), resolved.validated()));
        }
        syncRedisTargets(entityRedisKeys, arRedisKeys);
        return responses;
    }

    private RateSheet resolveRateSheet(CreateRateSheetItemRequest req) {
        if (req.getRateSheetId() != null) {
            return rateSheetRepository
                    .findById(req.getRateSheetId())
                    .orElseThrow(() -> new ResourceNotFoundException(DashboardMessageConstants.RATE_SHEET_NOT_FOUND));
        }
        return accountRateSheetResolver.resolveOrCreateRateSheet(
                req.getAccountNature(), req.getAccountNumber(), req.getEntity(), req.getChargeType());
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

    private CreateRateSheetItemResponse updateItem(
            CreateRateSheetItemRequest req,
            RateSheetItemFieldValidation.ValidatedFields normalized) {
        if (req.getRateSheetItemId() == null) {
            throw new BadRequestException(RateSheetMessageConstants.RATE_SHEET_ITEM_ID_REQUIRED);
        }

        RateSheetItem item = rateSheetItemRepository.findWithRatesById(req.getRateSheetItemId())
                .orElseThrow(() -> new ResourceNotFoundException(RateSheetMessageConstants.ITEM_NOT_FOUND));

        duplicateValidator.assertNotDuplicateOnUpdate(
                req.getRateSheetId(),
                req.getRateSheetItemId(),
                normalized.serviceCode(),
                normalized.shipmentType(),
                normalized.businessType(),
                normalized.categoryType());

        mapItemFields(item, req, normalized);
        syncRates(item, req.getRates());
        RateSheetItem saved = rateSheetItemRepository.save(item);
        return toCreateItemResponse(saved);
    }

    @Transactional(readOnly = false)
    @Caching(evict = {
            @CacheEvict(cacheNames = DashboardCacheNames.RATE_SHEET_ITEMS, allEntries = true),
            @CacheEvict(cacheNames = DashboardCacheNames.RATE_SHEET_ITEM_BY_ID, allEntries = true),
            @CacheEvict(cacheNames = DashboardCacheNames.RATE_SHEET_DROPDOWN, allEntries = true),
            @CacheEvict(cacheNames = DashboardCacheNames.ACCOUNT_DETAILS, allEntries = true)
    })
    public void deleteItem(UUID itemId) {
        RateSheetItem item = rateSheetItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException(RateSheetMessageConstants.ITEM_NOT_FOUND));
        Account account = resolveAccountForRateSheetItem(item);
        item.setIsDeleted(true);
        rateSheetItemRepository.save(item);
        syncAccountRedisForAccount(account);
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

    @Cacheable(cacheNames = DashboardCacheNames.RATE_SHEET_ITEMS, key = "#rateSheetId")
    public List<RateSheetItemResponse> getRateSheet(UUID rateSheetId) {
        return rateSheetItemRepository.findByRateSheet_IdAndIsDeletedFalse(rateSheetId)
                .stream()
                .map(this::toItemResponse)
                .toList();
    }

    @Transactional(readOnly = false)
    public List<RateSheetItemResponse> getRateSheetByAccountContext(
            com.customcharge.persistencelayer.enums.AccountNature accountNature,
            String accountNumber,
            String entity,
            String chargeType) {
        RateSheet rateSheet =
                accountRateSheetResolver.resolveOrCreateRateSheet(accountNature, accountNumber, entity, chargeType);
        return getRateSheet(rateSheet.getId());
    }

    public List<RateSheetItemResponse> getRateSheet(UUID accountId, String chargeName) {
        RateSheet rateSheet = rateSheetRepository.findByAccount_Id(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(DashboardMessageConstants.ACCOUNT_NOT_FOUND));

        return rateSheetItemRepository.findByRateSheetId(rateSheet.getId())
                .stream()
                .map(this::toItemResponse)
                .toList();
    }

    @Transactional(readOnly = false)
    @Caching(evict = {
            @CacheEvict(cacheNames = DashboardCacheNames.RATE_SHEET_ITEMS, allEntries = true),
            @CacheEvict(cacheNames = DashboardCacheNames.RATE_SHEET_ITEM_BY_ID, allEntries = true),
            @CacheEvict(cacheNames = DashboardCacheNames.RATE_SHEET_DROPDOWN, allEntries = true),
            @CacheEvict(cacheNames = DashboardCacheNames.ACCOUNT_DETAILS, allEntries = true)
    })
    public MessageResponse upsert(RateConfigRequest req) {
        RateSheetItem item = rateSheetItemRepository.findWithRatesById(req.getRateSheetItemId())
                .orElseThrow(() -> new ResourceNotFoundException(RateSheetMessageConstants.ITEM_NOT_FOUND));

        List<RateSheetItemRateRequest> rates = new ArrayList<>();
        if (item.getRates() != null) {
            for (RateSheetItemRate existing : item.getRates()) {
                if (req.getRateId() != null && req.getRateId().equals(existing.getId())) {
                    continue;
                }
                RateSheetItemRateRequest existingRequest = new RateSheetItemRateRequest();
                existingRequest.setId(existing.getId());
                existingRequest.setRateType(existing.getRateType().getDisplayName());
                existingRequest.setPercentageOf(
                        existing.getPercentageOf() != null ? existing.getPercentageOf().getDisplayName() : null);
                existingRequest.setValue(existing.getValue());
                rates.add(existingRequest);
            }
        }

        RateSheetItemRateRequest rateRequest = new RateSheetItemRateRequest();
        rateRequest.setId(req.getRateId());
        rateRequest.setRateType(req.getRateType());
        rateRequest.setPercentageOf(req.getPercentageOf());
        rateRequest.setValue(req.getValue());
        rates.add(rateRequest);

        syncRates(item, rates);
        rateSheetItemRepository.save(item);
        syncAccountRedisForAccount(resolveAccountForRateSheetItem(item));
        return new MessageResponse(DashboardMessageConstants.UPDATED_SUCCESSFULLY);
    }

    public Map<String, Object> buildResponse() {
        return buildResponse(null, null, null, null, null);
    }

    public Map<String, Object> buildResponse(String accountId, String entityCode, String chargeType) {
        return buildResponse(accountId, null, null, entityCode, chargeType);
    }

    @Cacheable(
            cacheNames = DashboardCacheNames.RATE_SHEET_DROPDOWN,
            key = "{#accountId ?: 'NA', #accountNumber ?: 'NA', #accountNature != null ? #accountNature.name() : 'NA', #entityCode ?: 'NA', #chargeType ?: 'NA'}")
    public Map<String, Object> buildResponse(
            String accountId,
            String accountNumber,
            AccountNature accountNature,
            String entityCode,
            String chargeType) {
        String resolvedAccountId = accountId;
        if (!hasAccountId(resolvedAccountId) && accountNature != null) {
            resolvedAccountId =
                    accountRateSheetResolver
                            .findExistingChargeConfigAccountId(
                                    accountNature, accountNumber, entityCode, chargeType)
                            .map(UUID::toString)
                            .orElse(null);
        }

        Map<String, Object> dropdowns = new HashMap<>();
        dropdowns.put(RateSheetConstants.DROPDOWN_KEY_RATE_TYPES,
                Arrays.stream(RateType.values()).map(RateType::getDisplayName).toList());
        dropdowns.put(RateSheetConstants.DROPDOWN_KEY_PERCENTAGE_OF,
                Arrays.stream(PercentageOf.values()).map(PercentageOf::getDisplayName).toList());
        List<String> shipmentType = new ArrayList<>(
                List.of(RateSheetConstants.VALUE_ALL)
        );
        shipmentType.addAll(
                accountService.masterCodes(MasterCategoryConstants.SHIPMENT_TYPE)
        );
        dropdowns.put(RateSheetConstants.DROPDOWN_KEY_SHIPMENT_TYPE,shipmentType);
        dropdowns.put(RateSheetConstants.DROPDOWN_KEY_BUSINESS_TYPE, businessTypeDropdown());
        dropdowns.put(RateSheetConstants.DROPDOWN_KEY_CATEGORY_TYPE, categoryTypeDropdown());
        dropdowns.put(
                RateSheetConstants.DROPDOWN_KEY_SERVICE_CODE,
                serviceChargeService.getActiveServiceCodesForDropdown(
                        resolvedAccountId, accountNature, entityCode, chargeType));
        return dropdowns;
    }

    private static boolean hasAccountId(String accountId) {
        if (!StringUtils.hasText(accountId)) {
            return false;
        }
        return !ServiceChargeMessageConstants.NULL_ACCOUNT_ID_PLACEHOLDER.equalsIgnoreCase(accountId.trim());
    }

    @Cacheable(cacheNames = DashboardCacheNames.RATE_SHEET_ITEM_BY_ID, key = "#rateSheetItemId")
    public RateSheetItemUpdateDTO getRateSheetItemID(UUID rateSheetItemId) {
        RateSheetItem item = rateSheetItemRepository.findWithRatesById(rateSheetItemId)
                .orElseThrow(() -> new ResourceNotFoundException(RateSheetMessageConstants.RATE_SHEET_ITEM_NOT_FOUND));

        Account account = resolveAccountForRateSheetItem(item);
        return new RateSheetItemUpdateDTO(
                item.getId(),
                item.getDescription(),
                item.getServiceCode(),
                item.getShipmentType(),
                item.getBusinessType(),
                item.getCategoryType(),
                toRateResponses(item.getRates()),
                buildResponse(
                        account.getId().toString(),
                        account.getEntityCode(),
                        account.getChargeType()));
    }

    private Account resolveAccountForRateSheetItem(RateSheetItem item) {
        if (item.getRateSheet() == null || item.getRateSheet().getId() == null) {
            throw new BadRequestException(RateSheetMessageConstants.RATE_SHEET_NOT_FOUND_FOR_ITEM);
        }
        RateSheet rateSheet = rateSheetRepository.findById(item.getRateSheet().getId())
                .orElseThrow(() -> new ResourceNotFoundException(DashboardMessageConstants.RATE_SHEET_NOT_FOUND));
        if (rateSheet.getAccount() == null) {
            throw new BadRequestException(RateSheetMessageConstants.ACCOUNT_NOT_FOUND_FOR_RATE_SHEET);
        }
        return rateSheet.getAccount();
    }

    private void mapItemFields(
            RateSheetItem item,
            CreateRateSheetItemRequest req,
            RateSheetItemFieldValidation.ValidatedFields validated) {
        item.setServiceCode(validated.serviceCode());
        item.setDescription(req.getDescription() != null ? req.getDescription().strip() : null);
        item.setShipmentType(validated.shipmentType());
        item.setBusinessType(validated.businessType());
        item.setCategoryType(validated.categoryType());
    }

    private List<String> businessTypeDropdown() {
        List<String> values = new ArrayList<>();
        values.add(RateSheetConstants.VALUE_ALL);
        accountService.masterCodes(MasterCategoryConstants.BUSINESS_TYPE).stream()
                .filter(this::isRateSheetBusinessType)
                .forEach(values::add);
        return values;
    }

    private List<String> categoryTypeDropdown() {
        List<String> values = new ArrayList<>();
        values.add(RateSheetConstants.VALUE_ALL);
        accountService.masterCodes(MasterCategoryConstants.CATEGORY_TYPE).stream()
                .filter(this::isRateSheetCategoryType)
                .forEach(values::add);
        return values;
    }

    private boolean isRateSheetBusinessType(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = value.strip();
        return "B2B".equalsIgnoreCase(normalized) || "B2C".equalsIgnoreCase(normalized);
    }

    private boolean isRateSheetCategoryType(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = value.strip();
        return "LV".equalsIgnoreCase(normalized) || "HV".equalsIgnoreCase(normalized);
    }

    private Account requireAccount(RateSheet rateSheet) {
        if (rateSheet.getAccount() == null) {
            throw new BadRequestException(RateSheetMessageConstants.ACCOUNT_NOT_FOUND_FOR_RATE_SHEET);
        }
        return rateSheet.getAccount();
    }

    private record ResolvedUpdateRequest(
            CreateRateSheetItemRequest request,
            RateSheetItemFieldValidation.ValidatedFields validated) {
    }

    private void syncRates(RateSheetItem item, List<RateSheetItemRateRequest> rateRequests) {
        RateSheetRateValidation.validateRatesPresent(rateRequests);
        for (RateSheetItemRateRequest rateRequest : rateRequests) {
            RateSheetRateValidation.validateRate(
                    rateRequest.getRateType(),
                    rateRequest.getValue(),
                    rateRequest.getPercentageOf());
        }

        if (item.getRates() == null) {
            item.setRates(new ArrayList<>());
        }

        Map<UUID, RateSheetItemRate> existingById = item.getRates().stream()
                .filter(rate -> rate.getId() != null)
                .collect(Collectors.toMap(RateSheetItemRate::getId, Function.identity(), (left, right) -> left));

        List<RateSheetItemRate> updatedRates = new ArrayList<>(rateRequests.size());
        for (RateSheetItemRateRequest rateRequest : rateRequests) {
            RateSheetItemRate rate = rateRequest.getId() != null
                    ? existingById.get(rateRequest.getId())
                    : null;
            if (rate == null) {
                rate = new RateSheetItemRate();
                rate.setRateSheetItem(item);
            }
            applyRateFields(rate, rateRequest);
            updatedRates.add(rate);
        }

        item.getRates().clear();
        item.getRates().addAll(updatedRates);
    }

    private void applyRateFields(RateSheetItemRate rate, RateSheetItemRateRequest rateRequest) {
        String rateType = rateRequest.getRateType();
        RateSheetRateValidation.validateRate(rateType, rateRequest.getValue(), rateRequest.getPercentageOf());
        rate.setRateType(RateType.fromDisplayName(rateType));
        rate.setValue(rateRequest.getValue());
        if (RateType.PERCENTAGE.getDisplayName().equalsIgnoreCase(rateType)) {
            rate.setPercentageOf(PercentageOf.fromDisplayName(rateRequest.getPercentageOf()));
        } else {
            rate.setPercentageOf(null);
        }
    }

    private CreateRateSheetItemResponse toCreateItemResponse(RateSheetItem item) {
        return new CreateRateSheetItemResponse(
                item.getId(),
                item.getRateSheet() != null ? item.getRateSheet().getId() : null,
                item.getDescription(),
                item.getServiceCode(),
                item.getShipmentType(),
                item.getBusinessType(),
                item.getCategoryType(),
                toRateResponses(item.getRates()));
    }

    private RateSheetItemResponse toItemResponse(RateSheetItem item) {
        return new RateSheetItemResponse(
                item.getId(),
                item.getDescription(),
                item.getServiceCode(),
                item.getShipmentType(),
                item.getBusinessType(),
                item.getCategoryType(),
                toRateResponses(item.getRates()));
    }

    private List<RateSheetItemRateResponse> toRateResponses(List<RateSheetItemRate> rates) {
        if (rates == null || rates.isEmpty()) {
            return List.of();
        }
        return rates.stream().map(this::toRateResponse).toList();
    }

    private RateSheetItemRateResponse toRateResponse(RateSheetItemRate rate) {
        return new RateSheetItemRateResponse(
                rate.getId(),
                rate.getRateType() != null ? rate.getRateType().name() : null,
                rate.getPercentageOf() != null ? rate.getPercentageOf().name() : null,
                rate.getValue());
    }
}
