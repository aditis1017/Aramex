package com.customcharge.persistencelayer.util;

import com.customcharge.persistencelayer.accountRedis.AccountRedisChargeDataService;
import com.customcharge.persistencelayer.dto.ServiceChargeQueryCriteria;
import com.customcharge.persistencelayer.dto.ServiceChargeResponse;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.ServiceCharge;
import com.customcharge.persistencelayer.entities.ServiceCodes;
import com.customcharge.persistencelayer.repository.ServiceChargeRepository;
import com.customcharge.persistencelayer.repository.ServiceCodesRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ServiceChargeResolver {

    private final ServiceCodesRepository serviceCodesRepository;
    private final ServiceChargeRepository serviceChargeRepository;
    private final ObjectProvider<AccountRedisChargeDataService> accountRedisChargeDataService;
    private final EntitySettingsServiceChargeFilter entitySettingsServiceChargeFilter;

    public ServiceChargeResolver(
            ServiceCodesRepository serviceCodesRepository,
            ServiceChargeRepository serviceChargeRepository,
            ObjectProvider<AccountRedisChargeDataService> accountRedisChargeDataService,
            EntitySettingsServiceChargeFilter entitySettingsServiceChargeFilter) {
        this.serviceCodesRepository = serviceCodesRepository;
        this.serviceChargeRepository = serviceChargeRepository;
        this.accountRedisChargeDataService = accountRedisChargeDataService;
        this.entitySettingsServiceChargeFilter = entitySettingsServiceChargeFilter;
    }

    public List<ServiceChargeResponse> resolve(ServiceChargeQueryCriteria criteria) {
        if (criteria == null || !StringUtils.hasText(criteria.getEntityCode())) {
            return List.of();
        }

        AccountRedisChargeDataService redisChargeDataService = accountRedisChargeDataService.getIfAvailable();
        if (redisChargeDataService != null) {
            Optional<List<ServiceChargeResponse>> fromRedis = redisChargeDataService.tryResolveFromRedis(criteria);
            if (fromRedis.isPresent()) {
                return entitySettingsServiceChargeFilter.filterArResponsesByEntitySettings(
                        criteria, fromRedis.get());
            }
        }

        return entitySettingsServiceChargeFilter.filterArResponsesByEntitySettings(
                criteria, resolveFromDatabase(criteria));
    }

    private List<ServiceChargeResponse> resolveFromDatabase(ServiceChargeQueryCriteria criteria) {
        List<ServiceCodes> masters = serviceCodesRepository.findActiveMastersByEntityCodeAndChargeTypes(
                criteria.getEntityCode().strip(),
                normalizeChargeTypesFilter(criteria.getChargeTypes()));
        if (masters.isEmpty()) {
            return List.of();
        }

        Account account = criteria.getAccount();
        if (account == null) {
            return masters.stream()
                    .map(master -> toResponse(defaultCharge(null, master)))
                    .toList();
        }

        List<UUID> masterIds = masters.stream().map(ServiceCodes::getId).toList();
        List<ServiceCharge> existing = serviceChargeRepository.findByAccountAndServiceCodesIn(account, masterIds);
        Map<UUID, ServiceCharge> byServiceCodeId = existing.stream()
                .collect(Collectors.toMap(sc -> sc.getServiceCodes().getId(), Function.identity(), (a, b) -> a));

        List<ServiceChargeResponse> result = new ArrayList<>(masters.size());
        for (ServiceCodes master : masters) {
            if (!isEligibleMaster(account, master)) {
                continue;
            }
            ServiceCharge charge = byServiceCodeId.get(master.getId());
            if (charge != null) {
                if (!isChargeActive(charge)) {
                    continue;
                }
                if (Boolean.FALSE.equals(criteria.getActive())) {
                    continue;
                }
                result.add(toResponse(charge));
            } else if (!Boolean.FALSE.equals(criteria.getActive())) {
                result.add(toResponse(defaultCharge(account, master)));
            }
        }
        return result;
    }

    private static boolean isChargeActive(ServiceCharge charge) {
        return charge.getActive() == null || charge.getActive();
    }

    private static boolean isEligibleMaster(Account account, ServiceCodes master) {
        if (Boolean.FALSE.equals(master.getIsActive())) {
            return false;
        }
        return true;
    }

    public static ServiceChargeResponse toResponse(ServiceCharge charge) {
        ServiceCodes master = charge != null ? charge.getServiceCodes() : null;
        boolean active = charge == null || charge.getActive() == null || charge.getActive();
        Double entityTaxValue = master != null && master.getEntity() != null
                ? master.getEntity().getTaxValue()
                : null;
        ServiceChargeTaxResolutionSupport.ResolvedTax tax =
                ServiceChargeTaxResolutionSupport.resolve(charge, master, entityTaxValue);
        return new ServiceChargeResponse(
                charge != null ? charge.getId() : null,
                master != null ? master.getServiceCode() : null,
                master != null ? master.getServiceDescription() : null,
                master != null ? master.getServiceDescription() : null,
                master != null ? master.getType() : null,
                active,
                tax.taxValue(),
                tax.taxCode(),
                active,
                defaultAccountNumberForResponse(charge),
                charge == null || charge.getIsBillable() == null || charge.getIsBillable(),
                charge != null && charge.getSplit() != null ? charge.getSplit() : "1",
                "",
                "",
                master != null ? master.getInfoAxsId() : null,
                master != null ? master.getFetchTax() : null);
    }


    private static String defaultAccountNumberForResponse(ServiceCharge charge) {
        String resolved = ServiceChargeDefaultAccountNumberSupport.resolve(null, charge);
        return resolved != null ? resolved : "";
    }

    public static ServiceCharge defaultCharge(Account account, ServiceCodes serviceCodes) {
        ServiceCharge charge = new ServiceCharge();
        charge.setAccount(account);
        charge.setServiceCodes(serviceCodes);
        charge.setActive(true);
        charge.setIsBillable(true);
        charge.setSplit("1");
        charge.setDefaultAccountNumber("");
        return charge;
    }

    private static List<String> normalizeChargeTypesFilter(List<String> chargeTypes) {
        if (chargeTypes == null || chargeTypes.isEmpty()) {
            return null;
        }
        List<String> normalized = chargeTypes.stream()
                .filter(t -> t != null && !t.isBlank())
                .map(String::strip)
                .distinct()
                .toList();
        return normalized.isEmpty() ? null : normalized;
    }
}
