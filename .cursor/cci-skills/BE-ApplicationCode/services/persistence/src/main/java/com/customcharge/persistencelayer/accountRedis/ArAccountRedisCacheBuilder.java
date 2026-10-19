package com.customcharge.persistencelayer.accountRedis;

import com.customcharge.persistencelayer.entities.ARAccount;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.enums.AccountNature;
import com.customcharge.persistencelayer.repository.AccountRepository;
import com.customcharge.persistencelayer.repository.RateSheetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ArAccountRedisCacheBuilder {

    private final AccountRepository accountRepository;
    private final RateSheetRepository rateSheetRepository;
    private final AccountRedisChargeDataPopulator chargeDataPopulator;

    public ArAccountRedisPayload build(ARAccount master) {
        ArAccountRedisPayload payload = new ArAccountRedisPayload();
        populateMasterFields(master, payload);

        List<Account> configs = accountRepository.findAllByArAccountId(master.getId());
        payload.setCurrency(resolveCurrency(master, configs));
        payload.setCharges(buildChargesMap(master, configs));
        return payload;
    }

    public static void populateMasterFields(ARAccount master, ArAccountRedisPayload payload) {
        payload.setId(master.getId());
        payload.setAccountNo(master.getAccountNumber());
        payload.setAccountName(master.getAccountName());
        payload.setCountry(master.getCountryCode());
        payload.setEntity(master.getEntityCode());
        payload.setArAccountNo(master.getArAccountNumber());
        payload.setAccountType(master.getAccountType());
        payload.setStatus(master.getStatus());
        payload.setBusinessType(master.getBusinessType());
        payload.setBusinessSegment(master.getBusinessSegment());
        payload.setRateCurrency(master.getRateCurrency());
        payload.setBillingCurrency(master.getBillingCurrency());
        payload.setCustomType(master.getCustomType());
        payload.setTeamId(master.getTeamId());
        payload.setTeamName(master.getTeamName());
        payload.setReachAccountId(master.getReachAccountId());
        payload.setCreatedDate(master.getCreatedDate());
        payload.setCreatedBy(master.getCreatedBy());
    }

    public Map<String, ArAccountRedisChargeTypeData> buildChargesMap(ARAccount master, List<Account> configs) {
        Map<String, ArAccountRedisChargeTypeData> chargesByType = new LinkedHashMap<>();
        for (Account config : configs) {
            String key = chargeTypeKey(config.getChargeType());
            chargesByType.put(key, toChargeTypeData(master, config));
        }
        return chargesByType;
    }

    public ArAccountRedisChargeTypeData toChargeTypeData(ARAccount master, Account config) {
        ArAccountRedisChargeTypeData data = new ArAccountRedisChargeTypeData();
        data.setAccountId(config.getId());
        rateSheetRepository.findByAccount_Id(config.getId()).ifPresent(rs -> data.setRateSheetId(rs.getId()));

        if (AccountNature.AR.equals(config.getAccountNature())) {
            data.setArAccountNo(config.getArInternalAccountNumber());
            data.setInvoiceFrequency(config.getInvoiceFrequent());
            data.setPotentialDelay(config.getPotentialDelay());
            data.setInvoiceAmountLimit(config.getInvoiceAmountLimit());
            data.setInvoiceShipmentLimit(config.getInvoiceShipmentLimit());
            data.setInvoiceBreakBy(config.getInvoiceBreakBy());
            data.setCustomType(master == null ? null : master.getCustomType());
            data.setEligibleForBilling(config.getEligibleForBilling());
            data.setInvoiceTrigger(config.getTriggerInvoice());
            data.setAmountCode(config.getAmount());
        } else if (AccountNature.ENTITY_SETTINGS.equals(config.getAccountNature())) {
            data.setSplitEntry(config.getSplitEntry());
            data.setSplitInvoice(config.getSplitInvoice());
            data.setInvoiceLimit(config.getInvoiceLimit());
            data.setInvoiceFrequency(config.getInvoiceFrequent());
            data.setEligibleForBilling(config.getEligibleForBilling());
            data.setInvoiceTrigger(config.getInvoiceTrigger());
            data.setSapCountryCode(config.getSapCountryCode());
            data.setEntryLimit(config.getEntryLimit());
            data.setDefaultCashAccount(config.getDefaultCashAccount());
            data.setDefaultArAccountNumber(config.getDefaultArAccountNumber());
            data.setAccount(config.getAccount());
            data.setDefaultAccountNumber(config.getDefaultAccountNumber());
            if (config.getBillingCurrency() != null) {
                data.setBillingCurrency(config.getBillingCurrency());
            }
        }

        chargeDataPopulator.populate(data, config);
        return data;
    }

    private static String resolveCurrency(ARAccount master, List<Account> configs) {
        if (master != null && master.getBillingCurrency() != null && !master.getBillingCurrency().isBlank()) {
            return master.getBillingCurrency().trim();
        }
        for (Account config : configs) {
            if (config.getBillingCurrency() != null) {
                return config.getBillingCurrency();
            }
        }
        return null;
    }

    public static String chargeTypeKey(String chargeType) {
        return chargeType == null || chargeType.isBlank() ? "" : chargeType.trim().toUpperCase();
    }
}
