package com.customcharge.chargescalculator.service.batch;

import com.customcharge.persistencelayer.dto.ServiceChargeDescriptionMappingDto;
import com.customcharge.persistencelayer.dto.ServiceChargeResponse;
import com.customcharge.persistencelayer.entities.ARAccount;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.ChargeLineItem;
import com.customcharge.persistencelayer.entities.Entity;
import com.customcharge.persistencelayer.repository.ChargeLineItemRepository;
import com.customcharge.persistencelayer.repository.ChargeRepository;
import com.customcharge.persistencelayer.repository.EntityRepository;
import com.customcharge.persistencelayer.repository.ServiceChargeMappingRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Component
public class RecordProcessingLookups {

    private final ChargeRepository chargeRepository;
    private final ChargeLineItemRepository chargeLineItemRepository;
    private final EntityRepository entityRepository;
    private final ServiceChargeMappingRepository serviceChargeMappingRepository;

    public RecordProcessingLookups(
            ChargeRepository chargeRepository,
            ChargeLineItemRepository chargeLineItemRepository,
            EntityRepository entityRepository,
            ServiceChargeMappingRepository serviceChargeMappingRepository) {
        this.chargeRepository = chargeRepository;
        this.chargeLineItemRepository = chargeLineItemRepository;
        this.entityRepository = entityRepository;
        this.serviceChargeMappingRepository = serviceChargeMappingRepository;
    }

    public RecordProcessingLookupCache openForShipment(UUID shipmentId) {
        RecordProcessingLookupCache cache =
                RecordProcessingLookupCache.load(shipmentId, chargeRepository, chargeLineItemRepository);
        RecordProcessingLookupScope.open(cache);
        return cache;
    }

    public void closeScope() {
        RecordProcessingLookupScope.close();
    }

    public Optional<Charge> findCharge(UUID shipmentId, String chargeCode) {
        RecordProcessingLookupCache cache = RecordProcessingLookupScope.get();
        if (cache != null) {
            Optional<Charge> cached = cache.findCharge(chargeCode);
            if (cached.isPresent()) {
                return cached;
            }
        }
        Optional<Charge> loaded = chargeRepository.findByShipment_IdAndChargeCode(shipmentId, chargeCode);
        if (cache != null) {
            loaded.ifPresent(cache::registerCharge);
        }
        return loaded;
    }

    public void registerCharge(Charge charge) {
        RecordProcessingLookupCache cache = RecordProcessingLookupScope.get();
        if (cache != null) {
            cache.registerCharge(charge);
        }
    }

    public List<ChargeLineItem> findLineItems(UUID chargeId) {
        RecordProcessingLookupCache cache = RecordProcessingLookupScope.get();
        if (cache != null && cache.hasLineItemsEntry(chargeId)) {
            return cache.lineItems(chargeId);
        }
        List<ChargeLineItem> loaded = chargeLineItemRepository.findByCharge_Id(chargeId);
        if (cache != null) {
            cache.replaceLineItems(chargeId, loaded);
        }
        return loaded;
    }

    public void cacheLineItems(UUID chargeId, List<ChargeLineItem> lines) {
        RecordProcessingLookupCache cache = RecordProcessingLookupScope.get();
        if (cache != null) {
            cache.replaceLineItems(chargeId, lines);
        }
    }

    public Optional<Entity> findEntity(String entityCode) {
        RecordProcessingLookupCache cache = RecordProcessingLookupScope.get();
        if (cache != null) {
            Optional<Entity> cached = cache.findEntity(entityCode);
            if (cached.isPresent()) {
                return cached;
            }
        }
        Optional<Entity> loaded = entityRepository.findByCodeIgnoreCase(entityCode);
        if (cache != null) {
            loaded.ifPresent(cache::cacheEntity);
        }
        return loaded;
    }

    public Entity requireEntity(String entityCode) {
        RecordProcessingLookupCache cache = RecordProcessingLookupScope.get();
        if (cache != null) {
            Optional<Entity> cached = cache.findEntity(entityCode);
            if (cached.isPresent()) {
                return cached.get();
            }
        }
        Entity loaded = entityRepository.findByCodeIgnoreCase(entityCode)
                .orElseThrow(() -> new IllegalStateException("Entity not found: " + entityCode));
        if (cache != null) {
            cache.cacheEntity(loaded);
        }
        return loaded;
    }

    public Optional<Account> partyArAccount(
            String entityCode,
            String accountNumber,
            String chargeCode,
            Supplier<Optional<Account>> loader) {
        RecordProcessingLookupCache cache = RecordProcessingLookupScope.get();
        if (cache == null) {
            return loader.get();
        }
        String key = RecordProcessingLookupCache.partyArAccountKey(entityCode, accountNumber, chargeCode);
        if (cache.hasPartyArAccountEntry(key)) {
            return cache.findPartyArAccount(key);
        }
        Optional<Account> loaded = loader.get();
        cache.cachePartyArAccount(key, loaded);
        return loaded;
    }

    public List<Account> dtaAccounts(String entityCode, Supplier<List<Account>> loader) {
        RecordProcessingLookupCache cache = RecordProcessingLookupScope.get();
        if (cache == null) {
            return loader.get();
        }
        String key = RecordProcessingLookupCache.dtaAccountsKey(entityCode);
        if (cache.hasAccountsEntry(key)) {
            return cache.findAccounts(key);
        }
        List<Account> loaded = loader.get();
        cache.cacheAccounts(key, loaded);
        return loaded;
    }

    public Optional<ARAccount> arMaster(
            String entityCode,
            String arAccountNumber,
            Supplier<Optional<ARAccount>> loader) {
        RecordProcessingLookupCache cache = RecordProcessingLookupScope.get();
        if (cache == null) {
            return loader.get();
        }
        String key = RecordProcessingLookupCache.arMasterKey(entityCode, arAccountNumber);
        if (cache.hasArMasterEntry(key)) {
            return cache.findArMaster(key);
        }
        Optional<ARAccount> loaded = loader.get();
        cache.cacheArMaster(key, loaded);
        return loaded;
    }

    public Optional<Account> dtcBillingAccount(
            String entityCode,
            String partyAccountNumber,
            Supplier<Optional<Account>> loader) {
        RecordProcessingLookupCache cache = RecordProcessingLookupScope.get();
        if (cache == null) {
            return loader.get();
        }
        String key = RecordProcessingLookupCache.dtcBillingAccountKey(entityCode, partyAccountNumber);
        if (cache.hasDtcBillingAccountEntry(key)) {
            return cache.findDtcBillingAccount(key);
        }
        Optional<Account> loaded = loader.get();
        cache.cacheDtcBillingAccount(key, loaded);
        return loaded;
    }

    public List<Account> entitySettingsAccounts(
            String entityCode,
            String chargeCode,
            Supplier<List<Account>> loader) {
        RecordProcessingLookupCache cache = RecordProcessingLookupScope.get();
        if (cache == null) {
            return loader.get();
        }
        String key = RecordProcessingLookupCache.entitySettingsAccountsKey(entityCode, chargeCode);
        if (cache.hasAccountsEntry(key)) {
            return cache.findAccounts(key);
        }
        List<Account> loaded = loader.get();
        cache.cacheAccounts(key, loaded);
        return loaded;
    }

    public List<ServiceChargeDescriptionMappingDto> descriptionMappings(
            String fromEntity,
            String toEntity,
            String chargeType,
            Supplier<List<ServiceChargeDescriptionMappingDto>> loader) {
        RecordProcessingLookupCache cache = RecordProcessingLookupScope.get();
        if (cache == null) {
            return loader.get();
        }
        String key = RecordProcessingLookupCache.descriptionMappingsKey(fromEntity, toEntity, chargeType);
        if (!cache.hasDescriptionMappingsEntry(key)) {
            cache.cacheDescriptionMappings(key, loader.get());
        }
        return cache.findDescriptionMappings(key);
    }

    public List<ServiceChargeResponse> serviceCharges(
            String entityCode,
            Account account,
            String chargeCode,
            Supplier<List<ServiceChargeResponse>> loader) {
        RecordProcessingLookupCache cache = RecordProcessingLookupScope.get();
        if (cache == null) {
            return loader.get();
        }
        UUID accountId = account != null ? account.getId() : null;
        String key = RecordProcessingLookupCache.serviceChargesKey(entityCode, accountId, chargeCode);
        if (cache.hasServiceChargesEntry(key)) {
            return cache.findServiceCharges(key);
        }
        List<ServiceChargeResponse> loaded = loader.get();
        cache.cacheServiceCharges(key, loaded);
        return loaded;
    }

    public boolean hasActualizedCharge(UUID shipmentId) {
        RecordProcessingLookupCache cache = RecordProcessingLookupScope.get();
        if (cache != null) {
            return cache.hasActualizedCharge();
        }
        return chargeRepository.existsByShipment_IdAndStatus(
                shipmentId, com.customcharge.chargescalculator.constants.BatchProcessingConstants.STATUS_ACTUALIZED);
    }

    public boolean hasAnyCharges(UUID shipmentId) {
        RecordProcessingLookupCache cache = RecordProcessingLookupScope.get();
        if (cache != null) {
            return cache.hasAnyCharges();
        }
        return !chargeRepository.findByShipment_IdWithEntity(shipmentId).isEmpty();
    }
}
