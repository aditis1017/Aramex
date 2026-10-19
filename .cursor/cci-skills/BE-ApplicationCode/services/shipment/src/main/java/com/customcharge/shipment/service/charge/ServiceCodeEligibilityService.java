package com.customcharge.shipment.service.charge;

import com.customcharge.persistencelayer.accountRedis.AccountRedisChargeDataService;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.RateSheetItem;
import com.customcharge.persistencelayer.entities.ServiceCodes;
import com.customcharge.persistencelayer.repository.ServiceCodesRepository;
import com.customcharge.shipment.constants.ShipmentChargeTypeSupport;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * Validates rate-sheet service codes against {@link ServiceCodes} masters for AR consignee accounts.
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ServiceCodeEligibilityService {

    ServiceCodesRepository serviceCodesRepository;
    AccountRedisChargeDataService accountRedisChargeDataService;

    public boolean isRateSheetItemEligible(
            Account account, String entityCode, String chargeCode, RateSheetItem item) {
        if (item == null || !StringUtils.hasText(entityCode) || !StringUtils.hasText(chargeCode)) {
            return false;
        }
        Optional<String> serviceCode = ChargeServiceCodeSupport.fromRateSheetItem(item);
        if (serviceCode.isEmpty()) {
            return false;
        }
        if (!ShipmentServiceCodeEligibilitySupport.isArAccount(account)) {
            return true;
        }

        String normalizedCharge = ShipmentChargeTypeSupport.normalize(chargeCode);
        List<ServiceCodes> masters = loadMasters(entityCode, normalizedCharge);

        Optional<ServiceCodes> primaryMaster = masters.stream()
                .filter(master -> normalizedCharge.equalsIgnoreCase(
                        ShipmentChargeTypeSupport.normalize(master.getChargeType())))
                .filter(master -> serviceCode.get().equalsIgnoreCase(master.getServiceCode()))
                .findFirst();
        if (primaryMaster.isEmpty()) {
            return false;
        }

        return ShipmentServiceCodeEligibilitySupport.isMasterEligibleForAccount(
                account, primaryMaster.get());
    }

    private List<ServiceCodes> loadMasters(String entityCode, String chargeCode) {
        List<ServiceCodes> fromRedis =
                accountRedisChargeDataService.getEntityServiceCodeMasters(entityCode.strip(), chargeCode);
        if (!fromRedis.isEmpty()
                || accountRedisChargeDataService
                        .readEntitySettingsChargeTypeData(entityCode, chargeCode)
                        .map(AccountRedisChargeDataService::isChargeDataSynced)
                        .orElse(false)) {
            return fromRedis;
        }
        return serviceCodesRepository.findByEntityCodeAndChargeTypeAndIsDeletedFalse(
                entityCode.strip(), chargeCode);
    }
}
