package com.customcharge.persistencelayer.accountRedis;

import com.customcharge.persistencelayer.dto.ServiceChargeQueryCriteria;
import com.customcharge.persistencelayer.dto.ServiceChargeResponse;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.RateSheet;
import com.customcharge.persistencelayer.entities.RateSheetItem;
import com.customcharge.persistencelayer.entities.ServiceCharge;
import com.customcharge.persistencelayer.entities.ServiceCodes;
import com.customcharge.persistencelayer.repository.RateSheetItemRepository;
import com.customcharge.persistencelayer.repository.RateSheetRepository;
import com.customcharge.persistencelayer.repository.ServiceChargeRepository;
import com.customcharge.persistencelayer.repository.ServiceCodesRepository;
import com.customcharge.persistencelayer.util.ServiceChargeResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountRedisChargeDataService {

    private final ObjectProvider<AccountRedisStringCache> accountRedisStringCache;
    private final ObjectProvider<AccountRedisReadListener> accountRedisReadListener;
    private final ObjectMapper objectMapper;
    private final RateSheetRepository rateSheetRepository;
    private final RateSheetItemRepository rateSheetItemRepository;
    private final ServiceCodesRepository serviceCodesRepository;
    private final ServiceChargeRepository serviceChargeRepository;

    public Optional<ArAccountRedisChargeTypeData> readChargeTypeData(Account account, String chargeType) {
        if (account == null || !StringUtils.hasText(chargeType)) {
            return Optional.empty();
        }
        String redisKey = AccountRedisKeyResolver.redisKeyFor(account);
        if (!StringUtils.hasText(redisKey)) {
            return Optional.empty();
        }
        return readChargeTypeData(redisKey, chargeType);
    }

    public Optional<ArAccountRedisChargeTypeData> readEntitySettingsChargeTypeData(
            String entityCode, String chargeType) {
        String redisKey = AccountRedisKeyResolver.redisKeyForEntitySettings(entityCode);
        if (!StringUtils.hasText(redisKey)) {
            return Optional.empty();
        }
        return readChargeTypeData(redisKey, chargeType);
    }

    public Optional<ArAccountRedisChargeTypeData> readChargeTypeData(String redisKey, String chargeType) {
        return readPayload(redisKey).flatMap(payload -> resolveChargeEntry(payload.getCharges(), chargeType));
    }

    public List<ArAccountRedisServiceCharge> getServiceCharges(Account account, String entityCode, String chargeType) {
        Optional<ArAccountRedisChargeTypeData> fromRedis = readChargeTypeData(account, chargeType);
        if (fromRedis.isPresent() && isChargeDataSynced(fromRedis.get())) {
            List<ArAccountRedisServiceCharge> charges = fromRedis.get().getServiceCharges();
            return charges != null ? charges : List.of();
        }
        return loadServiceChargesFromDb(account, entityCode, chargeType).stream()
                .map(this::toRedisServiceCharge)
                .toList();
    }

    public List<ServiceCodes> getServiceCodeMasters(Account account, String entityCode, String chargeType) {
        return getServiceCharges(account, entityCode, chargeType).stream()
                .map(charge -> AccountRedisServiceChargeMapper.toServiceCodesMaster(
                        charge, entityCode, normalizeChargeType(chargeType)))
                .toList();
    }

    public List<ServiceCodes> getEntityServiceCodeMasters(String entityCode, String chargeType) {
        Optional<ArAccountRedisChargeTypeData> fromRedis =
                readEntitySettingsChargeTypeData(entityCode, chargeType);
        if (fromRedis.isPresent() && isChargeDataSynced(fromRedis.get())) {
            List<ArAccountRedisServiceCharge> charges = fromRedis.get().getServiceCharges();
            if (charges == null) {
                return List.of();
            }
            return charges.stream()
                    .map(charge -> AccountRedisServiceChargeMapper.toServiceCodesMaster(
                            charge, entityCode, normalizeChargeType(chargeType)))
                    .toList();
        }
        return serviceCodesRepository.findByEntityCodeAndChargeTypeAndIsDeletedFalse(
                entityCode.strip(), normalizeChargeType(chargeType));
    }

    public Optional<ServiceCodes> findServiceCodeMaster(
            Account account, String entityCode, String chargeType, String serviceCode) {
        if (!StringUtils.hasText(serviceCode)) {
            return Optional.empty();
        }
        String normalizedServiceCode = serviceCode.strip().toUpperCase(Locale.ROOT);
        Optional<ArAccountRedisServiceCharge> fromRedis = getServiceCharges(account, entityCode, chargeType).stream()
                .filter(charge -> charge.getServiceCode() != null
                        && normalizedServiceCode.equals(charge.getServiceCode().strip().toUpperCase(Locale.ROOT)))
                .findFirst();
        if (fromRedis.isPresent()) {
            return Optional.of(AccountRedisServiceChargeMapper.toServiceCodesMaster(
                    fromRedis.get(), entityCode, normalizeChargeType(chargeType)));
        }
        return serviceCodesRepository.findByEntity_CodeAndServiceCodeAndChargeTypeAndIsDeletedFalse(
                entityCode.strip(), serviceCode.strip(), normalizeChargeType(chargeType));
    }

    public Optional<ServiceCodes> findServiceCodeMaster(
            String entityCode, String chargeType, String serviceCode) {
        Optional<ArAccountRedisChargeTypeData> fromRedis =
                readEntitySettingsChargeTypeData(entityCode, chargeType);
        if (fromRedis.isPresent() && isChargeDataSynced(fromRedis.get())) {
            String normalizedServiceCode = serviceCode.strip().toUpperCase(Locale.ROOT);
            if (fromRedis.get().getServiceCharges() != null) {
                for (ArAccountRedisServiceCharge charge : fromRedis.get().getServiceCharges()) {
                    if (charge.getServiceCode() != null
                            && normalizedServiceCode.equals(
                                    charge.getServiceCode().strip().toUpperCase(Locale.ROOT))) {
                        return Optional.of(AccountRedisServiceChargeMapper.toServiceCodesMaster(
                                charge, entityCode, normalizeChargeType(chargeType)));
                    }
                }
            }
            return Optional.empty();
        }
        return serviceCodesRepository.findByEntity_CodeAndServiceCodeAndChargeTypeAndIsDeletedFalse(
                entityCode.strip(), serviceCode.strip(), normalizeChargeType(chargeType));
    }

    public Optional<ServiceCharge> findServiceCharge(
            Account account, String entityCode, String chargeType, ServiceCodes master) {
        if (account == null || master == null) {
            return Optional.empty();
        }
        if (master.getId() != null) {
            Optional<ServiceCharge> fromRedis = findServiceChargeByMasterId(account, entityCode, chargeType, master.getId());
            if (fromRedis.isPresent()) {
                return fromRedis;
            }
        }
        if (StringUtils.hasText(master.getServiceCode())) {
            Optional<ServiceCharge> fromRedis =
                    findServiceChargeByServiceCode(account, entityCode, chargeType, master.getServiceCode());
            if (fromRedis.isPresent()) {
                return fromRedis;
            }
        }
        if (master.getId() == null) {
            return Optional.empty();
        }
        return serviceChargeRepository.findByAccountAndServiceCodes_IdWithActiveServiceCodes(
                account, master.getId());
    }

    public Map<UUID, ServiceCharge> loadServiceChargesByMasterId(
            Account account, String entityCode, String chargeType) {
        List<ArAccountRedisServiceCharge> charges = getServiceCharges(account, entityCode, chargeType);
        Map<UUID, ServiceCharge> indexed = new LinkedHashMap<>();
        String normalizedChargeType = normalizeChargeType(chargeType);
        for (ArAccountRedisServiceCharge redisCharge : charges) {
            if (redisCharge.getServiceCodeMasterId() == null) {
                continue;
            }
            ServiceCodes master =
                    AccountRedisServiceChargeMapper.toServiceCodesMaster(redisCharge, entityCode, normalizedChargeType);
            ServiceCharge charge = AccountRedisServiceChargeMapper.toServiceCharge(redisCharge, account, master);
            indexed.put(redisCharge.getServiceCodeMasterId(), charge);
        }
        if (!indexed.isEmpty() || isRedisChargeDataAvailable(account, chargeType)) {
            return indexed;
        }
        return serviceChargeRepository.findByAccountWithActiveServiceCodes(
                        account, entityCode.strip(), normalizedChargeType)
                .stream()
                .filter(charge -> charge.getServiceCodes() != null)
                .collect(LinkedHashMap::new,
                        (map, charge) -> map.put(charge.getServiceCodes().getId(), charge),
                        Map::putAll);
    }

    public Optional<AccountRedisRateSheetSnapshot> getRateSheetSnapshot(
            Account account, String entityCode, String chargeType) {
        Optional<ArAccountRedisChargeTypeData> fromRedis = readChargeTypeData(account, chargeType);
        if (fromRedis.isPresent() && isChargeDataSynced(fromRedis.get())) {
            return buildSnapshotFromRedis(fromRedis.get(), account);
        }
        return loadRateSheetFromDb(account);
    }

    /**
     * Resolves service charges from Redis when every required charge type is synced.
     * Returns {@link Optional#empty()} when Redis cannot satisfy the query (caller should use DB).
     */
    public Optional<List<ServiceChargeResponse>> tryResolveFromRedis(ServiceChargeQueryCriteria criteria) {
        if (criteria == null || !StringUtils.hasText(criteria.getEntityCode())) {
            return Optional.of(List.of());
        }
        String entityCode = criteria.getEntityCode().strip();
        List<MastersForChargeType> mastersByType =
                loadSyncedMastersByChargeType(entityCode, criteria.getAccount(), criteria.getChargeTypes());
        if (mastersByType == null) {
            return Optional.empty();
        }
        if (mastersByType.isEmpty()) {
            return Optional.of(List.of());
        }

        Account account = criteria.getAccount();
        if (account == null) {
            List<ServiceChargeResponse> responses = new ArrayList<>();
            for (MastersForChargeType entry : mastersByType) {
                for (ServiceCodes master : entry.masters()) {
                    if (!isEligibleMaster(master)) {
                        continue;
                    }
                    responses.add(ServiceChargeResolver.toResponse(ServiceChargeResolver.defaultCharge(null, master)));
                }
            }
            return Optional.of(responses);
        }

        List<ServiceChargeResponse> result = new ArrayList<>();
        for (MastersForChargeType entry : mastersByType) {
            Map<UUID, ServiceCharge> byMasterId =
                    loadServiceChargesByMasterId(account, entityCode, entry.chargeType());
            for (ServiceCodes master : entry.masters()) {
                if (!isEligibleMaster(master)) {
                    continue;
                }
                ServiceCharge charge = byMasterId.get(master.getId());
                if (charge != null) {
                    if (!isChargeActive(charge)) {
                        continue;
                    }
                    if (Boolean.FALSE.equals(criteria.getActive())) {
                        continue;
                    }
                    result.add(ServiceChargeResolver.toResponse(charge));
                } else if (!Boolean.FALSE.equals(criteria.getActive())) {
                    result.add(ServiceChargeResolver.toResponse(ServiceChargeResolver.defaultCharge(account, master)));
                }
            }
        }
        return Optional.of(result);
    }

    public void preloadEntityServiceCodes(
            Map<String, ServiceCodes> target,
            String entityCode,
            List<String> chargeTypes) {
        if (!StringUtils.hasText(entityCode) || chargeTypes == null || chargeTypes.isEmpty()) {
            return;
        }
        String redisKey = AccountRedisKeyResolver.redisKeyForEntitySettings(entityCode);
        Optional<ArAccountRedisPayload> payload = readPayload(redisKey);
        if (payload.isEmpty()) {
            preloadFromDb(target, entityCode, chargeTypes);
            return;
        }
        boolean loadedFromRedis = false;
        for (String chargeType : chargeTypes) {
            Optional<ArAccountRedisChargeTypeData> chargeData =
                    resolveChargeEntry(payload.get().getCharges(), chargeType);
            if (chargeData.isEmpty() || !isChargeDataSynced(chargeData.get())) {
                continue;
            }
            loadedFromRedis = true;
            List<ArAccountRedisServiceCharge> charges = chargeData.get().getServiceCharges();
            if (charges == null) {
                continue;
            }
            for (ArAccountRedisServiceCharge redisCharge : charges) {
                ServiceCodes master = AccountRedisServiceChargeMapper.toServiceCodesMaster(
                        redisCharge, entityCode, normalizeChargeType(chargeType));
                target.put(serviceCodeKey(entityCode, chargeType, master.getServiceCode()), master);
            }
        }
        if (!loadedFromRedis) {
            preloadFromDb(target, entityCode, chargeTypes);
        }
    }

    public static boolean isChargeDataSynced(ArAccountRedisChargeTypeData data) {
        return data != null && Boolean.TRUE.equals(data.getChargeDataSynced());
    }

    private boolean isRedisChargeDataAvailable(Account account, String chargeType) {
        return readChargeTypeData(account, chargeType)
                .map(AccountRedisChargeDataService::isChargeDataSynced)
                .orElse(false);
    }

    private Optional<ArAccountRedisPayload> readPayload(String redisKey) {
        AccountRedisReadListener listener = accountRedisReadListener.getIfAvailable();
        if (listener != null && listener.skipReads()) {
            return Optional.empty();
        }
        AccountRedisStringCache cache = accountRedisStringCache.getIfAvailable();
        if (cache == null || !StringUtils.hasText(redisKey)) {
            return Optional.empty();
        }
        try {
            Optional<String> json = cache.get(redisKey);
            if (json.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json.get(), ArAccountRedisPayload.class));
        } catch (Exception ex) {
            if (listener != null) {
                listener.onReadFailure(redisKey, ex);
            }
            log.warn(
                    "Failed to read account Redis payload for key {} ({}): {}",
                    redisKey,
                    ex.getClass().getSimpleName(),
                    ex.getMessage());
            return Optional.empty();
        }
    }

    private static Optional<ArAccountRedisChargeTypeData> resolveChargeEntry(
            Map<String, ArAccountRedisChargeTypeData> charges, String chargeType) {
        if (charges == null || !StringUtils.hasText(chargeType)) {
            return Optional.empty();
        }
        String chargeKey = AccountRedisChargeDataPopulator.chargeTypeKey(chargeType);
        ArAccountRedisChargeTypeData exact = charges.get(chargeKey);
        if (exact != null) {
            return Optional.of(exact);
        }
        for (Map.Entry<String, ArAccountRedisChargeTypeData> entry : charges.entrySet()) {
            if (chargeKey.equalsIgnoreCase(entry.getKey())) {
                return Optional.of(entry.getValue());
            }
        }
        return Optional.empty();
    }

    private List<ServiceChargeResponse> loadServiceChargesFromDb(
            Account account, String entityCode, String chargeType) {
        if (account == null) {
            return List.of();
        }
        List<ServiceCodes> masters = serviceCodesRepository.findByEntityCodeAndChargeTypeAndIsDeletedFalse(
                entityCode.strip(), normalizeChargeType(chargeType));
        if (masters.isEmpty()) {
            return List.of();
        }
        List<UUID> masterIds = masters.stream().map(ServiceCodes::getId).toList();
        Map<UUID, ServiceCharge> byServiceCodeId = serviceChargeRepository.findByAccountAndServiceCodesIn(
                        account, masterIds).stream()
                .filter(charge -> charge.getServiceCodes() != null)
                .collect(Collectors.toMap(
                        charge -> charge.getServiceCodes().getId(),
                        Function.identity(),
                        (left, right) -> left));

        List<ServiceChargeResponse> result = new ArrayList<>(masters.size());
        for (ServiceCodes master : masters) {
            if (!isEligibleMaster(master)) {
                continue;
            }
            ServiceCharge charge = byServiceCodeId.get(master.getId());
            if (charge != null) {
                if (!isChargeActive(charge)) {
                    continue;
                }
                result.add(ServiceChargeResolver.toResponse(charge));
            } else {
                result.add(ServiceChargeResolver.toResponse(ServiceChargeResolver.defaultCharge(account, master)));
            }
        }
        return result;
    }

    private record MastersForChargeType(String chargeType, List<ServiceCodes> masters) {}

    private List<MastersForChargeType> loadSyncedMastersByChargeType(
            String entityCode, Account account, List<String> chargeTypes) {
        List<String> typesToLoad = resolveChargeTypesToLoad(entityCode, account, chargeTypes);
        if (typesToLoad == null) {
            return null;
        }
        if (typesToLoad.isEmpty()) {
            return List.of();
        }

        List<MastersForChargeType> result = new ArrayList<>();
        for (String chargeType : typesToLoad) {
            Optional<ArAccountRedisChargeTypeData> chargeData = account != null
                    ? readChargeTypeData(account, chargeType)
                    : readEntitySettingsChargeTypeData(entityCode, chargeType);
            if (chargeData.isEmpty() || !isChargeDataSynced(chargeData.get())) {
                return null;
            }
            List<ServiceCodes> masters = mapRedisChargesToMasters(
                    chargeData.get().getServiceCharges(), entityCode, chargeType);
            result.add(new MastersForChargeType(chargeType, masters));
        }
        return result;
    }

    private List<String> resolveChargeTypesToLoad(
            String entityCode, Account account, List<String> chargeTypes) {
        if (chargeTypes != null && !chargeTypes.isEmpty()) {
            return chargeTypes.stream()
                    .filter(StringUtils::hasText)
                    .map(AccountRedisChargeDataService::normalizeChargeType)
                    .distinct()
                    .toList();
        }
        if (account != null && StringUtils.hasText(account.getChargeType())) {
            return List.of(normalizeChargeType(account.getChargeType()));
        }
        String redisKey = AccountRedisKeyResolver.redisKeyForEntitySettings(entityCode);
        Optional<ArAccountRedisPayload> payload = readPayload(redisKey);
        if (payload.isEmpty() || payload.get().getCharges() == null) {
            return null;
        }
        List<String> syncedTypes = new ArrayList<>();
        for (Map.Entry<String, ArAccountRedisChargeTypeData> entry : payload.get().getCharges().entrySet()) {
            if (!isChargeDataSynced(entry.getValue())) {
                return null;
            }
            syncedTypes.add(normalizeChargeType(entry.getKey()));
        }
        return syncedTypes;
    }

    private static List<ServiceCodes> mapRedisChargesToMasters(
            List<ArAccountRedisServiceCharge> charges, String entityCode, String chargeType) {
        if (charges == null || charges.isEmpty()) {
            return List.of();
        }
        String normalizedChargeType = normalizeChargeType(chargeType);
        return charges.stream()
                .map(charge -> AccountRedisServiceChargeMapper.toServiceCodesMaster(
                        charge, entityCode, normalizedChargeType))
                .toList();
    }

    private static boolean isChargeActive(ServiceCharge charge) {
        return charge.getActive() == null || charge.getActive();
    }

    private static boolean isEligibleMaster(ServiceCodes master) {
        return master != null && !Boolean.FALSE.equals(master.getIsActive());
    }

    private ArAccountRedisServiceCharge toRedisServiceCharge(ServiceChargeResponse response) {
        ArAccountRedisServiceCharge redis = new ArAccountRedisServiceCharge();
        redis.setId(response.getId());
        redis.setServiceCode(response.getServiceCode());
        redis.setDescription(response.getDescription());
        redis.setServiceDescription(response.getServiceDescription());
        redis.setType(response.getType());
        redis.setMasterActive(response.getIsActive());
        redis.setTaxValue(response.getTaxValue());
        redis.setTaxCode(response.getTaxCode());
        redis.setActive(response.getActive());
        redis.setDefaultAccountNumber(response.getDefaultAccountNumber());
        redis.setIsBillable(response.getIsBillable());
        redis.setSplit(response.getSplit());
        redis.setInfoAxsId(response.getInfoAxsId());
        redis.setFetchTax(response.getFetchTax());
        return redis;
    }

    private Optional<ServiceCharge> findServiceChargeByMasterId(
            Account account, String entityCode, String chargeType, UUID masterId) {
        return getServiceCharges(account, entityCode, chargeType).stream()
                .filter(charge -> masterId.equals(charge.getServiceCodeMasterId()))
                .findFirst()
                .map(charge -> AccountRedisServiceChargeMapper.toServiceCharge(
                        charge,
                        account,
                        AccountRedisServiceChargeMapper.toServiceCodesMaster(
                                charge, entityCode, normalizeChargeType(chargeType))));
    }

    private Optional<ServiceCharge> findServiceChargeByServiceCode(
            Account account, String entityCode, String chargeType, String serviceCode) {
        String normalized = serviceCode.strip().toUpperCase(Locale.ROOT);
        return getServiceCharges(account, entityCode, chargeType).stream()
                .filter(charge -> charge.getServiceCode() != null
                        && normalized.equals(charge.getServiceCode().strip().toUpperCase(Locale.ROOT)))
                .findFirst()
                .map(charge -> AccountRedisServiceChargeMapper.toServiceCharge(
                        charge,
                        account,
                        AccountRedisServiceChargeMapper.toServiceCodesMaster(
                                charge, entityCode, normalizeChargeType(chargeType))));
    }

    private Optional<AccountRedisRateSheetSnapshot> buildSnapshotFromRedis(
            ArAccountRedisChargeTypeData chargeData, Account account) {
        if (chargeData.getRateSheetId() == null) {
            return Optional.empty();
        }
        RateSheet rateSheet = new RateSheet();
        AccountRedisEntityIdSupport.setId(rateSheet, chargeData.getRateSheetId());
        rateSheet.setAccount(account);
        rateSheet.setActive(true);
        List<RateSheetItem> items = AccountRedisRateSheetMapper.toEntities(chargeData.getRateSheetItems(), rateSheet);
        return Optional.of(new AccountRedisRateSheetSnapshot(rateSheet, items));
    }

    private Optional<AccountRedisRateSheetSnapshot> loadRateSheetFromDb(Account account) {
        if (account == null || account.getId() == null) {
            return Optional.empty();
        }
        return rateSheetRepository.findByAccount_Id(account.getId())
                .filter(rateSheet -> rateSheet != null && !Boolean.FALSE.equals(rateSheet.getActive()))
                .map(rateSheet -> {
                    List<RateSheetItem> items =
                            rateSheetItemRepository.findByRateSheet_IdAndIsDeletedFalse(rateSheet.getId());
                    return new AccountRedisRateSheetSnapshot(rateSheet, items);
                });
    }

    private void preloadFromDb(Map<String, ServiceCodes> target, String entityCode, List<String> chargeTypes) {
        List<ServiceCodes> masters = serviceCodesRepository.findActiveMastersByEntityCodeAndChargeTypes(
                entityCode.strip(),
                chargeTypes.stream().map(AccountRedisChargeDataService::normalizeChargeType).toList());
        for (ServiceCodes master : masters) {
            target.put(serviceCodeKey(entityCode, master.getChargeType(), master.getServiceCode()), master);
        }
    }

    private static String serviceCodeKey(String entityCode, String chargeType, String serviceCode) {
        return entityCode.strip().toUpperCase(Locale.ROOT) + '|'
                + normalizeChargeType(chargeType) + '|'
                + (serviceCode == null ? "" : serviceCode.strip().toUpperCase(Locale.ROOT));
    }

    private static String normalizeChargeType(String chargeType) {
        return chargeType == null ? "" : chargeType.strip().toUpperCase(Locale.ROOT);
    }
}
