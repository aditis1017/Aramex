package com.customcharge.shipment.service.charge.processing;

import com.customcharge.persistencelayer.accountRedis.AccountRedisChargeDataService;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.ChargeLineItem;
import com.customcharge.persistencelayer.entities.Entity;
import com.customcharge.persistencelayer.entities.ServiceCharge;
import com.customcharge.persistencelayer.entities.ServiceCodes;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.repository.EntityRepository;
import com.customcharge.persistencelayer.repository.ServiceChargeRepository;
import com.customcharge.persistencelayer.repository.ServiceCodesRepository;
import com.customcharge.shipment.constants.ShipmentChargeTypeSupport;
import com.customcharge.shipment.service.charge.ChargeServiceCodeSupport;
import com.customcharge.shipment.service.charge.ShipmentCurrencyContext;
import com.customcharge.shipment.service.charge.processing.model.ResolvedRateSheet;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Request-scoped caches for a single shipment charge creation run.
 * Reduces repeated Hibernate queries without changing business rules.
 */
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class ShipmentProcessingContext {

    private static final ThreadLocal<ShipmentProcessingContext> HOLDER = new ThreadLocal<>();

    @Getter
    Shipment shipment;
    @Getter
    ShipmentCurrencyContext currencyContext;

    EntityRepository entityRepository;
    ServiceCodesRepository serviceCodesRepository;
    ServiceChargeRepository serviceChargeRepository;
    AccountRedisChargeDataService accountRedisChargeDataService;

    Map<String, Entity> entitiesByCode = new HashMap<>();
    Map<String, ServiceCodes> serviceCodesByKey = new HashMap<>();
    Map<String, ServiceCharge> serviceChargesByKey = new HashMap<>();
    Map<String, Optional<Account>> accountsByKey = new HashMap<>();
    Map<UUID, Optional<ResolvedRateSheet>> rateSheetsByAccountId = new HashMap<>();
    Map<String, Optional<Account>> dtaArAccountsByEntity = new HashMap<>();
    Map<String, Charge> chargesByCode = new HashMap<>();
    Map<String, List<ChargeLineItem>> lineItemsByChargeCode = new HashMap<>();
    Set<String> indexedServiceChargeKeys = new HashSet<>();
    @NonFinal
    boolean accountRedisUnavailable;

    ShipmentProcessingContext(
            Shipment shipment,
            ShipmentCurrencyContext currencyContext,
            EntityRepository entityRepository,
            ServiceCodesRepository serviceCodesRepository,
            ServiceChargeRepository serviceChargeRepository,
            AccountRedisChargeDataService accountRedisChargeDataService) {
        this.shipment = shipment;
        this.currencyContext = currencyContext;
        this.entityRepository = entityRepository;
        this.serviceCodesRepository = serviceCodesRepository;
        this.serviceChargeRepository = serviceChargeRepository;
        this.accountRedisChargeDataService = accountRedisChargeDataService;
    }

    public static void bind(ShipmentProcessingContext context) {
        HOLDER.set(context);
    }

    public static ShipmentProcessingContext get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    public void markAccountRedisUnavailable() {
        accountRedisUnavailable = true;
    }

    public boolean isAccountRedisUnavailable() {
        return accountRedisUnavailable;
    }

    /**
     * Caches account lookups for the active charge-creation run.
     * <p>Uses get/load/putIfAbsent instead of {@code HashMap#computeIfAbsent} so nested loaders
     * (e.g. {@code PARTY_AR} → {@code ENTITY_SETTINGS}/{@code AR}) can safely cache other keys
     * without throwing {@link java.util.ConcurrentModificationException} on modern JDKs.
     */
    public static Optional<Account> cachedAccount(String key, Supplier<Optional<Account>> loader) {
        ShipmentProcessingContext context = get();
        if (context == null) {
            return loader.get();
        }
        return loadAndCache(context.accountsByKey, key, loader);
    }

    public static Optional<ResolvedRateSheet> cachedRateSheet(
            UUID accountId, Supplier<Optional<ResolvedRateSheet>> loader) {
        ShipmentProcessingContext context = get();
        if (context == null || accountId == null) {
            return loader.get();
        }
        return loadAndCache(context.rateSheetsByAccountId, accountId, loader);
    }

    public static Optional<Account> cachedDtaArAccount(String entityCode, Supplier<Optional<Account>> loader) {
        ShipmentProcessingContext context = get();
        if (context == null) {
            return loader.get();
        }
        return loadAndCache(context.dtaArAccountsByEntity, normalize(entityCode), loader);
    }

    private static <K, V> Optional<V> loadAndCache(
            Map<K, Optional<V>> cache, K key, Supplier<Optional<V>> loader) {
        Optional<V> cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        Optional<V> loaded = loader.get();
        Optional<V> raced = cache.putIfAbsent(key, loaded);
        return raced != null ? raced : loaded;
    }

    public void preloadForChargeCodes(List<String> chargeCodes) {
        if (chargeCodes == null || chargeCodes.isEmpty()) {
            return;
        }
        Set<String> entityCodes = new HashSet<>();
        List<String> normalizedChargeCodes = chargeCodes.stream()
                .filter(StringUtils::hasText)
                .map(ShipmentChargeTypeSupport::normalize)
                .distinct()
                .toList();
        for (String chargeCode : normalizedChargeCodes) {
            addEntityCode(entityCodes, ShipmentChargeTypeSupport.resolveBillingEntityCode(shipment, chargeCode));
        }
        addEntityCode(entityCodes, shipment.getOriginEntity());
        addEntityCode(entityCodes, shipment.getThirdPartyEntity());
        addEntityCode(entityCodes, shipment.getHAWBDestinationEntity());

        for (String entityCode : entityCodes) {
            preloadEntity(entityCode);
            preloadServiceCodes(entityCode, normalizedChargeCodes);
        }
    }

    public Optional<Entity> findEntity(String entityCode) {
        if (!StringUtils.hasText(entityCode)) {
            return Optional.empty();
        }
        String key = normalize(entityCode);
        Entity cached = entitiesByCode.get(key);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<Entity> loaded = entityRepository.findByCodeIgnoreCase(key);
        loaded.ifPresent(entity -> entitiesByCode.put(key, entity));
        return loaded;
    }

    public Optional<ServiceCodes> findServiceCode(String entityCode, String chargeCode, String serviceCode) {
        if (!StringUtils.hasText(entityCode) || !StringUtils.hasText(serviceCode)) {
            return Optional.empty();
        }
        String key = serviceCodeKey(entityCode, chargeCode, serviceCode);
        ServiceCodes cached = serviceCodesByKey.get(key);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<ServiceCodes> loaded = Optional.empty();
        if (accountRedisChargeDataService != null) {
            loaded = accountRedisChargeDataService.findServiceCodeMaster(
                    normalize(entityCode), ShipmentChargeTypeSupport.normalize(chargeCode), serviceCode);
        }
        if (loaded.isEmpty() && !isEntityChargeDataSynced(entityCode, chargeCode)) {
            loaded = serviceCodesRepository.findByEntity_CodeAndServiceCodeAndChargeTypeAndIsDeletedFalse(
                    normalize(entityCode),
                    ChargeServiceCodeSupport.normalize(serviceCode),
                    ShipmentChargeTypeSupport.normalize(chargeCode));
        }
        loaded.ifPresent(master -> serviceCodesByKey.put(key, master));
        return loaded;
    }

    public Optional<ServiceCharge> findServiceCharge(Account account, ServiceCodes master) {
        if (account == null || master == null || master.getId() == null || account.getId() == null) {
            return Optional.empty();
        }
        String key = serviceChargeKey(account.getId(), master.getId());
        ServiceCharge cached = serviceChargesByKey.get(key);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<ServiceCharge> loaded = Optional.empty();
        if (accountRedisChargeDataService != null) {
            loaded = accountRedisChargeDataService.findServiceCharge(
                    account, account.getEntityCode(), account.getChargeType(), master);
        }
        if (loaded.isEmpty() && master.getId() != null
                && !isAccountChargeDataSynced(account, account.getChargeType())) {
            loaded = serviceChargeRepository.findByAccountAndServiceCodes_IdWithActiveServiceCodes(
                    account, master.getId());
        }
        loaded.ifPresent(charge -> serviceChargesByKey.put(key, charge));
        return loaded;
    }

    public void ensureServiceChargesIndexed(Account account, String entityCode, String chargeCode) {
        if (account == null || account.getId() == null || !StringUtils.hasText(entityCode) || !StringUtils.hasText(chargeCode)) {
            return;
        }
        String indexKey = account.getId() + ":" + ShipmentChargeTypeSupport.normalize(chargeCode);
        if (!indexedServiceChargeKeys.add(indexKey)) {
            return;
        }
        List<ServiceCodes> masters = serviceCodesByKey.values().stream()
                .filter(master -> master.getEntity() != null)
                .filter(master -> normalize(entityCode).equalsIgnoreCase(normalize(master.getEntity().getCode())))
                .filter(master -> ShipmentChargeTypeSupport.normalize(chargeCode).equalsIgnoreCase(
                        ShipmentChargeTypeSupport.normalize(master.getChargeType())))
                .toList();
        if (masters.isEmpty() && accountRedisChargeDataService != null) {
            accountRedisChargeDataService.preloadEntityServiceCodes(
                    serviceCodesByKey, normalize(entityCode), List.of(ShipmentChargeTypeSupport.normalize(chargeCode)));
            masters = serviceCodesByKey.values().stream()
                    .filter(master -> master.getEntity() != null)
                    .filter(master -> normalize(entityCode).equalsIgnoreCase(normalize(master.getEntity().getCode())))
                    .filter(master -> ShipmentChargeTypeSupport.normalize(chargeCode).equalsIgnoreCase(
                            ShipmentChargeTypeSupport.normalize(master.getChargeType())))
                    .toList();
        }
        if (masters.isEmpty() && !isEntityChargeDataSynced(entityCode, chargeCode)) {
            List<ServiceCodes> loaded = serviceCodesRepository.findActiveMastersByEntityCodeAndChargeTypes(
                    normalize(entityCode), List.of(ShipmentChargeTypeSupport.normalize(chargeCode)));
            for (ServiceCodes master : loaded) {
                serviceCodesByKey.put(
                        serviceCodeKey(entityCode, master.getChargeType(), master.getServiceCode()), master);
            }
            masters = loaded;
        }
        List<UUID> masterIds = masters.stream().map(ServiceCodes::getId).toList();
        if (masterIds.isEmpty()) {
            return;
        }
        Map<UUID, ServiceCharge> chargesByMasterId = Map.of();
        if (accountRedisChargeDataService != null) {
            chargesByMasterId = accountRedisChargeDataService.loadServiceChargesByMasterId(
                    account, normalize(entityCode), chargeCode);
        }
        if (!chargesByMasterId.isEmpty()) {
            chargesByMasterId.forEach((masterId, charge) -> serviceChargesByKey.put(
                    serviceChargeKey(account.getId(), masterId), charge));
            return;
        }
        if (!isAccountChargeDataSynced(account, chargeCode)) {
            serviceChargeRepository.findByAccountAndServiceCodesIn(account, masterIds)
                    .forEach(charge -> serviceChargesByKey.put(
                            serviceChargeKey(account.getId(), charge.getServiceCodes().getId()), charge));
        }
    }

    public void registerCharge(String chargeCode, Charge charge) {
        if (!StringUtils.hasText(chargeCode) || charge == null) {
            return;
        }
        chargesByCode.put(ShipmentChargeTypeSupport.normalize(chargeCode), charge);
    }

    public Optional<Charge> findChargeByCode(String chargeCode) {
        if (!StringUtils.hasText(chargeCode)) {
            return Optional.empty();
        }
        return Optional.ofNullable(chargesByCode.get(ShipmentChargeTypeSupport.normalize(chargeCode)));
    }

    public void registerLineItems(String chargeCode, List<ChargeLineItem> lineItems) {
        if (!StringUtils.hasText(chargeCode) || lineItems == null || lineItems.isEmpty()) {
            return;
        }
        lineItemsByChargeCode.put(ShipmentChargeTypeSupport.normalize(chargeCode), List.copyOf(lineItems));
    }

    public Optional<List<ChargeLineItem>> findLineItemsByChargeCode(String chargeCode) {
        if (!StringUtils.hasText(chargeCode)) {
            return Optional.empty();
        }
        List<ChargeLineItem> lineItems = lineItemsByChargeCode.get(ShipmentChargeTypeSupport.normalize(chargeCode));
        return lineItems == null ? Optional.empty() : Optional.of(lineItems);
    }

    private void preloadEntity(String entityCode) {
        if (!StringUtils.hasText(entityCode)) {
            return;
        }
        findEntity(entityCode);
    }

    private void preloadServiceCodes(String entityCode, List<String> chargeCodes) {
        if (!StringUtils.hasText(entityCode) || chargeCodes == null || chargeCodes.isEmpty()) {
            return;
        }
        if (accountRedisChargeDataService != null) {
            accountRedisChargeDataService.preloadEntityServiceCodes(serviceCodesByKey, normalize(entityCode), chargeCodes);
            if (!serviceCodesByKey.isEmpty() || isAnyEntityChargeDataSynced(entityCode, chargeCodes)) {
                return;
            }
        }
        List<ServiceCodes> masters = serviceCodesRepository.findActiveMastersByEntityCodeAndChargeTypes(
                normalize(entityCode), chargeCodes);
        for (ServiceCodes master : masters) {
            serviceCodesByKey.put(
                    serviceCodeKey(entityCode, master.getChargeType(), master.getServiceCode()), master);
        }
    }

    private static void addEntityCode(Set<String> entityCodes, String entityCode) {
        if (StringUtils.hasText(entityCode)) {
            entityCodes.add(normalize(entityCode));
        }
    }

    static String serviceCodeKey(String entityCode, String chargeCode, String serviceCode) {
        return normalize(entityCode) + '|'
                + ShipmentChargeTypeSupport.normalize(chargeCode) + '|'
                + ChargeServiceCodeSupport.normalize(serviceCode);
    }

    private static String serviceChargeKey(UUID accountId, UUID serviceCodeId) {
        return accountId + "|" + serviceCodeId;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip().toUpperCase(Locale.ROOT);
    }

    private boolean isEntityChargeDataSynced(String entityCode, String chargeCode) {
        if (accountRedisChargeDataService == null || !StringUtils.hasText(entityCode) || !StringUtils.hasText(chargeCode)) {
            return false;
        }
        return accountRedisChargeDataService.readEntitySettingsChargeTypeData(
                        normalize(entityCode), ShipmentChargeTypeSupport.normalize(chargeCode))
                .map(AccountRedisChargeDataService::isChargeDataSynced)
                .orElse(false);
    }

    private boolean isAnyEntityChargeDataSynced(String entityCode, List<String> chargeCodes) {
        if (chargeCodes == null || chargeCodes.isEmpty()) {
            return false;
        }
        return chargeCodes.stream().anyMatch(chargeCode -> isEntityChargeDataSynced(entityCode, chargeCode));
    }

    private boolean isAccountChargeDataSynced(Account account, String chargeCode) {
        if (accountRedisChargeDataService == null || account == null || !StringUtils.hasText(chargeCode)) {
            return false;
        }
        return accountRedisChargeDataService.readChargeTypeData(
                        account, ShipmentChargeTypeSupport.normalize(chargeCode))
                .map(AccountRedisChargeDataService::isChargeDataSynced)
                .orElse(false);
    }

    public static ShipmentProcessingContext create(
            Shipment shipment,
            ShipmentCurrencyContext currencyContext,
            EntityRepository entityRepository,
            ServiceCodesRepository serviceCodesRepository,
            ServiceChargeRepository serviceChargeRepository,
            AccountRedisChargeDataService accountRedisChargeDataService) {
        return new ShipmentProcessingContext(
                shipment,
                currencyContext,
                entityRepository,
                serviceCodesRepository,
                serviceChargeRepository,
                accountRedisChargeDataService);
    }
}
