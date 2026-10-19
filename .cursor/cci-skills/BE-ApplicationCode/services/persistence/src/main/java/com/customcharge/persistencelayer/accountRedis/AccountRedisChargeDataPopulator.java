package com.customcharge.persistencelayer.accountRedis;

import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.RateSheet;
import com.customcharge.persistencelayer.entities.RateSheetItem;
import com.customcharge.persistencelayer.entities.ServiceCharge;
import com.customcharge.persistencelayer.entities.ServiceCodes;
import com.customcharge.persistencelayer.enums.AccountNature;
import com.customcharge.persistencelayer.repository.EntityRepository;
import com.customcharge.persistencelayer.repository.RateSheetItemRepository;
import com.customcharge.persistencelayer.repository.RateSheetRepository;
import com.customcharge.persistencelayer.repository.ServiceChargeRepository;
import com.customcharge.persistencelayer.repository.ServiceCodesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AccountRedisChargeDataPopulator {

    private final ServiceCodesRepository serviceCodesRepository;
    private final ServiceChargeRepository serviceChargeRepository;
    private final RateSheetRepository rateSheetRepository;
    private final RateSheetItemRepository rateSheetItemRepository;
    private final EntityRepository entityRepository;

    public void populate(ArAccountRedisChargeTypeData data, Account config) {
        if (data == null || config == null || config.getId() == null) {
            return;
        }
        String entityCode = config.getEntityCode();
        String chargeType = config.getChargeType();
        if (!StringUtils.hasText(entityCode) || !StringUtils.hasText(chargeType)) {
            return;
        }

        Double entityTaxValue = entityRepository.findByCodeIgnoreCase(entityCode.trim())
                .map(com.customcharge.persistencelayer.entities.Entity::getTaxValue)
                .orElse(null);

        List<ServiceCodes> masters =
                serviceCodesRepository.findByEntityCodeAndChargeTypeAndIsDeletedFalse(
                        entityCode.trim(), chargeType.trim());
        Map<UUID, ServiceCharge> chargeByMasterId = loadAccountCharges(config, entityCode, chargeType);

        data.setServiceCharges(masters.stream()
                .map(master -> AccountRedisServiceChargeMapper.toRedis(
                        master, chargeByMasterId.get(master.getId()), entityTaxValue))
                .toList());

        rateSheetRepository.findByAccount_Id(config.getId()).ifPresent(rateSheet -> {
            data.setRateSheetId(rateSheet.getId());
            List<ArAccountRedisRateSheetItem> items =
                    rateSheetItemRepository.findByRateSheet_IdAndIsDeletedFalse(rateSheet.getId()).stream()
                            .map(AccountRedisRateSheetMapper::toRedis)
                            .toList();
            data.setRateSheetItems(items);
        });

        if (data.getRateSheetItems() == null) {
            data.setRateSheetItems(List.of());
        }
        data.setChargeDataSynced(true);
    }

    private Map<UUID, ServiceCharge> loadAccountCharges(Account account, String entityCode, String chargeType) {
        if (AccountNature.AP.equals(account.getAccountNature())) {
            return Map.of();
        }
        List<ServiceCharge> charges = serviceChargeRepository.findByAccountWithActiveServiceCodes(
                account, entityCode.trim(), chargeType.trim());
        return charges.stream()
                .filter(charge -> charge.getServiceCodes() != null)
                .collect(Collectors.toMap(
                        charge -> charge.getServiceCodes().getId(),
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new));
    }

    public static String chargeTypeKey(String chargeType) {
        return chargeType == null || chargeType.isBlank()
                ? ""
                : chargeType.trim().toUpperCase(Locale.ROOT);
    }
}
