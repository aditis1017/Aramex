package com.customcharge.persistencelayer.accountRedis;

import com.customcharge.persistencelayer.entities.APAccount;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.enums.AccountNature;
import com.customcharge.persistencelayer.repository.AccountRepository;
import com.customcharge.persistencelayer.repository.RateSheetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ApAccountRedisCacheBuilder {

    private final AccountRepository accountRepository;
    private final RateSheetRepository rateSheetRepository;
    private final AccountRedisChargeDataPopulator chargeDataPopulator;

    public ArAccountRedisPayload build(APAccount master) {
        ArAccountRedisPayload payload = new ArAccountRedisPayload();
        populateMasterFields(master, payload);

        List<Account> configs = accountRepository.findAllByApAccountId(master.getId());
        payload.setCurrency(resolveCurrency(configs));
        payload.setCountry(resolveCountry(configs));
        payload.setCharges(buildChargesMap(configs));
        return payload;
    }

    /**
     * Builds cache payload from already-loaded rows (avoids extra DB reads on create/update hot paths).
     */
    public ArAccountRedisPayload build(APAccount master, Account config, UUID rateSheetId) {
        ArAccountRedisPayload payload = new ArAccountRedisPayload();
        populateMasterFields(master, payload);

        List<Account> configs = List.of(config);
        payload.setCurrency(resolveCurrency(configs));
        payload.setCountry(resolveCountry(configs));

        Map<String, ArAccountRedisChargeTypeData> chargesByType = new LinkedHashMap<>();
        String chargeKey = ArAccountRedisCacheBuilder.chargeTypeKey(config.getChargeType());
        chargesByType.put(chargeKey, toChargeTypeData(config, rateSheetId));
        payload.setCharges(chargesByType);
        return payload;
    }

    public static void populateMasterFields(APAccount master, ArAccountRedisPayload payload) {
        payload.setId(master.getId());
        payload.setAccountNo(master.getAccountNumber());
        payload.setAccountName(master.getAccountName());
        payload.setEntity(master.getEntityCode());
        payload.setAccountType(master.getAccountType());
        payload.setStatus(master.getStatus());
        payload.setMarketStructure(master.getMarketStructure());
    }

    private Map<String, ArAccountRedisChargeTypeData> buildChargesMap(List<Account> configs) {
        Map<String, ArAccountRedisChargeTypeData> chargesByType = new LinkedHashMap<>();
        for (Account config : configs) {
            String key = ArAccountRedisCacheBuilder.chargeTypeKey(config.getChargeType());
            chargesByType.put(key, toChargeTypeData(config));
        }
        return chargesByType;
    }

    private ArAccountRedisChargeTypeData toChargeTypeData(Account config) {
        return toChargeTypeData(config, null);
    }

    private ArAccountRedisChargeTypeData toChargeTypeData(Account config, UUID rateSheetId) {
        ArAccountRedisChargeTypeData data = new ArAccountRedisChargeTypeData();
        data.setAccountId(config.getId());
        if (rateSheetId != null) {
            data.setRateSheetId(rateSheetId);
        } else {
            rateSheetRepository.findByAccount_Id(config.getId()).ifPresent(rs -> data.setRateSheetId(rs.getId()));
        }
        if (AccountNature.AP.equals(config.getAccountNature())) {
            data.setRateCurrency(config.getRateCurrency());
            data.setBillingCurrency(config.getBillingCurrency());
        }
        chargeDataPopulator.populate(data, config);
        return data;
    }

    private static String resolveCurrency(List<Account> configs) {
        for (Account config : configs) {
            if (config.getBillingCurrency() != null) {
                return config.getBillingCurrency();
            }
        }
        return null;
    }

    private static String resolveCountry(List<Account> configs) {
        for (Account config : configs) {
            String countryCode = config.getCountryCode();
            if (countryCode != null) {
                return countryCode;
            }
        }
        return null;
    }
}
