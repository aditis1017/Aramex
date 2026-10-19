package com.customcharge.chargescalculator.service.batch;

import com.customcharge.chargescalculator.constants.BatchProcessingConstants;
import com.customcharge.persistencelayer.dto.ServiceChargeDescriptionMappingDto;
import com.customcharge.persistencelayer.dto.ServiceChargeResponse;
import com.customcharge.persistencelayer.entities.ARAccount;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.ChargeLineItem;
import com.customcharge.persistencelayer.entities.Entity;
import com.customcharge.persistencelayer.repository.ChargeLineItemRepository;
import com.customcharge.persistencelayer.repository.ChargeRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Per-row in-memory snapshot of shipment charges, line items, and hot lookup keys.
 */
public final class RecordProcessingLookupCache {

    private final UUID shipmentId;
    private final Map<String, Charge> chargesByCode = new HashMap<>();
    private final Map<UUID, List<ChargeLineItem>> lineItemsByChargeId = new HashMap<>();
    private final Map<String, Entity> entitiesByCode = new HashMap<>();
    private final Map<String, List<Account>> accountsByKey = new HashMap<>();
    private final Map<String, List<ServiceChargeDescriptionMappingDto>> mappingsByKey = new HashMap<>();
    private final Map<String, List<ServiceChargeResponse>> serviceChargesByKey = new HashMap<>();
    private final Map<String, Optional<Account>> partyArAccountsByKey = new HashMap<>();
    private final Map<String, Optional<Account>> dtcBillingAccountsByKey = new HashMap<>();
    private final Map<String, Optional<ARAccount>> arMastersByKey = new HashMap<>();

    private RecordProcessingLookupCache(UUID shipmentId) {
        this.shipmentId = shipmentId;
    }

    public static RecordProcessingLookupCache load(
            UUID shipmentId,
            ChargeRepository chargeRepository,
            ChargeLineItemRepository chargeLineItemRepository) {
        RecordProcessingLookupCache cache = new RecordProcessingLookupCache(shipmentId);
        List<Charge> charges = chargeRepository.findByShipment_IdWithEntity(shipmentId);
        List<UUID> chargeIds = new ArrayList<>(charges.size());
        for (Charge charge : charges) {
            if (charge.getChargeCode() != null) {
                cache.chargesByCode.put(normalizeChargeCode(charge.getChargeCode()), charge);
            }
            chargeIds.add(charge.getId());
        }
        if (!chargeIds.isEmpty()) {
            for (ChargeLineItem line : chargeLineItemRepository.findByCharge_IdIn(chargeIds)) {
                if (line.getCharge() == null || line.getCharge().getId() == null) {
                    continue;
                }
                cache.lineItemsByChargeId
                        .computeIfAbsent(line.getCharge().getId(), ignored -> new ArrayList<>())
                        .add(line);
            }
        }
        return cache;
    }

    public UUID shipmentId() {
        return shipmentId;
    }

    public Optional<Charge> findCharge(String chargeCode) {
        if (chargeCode == null || chargeCode.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(chargesByCode.get(normalizeChargeCode(chargeCode)));
    }

    public void registerCharge(Charge charge) {
        if (charge == null || charge.getChargeCode() == null) {
            return;
        }
        chargesByCode.put(normalizeChargeCode(charge.getChargeCode()), charge);
        lineItemsByChargeId.putIfAbsent(charge.getId(), new ArrayList<>());
    }

    public List<ChargeLineItem> lineItems(UUID chargeId) {
        return List.copyOf(lineItemsByChargeId.getOrDefault(chargeId, List.of()));
    }

    public boolean hasLineItemsEntry(UUID chargeId) {
        return lineItemsByChargeId.containsKey(chargeId);
    }

    public boolean hasAnyCharges() {
        return !chargesByCode.isEmpty();
    }

    public void replaceLineItems(UUID chargeId, List<ChargeLineItem> lines) {
        lineItemsByChargeId.put(chargeId, new ArrayList<>(lines));
    }

    public void cacheEntity(Entity entity) {
        if (entity == null || entity.getCode() == null) {
            return;
        }
        entitiesByCode.put(normalizeEntityCode(entity.getCode()), entity);
    }

    public Optional<Entity> findEntity(String entityCode) {
        if (entityCode == null || entityCode.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(entitiesByCode.get(normalizeEntityCode(entityCode)));
    }

    public boolean hasAccountsEntry(String cacheKey) {
        return accountsByKey.containsKey(cacheKey);
    }

    public boolean hasDescriptionMappingsEntry(String cacheKey) {
        return mappingsByKey.containsKey(cacheKey);
    }

    public List<Account> findAccounts(String cacheKey) {
        return accountsByKey.getOrDefault(cacheKey, List.of());
    }

    public void cacheAccounts(String cacheKey, List<Account> accounts) {
        accountsByKey.put(cacheKey, List.copyOf(accounts));
    }

    public List<ServiceChargeDescriptionMappingDto> findDescriptionMappings(String cacheKey) {
        return mappingsByKey.getOrDefault(cacheKey, List.of());
    }

    public void cacheDescriptionMappings(String cacheKey, List<ServiceChargeDescriptionMappingDto> mappings) {
        mappingsByKey.put(cacheKey, List.copyOf(mappings));
    }

    public boolean hasServiceChargesEntry(String cacheKey) {
        return serviceChargesByKey.containsKey(cacheKey);
    }

    public List<ServiceChargeResponse> findServiceCharges(String cacheKey) {
        return serviceChargesByKey.getOrDefault(cacheKey, List.of());
    }

    public void cacheServiceCharges(String cacheKey, List<ServiceChargeResponse> serviceCharges) {
        serviceChargesByKey.put(cacheKey, List.copyOf(serviceCharges));
    }

    public boolean hasPartyArAccountEntry(String cacheKey) {
        return partyArAccountsByKey.containsKey(cacheKey);
    }

    public Optional<Account> findPartyArAccount(String cacheKey) {
        return partyArAccountsByKey.getOrDefault(cacheKey, Optional.empty());
    }

    public void cachePartyArAccount(String cacheKey, Optional<Account> account) {
        partyArAccountsByKey.put(cacheKey, account == null ? Optional.empty() : account);
    }

    public boolean hasDtcBillingAccountEntry(String cacheKey) {
        return dtcBillingAccountsByKey.containsKey(cacheKey);
    }

    public Optional<Account> findDtcBillingAccount(String cacheKey) {
        return dtcBillingAccountsByKey.getOrDefault(cacheKey, Optional.empty());
    }

    public void cacheDtcBillingAccount(String cacheKey, Optional<Account> account) {
        dtcBillingAccountsByKey.put(cacheKey, account == null ? Optional.empty() : account);
    }

    public boolean hasArMasterEntry(String cacheKey) {
        return arMastersByKey.containsKey(cacheKey);
    }

    public Optional<ARAccount> findArMaster(String cacheKey) {
        return arMastersByKey.getOrDefault(cacheKey, Optional.empty());
    }

    public void cacheArMaster(String cacheKey, Optional<ARAccount> arAccount) {
        arMastersByKey.put(cacheKey, arAccount == null ? Optional.empty() : arAccount);
    }

    public boolean hasActualizedCharge() {
        return chargesByCode.values().stream()
                .anyMatch(charge -> BatchProcessingConstants.STATUS_ACTUALIZED.equalsIgnoreCase(
                        charge.getStatus() == null ? "" : charge.getStatus().strip()));
    }

    static String entitySettingsAccountsKey(String entityCode, String chargeCode) {
        return "entitySettings|" + normalizeEntityCode(entityCode) + '|' + normalizeChargeCode(chargeCode);
    }

    static String descriptionMappingsKey(String fromEntity, String toEntity, String chargeType) {
        return "mapping|"
                + normalizeEntityCode(fromEntity) + '|'
                + normalizeEntityCode(toEntity) + '|'
                + normalizeChargeCode(chargeType);
    }

    static String serviceChargesKey(String entityCode, UUID accountId, String chargeCode) {
        return "serviceCharges|"
                + normalizeEntityCode(entityCode) + '|'
                + (accountId == null ? "none" : accountId) + '|'
                + normalizeChargeCode(chargeCode);
    }

    static String partyArAccountKey(String entityCode, String accountNumber, String chargeCode) {
        return "partyAr|"
                + normalizeEntityCode(entityCode) + '|'
                + accountNumber.strip() + '|'
                + normalizeChargeCode(chargeCode);
    }

    static String dtaAccountsKey(String entityCode) {
        return "dtaAccounts|" + normalizeEntityCode(entityCode);
    }

    static String dtcBillingAccountKey(String entityCode, String partyAccountNumber) {
        String party = partyAccountNumber == null || partyAccountNumber.isBlank()
                ? "_none_"
                : partyAccountNumber.strip();
        return "dtcBilling|" + normalizeEntityCode(entityCode) + '|' + party;
    }

    static String arMasterKey(String entityCode, String arAccountNumber) {
        String accountNumber = arAccountNumber == null || arAccountNumber.isBlank()
                ? "_none_"
                : arAccountNumber.strip();
        return "arMaster|" + normalizeEntityCode(entityCode) + '|' + accountNumber;
    }

    private static String normalizeChargeCode(String chargeCode) {
        return chargeCode.strip().toUpperCase(Locale.ROOT);
    }

    private static String normalizeEntityCode(String entityCode) {
        return entityCode.strip().toUpperCase(Locale.ROOT);
    }
}
