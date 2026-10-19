package com.customcharge.dashboard.service;

import com.customcharge.dashboard.cache.DashboardCacheNames;
import com.customcharge.dashboard.constants.ChargeTypeConstants;
import com.customcharge.dashboard.dto.charge.ShipmentChargeDetailsResponse;
import com.customcharge.dashboard.mapper.ShipmentChargeDetailsMapper;
import com.customcharge.dashboard.service.impl.ServiceCodesService;
import com.customcharge.dashboard.util.RelatedChargeTypeResolver;
import com.customcharge.persistencelayer.entities.APAccount;
import com.customcharge.persistencelayer.entities.ARAccount;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.ChargeLineItem;
import com.customcharge.persistencelayer.entities.ServiceCodes;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.exception.ResourceNotFoundException;
import com.customcharge.persistencelayer.repository.APAccountRepository;
import com.customcharge.persistencelayer.repository.ARAccountRepository;
import com.customcharge.persistencelayer.repository.AccountRepository;
import com.customcharge.persistencelayer.repository.ChargeLineItemRepository;
import com.customcharge.persistencelayer.repository.ChargeRepository;
import com.customcharge.persistencelayer.repository.ServiceCodesRepository;
import com.customcharge.persistencelayer.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final ShipmentRepository shipmentRepository;
    private final ChargeRepository chargeRepository;
    private final ChargeLineItemRepository chargeLineItemRepository;
    private final ServiceCodesRepository serviceCodesRepository;
    private final AccountRepository accountRepository;
    private final APAccountRepository apAccountRepository;
    private final ARAccountRepository arAccountRepository;
    private final ShipmentChargeDetailsMapper shipmentChargeDetailsMapper;

    public ShipmentChargeDetailsResponse getShipmentChargeDetails(String hawbNumber, String chargeType) {
        String normalizedChargeType = ServiceCodesService.requireChargeType(chargeType);

        Shipment shipment = shipmentRepository.findByHawbNumber(hawbNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Shipment not found for tracking number: " + hawbNumber));

        List<Charge> shipmentCharges = chargeRepository.findByShipment_IdWithEntity(shipment.getId());
        if (shipmentCharges.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No charges found for tracking number: " + hawbNumber);
        }

        Set<String> shipmentChargeCodes = shipmentCharges.stream()
                .map(Charge::getChargeCode)
                .map(RelatedChargeTypeResolver::normalize)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (!shipmentChargeCodes.contains(normalizedChargeType)) {
            throw new ResourceNotFoundException(
                    "Charge type " + normalizedChargeType + " not found for tracking number: " + hawbNumber);
        }

        List<Charge> includedCharges = shipmentCharges.stream()
                .sorted(Comparator.comparing(charge -> ChargeTypeConstants.chargeDetailsDisplayIndex(
                        RelatedChargeTypeResolver.normalize(charge.getChargeCode()))))
                .toList();

        List<UUID> chargeIds = includedCharges.stream().map(Charge::getId).toList();
        List<ChargeLineItem> lineItems = chargeIds.isEmpty()
                ? List.of()
                : chargeLineItemRepository.findByCharge_IdIn(chargeIds);

        Map<UUID, List<ChargeLineItem>> lineItemsByChargeId = lineItems.stream()
                .collect(Collectors.groupingBy(item -> item.getCharge().getId(), LinkedHashMap::new, Collectors.toList()));

        Map<String, ServiceCodes> serviceCodesByKey = loadServiceCodes(includedCharges);
        Map<String, Account> entitySettingsAccountsByKey = loadEntitySettingsAccounts(includedCharges);
        Map<String, APAccount> apAccountsByKey = loadApAccountsByEntitySettingsDefault(
                includedCharges, entitySettingsAccountsByKey);
        loadApAccountsFromCostLineItems(includedCharges, lineItemsByChargeId, apAccountsByKey);
        Map<String, ARAccount> dtcArAccountsByKey =
                loadDtcArAccounts(includedCharges, shipment, lineItemsByChargeId);
        ARAccount sellerArAccount = loadSellerArAccount(shipment).orElse(null);

        return shipmentChargeDetailsMapper.toResponse(
                shipment,
                normalizedChargeType,
                shipmentChargeCodes,
                includedCharges,
                lineItemsByChargeId,
                serviceCodesByKey,
                entitySettingsAccountsByKey,
                apAccountsByKey,
                dtcArAccountsByKey,
                sellerArAccount);
    }

    private Map<String, ServiceCodes> loadServiceCodes(List<Charge> charges) {
        Map<String, List<String>> chargeTypesByEntity = new LinkedHashMap<>();
        for (Charge charge : charges) {
            if (charge.getEntity() == null || charge.getEntity().getCode() == null) {
                continue;
            }
            String entityCode = charge.getEntity().getCode();
            String chargeType = RelatedChargeTypeResolver.normalize(charge.getChargeCode());
            chargeTypesByEntity.computeIfAbsent(entityCode, ignored -> new ArrayList<>()).add(chargeType);
        }

        Map<String, ServiceCodes> serviceCodesByKey = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : chargeTypesByEntity.entrySet()) {
            List<String> distinctTypes = entry.getValue().stream().distinct().toList();
            List<ServiceCodes> masters = serviceCodesRepository.findActiveMastersByEntityCodeAndChargeTypes(
                    entry.getKey(), distinctTypes);
            for (ServiceCodes master : masters) {
                serviceCodesByKey.put(
                        ShipmentChargeDetailsMapper.serviceCodeKey(
                                entry.getKey(),
                                master.getChargeType(),
                                master.getServiceCode()),
                        master);
            }
        }
        return serviceCodesByKey;
    }

    private Map<String, Account> loadEntitySettingsAccounts(List<Charge> charges) {
        Set<String> entityCodes = new LinkedHashSet<>();
        Set<String> chargeTypes = new LinkedHashSet<>();
        for (Charge charge : charges) {
            if (charge.getEntity() != null && charge.getEntity().getCode() != null) {
                entityCodes.add(charge.getEntity().getCode().toLowerCase(Locale.ROOT));
            }
            chargeTypes.add(RelatedChargeTypeResolver.normalize(charge.getChargeCode()));
        }
        if (entityCodes.isEmpty() || chargeTypes.isEmpty()) {
            return Map.of();
        }

        List<Account> accounts = accountRepository.findEntitySettingsAccountsByEntityCodesAndChargeTypes(
                entityCodes, chargeTypes);

        Map<String, Account> accountsByKey = new LinkedHashMap<>();
        for (Account account : accounts) {
            accountsByKey.put(
                    ShipmentChargeDetailsMapper.accountKey(account.getEntityCode(), account.getChargeType()),
                    account);
        }
        return accountsByKey;
    }

    /**
     * Loads AP masters referenced by Entity Settings {@code defaultAccountNumber}
     * for cost charge types (ECD/ECS/EC3). Keyed as entity|accountNumber.
     */
    private Map<String, APAccount> loadApAccountsByEntitySettingsDefault(
            List<Charge> charges,
            Map<String, Account> entitySettingsAccountsByKey) {

        Map<String, APAccount> apAccountsByKey = new LinkedHashMap<>();
        for (Charge charge : charges) {
            if (!ChargeTypeConstants.isCostAp(charge.getChargeCode())) {
                continue;
            }
            if (charge.getEntity() == null || charge.getEntity().getCode() == null) {
                continue;
            }
            String entityCode = charge.getEntity().getCode();
            String chargeType = RelatedChargeTypeResolver.normalize(charge.getChargeCode());
            Account entitySettings = entitySettingsAccountsByKey.get(
                    ShipmentChargeDetailsMapper.accountKey(entityCode, chargeType));
            if (entitySettings == null || !StringUtils.hasText(entitySettings.getDefaultAccountNumber())) {
                continue;
            }
            String defaultAccountNumber = entitySettings.getDefaultAccountNumber().trim();
            String key = ShipmentChargeDetailsMapper.apAccountKey(entityCode, defaultAccountNumber);
            if (apAccountsByKey.containsKey(key)) {
                continue;
            }
            apAccountRepository
                    .findByAccountNumberAndEntityCode(defaultAccountNumber, entityCode)
                    .ifPresent(apAccount -> apAccountsByKey.put(key, apAccount));
        }
        return apAccountsByKey;
    }

    private void loadApAccountsFromCostLineItems(
            List<Charge> charges,
            Map<UUID, List<ChargeLineItem>> lineItemsByChargeId,
            Map<String, APAccount> apAccountsByKey) {

        Set<String> lookedUpKeys = new LinkedHashSet<>();
        for (Charge charge : charges) {
            if (!ChargeTypeConstants.isCostAp(charge.getChargeCode())) {
                continue;
            }
            if (charge.getEntity() == null || !StringUtils.hasText(charge.getEntity().getCode())) {
                continue;
            }
            String entityCode = charge.getEntity().getCode().trim();
            for (ChargeLineItem lineItem : lineItemsByChargeId.getOrDefault(charge.getId(), List.of())) {
                if (lineItem == null || !StringUtils.hasText(lineItem.getApAccountNumber())) {
                    continue;
                }
                String apAccountNumber = lineItem.getApAccountNumber().trim();
                String key = ShipmentChargeDetailsMapper.apAccountKey(entityCode, apAccountNumber);
                if (apAccountsByKey.containsKey(key) || !lookedUpKeys.add(key)) {
                    continue;
                }
                apAccountRepository
                        .findByAccountNumberAndEntityCode(apAccountNumber, entityCode)
                        .ifPresent(apAccount -> apAccountsByKey.put(key, apAccount));
            }
        }
    }

    /**
     * Loads the DTC consignee AR master only when both charge-level account fields are available.
     * Keyed as entity|accountNumber.
     */
    private Map<String, ARAccount> loadDtcArAccounts(
            List<Charge> charges,
            Shipment shipment,
            Map<UUID, List<ChargeLineItem>> lineItemsByChargeId) {
        if (shipment == null
                || !StringUtils.hasText(shipment.getConsigneeNumber())
                || !StringUtils.hasText(shipment.getConsigneeName())) {
            return Map.of();
        }

        String accountNumber = shipment.getConsigneeNumber().trim();
        Map<String, ARAccount> arAccountsByKey = new LinkedHashMap<>();
        Set<String> lookedUpKeys = new LinkedHashSet<>();
        for (Charge charge : charges) {
            if (!ChargeTypeConstants.DTC.equals(
                    RelatedChargeTypeResolver.normalize(charge.getChargeCode()))) {
                continue;
            }
            if (charge.getEntity() == null || !StringUtils.hasText(charge.getEntity().getCode())) {
                continue;
            }
            boolean hasEmptyLineAccount = lineItemsByChargeId
                    .getOrDefault(charge.getId(), List.of())
                    .stream()
                    .anyMatch(item -> item.getAccount() == null
                            || (!StringUtils.hasText(item.getAccount().getAccountNumber())
                                    && !StringUtils.hasText(item.getAccount().getAccountName())));
            if (!hasEmptyLineAccount) {
                continue;
            }

            String entityCode = charge.getEntity().getCode().trim();
            String key = ShipmentChargeDetailsMapper.arAccountKey(entityCode, accountNumber);
            if (!lookedUpKeys.add(key)) {
                continue;
            }
            arAccountRepository
                    .findByAccountNumberAndEntityCode(accountNumber, entityCode)
                    .ifPresent(arAccount -> arAccountsByKey.put(key, arAccount));
        }
        return arAccountsByKey;
    }

    /**
     * Loads the seller AR master for shipper account number and origin entity.
     */
    private Optional<ARAccount> loadSellerArAccount(Shipment shipment) {
        if (shipment == null
                || !StringUtils.hasText(shipment.getShipperNumber())
                || !StringUtils.hasText(shipment.getOriginEntity())) {
            return Optional.empty();
        }
        return arAccountRepository.findByAccountNumberAndEntityCode(
                shipment.getShipperNumber().trim(),
                shipment.getOriginEntity().trim());
    }

}
