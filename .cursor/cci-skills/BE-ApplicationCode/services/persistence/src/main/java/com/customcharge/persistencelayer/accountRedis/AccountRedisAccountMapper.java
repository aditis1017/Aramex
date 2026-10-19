package com.customcharge.persistencelayer.accountRedis;

import com.customcharge.persistencelayer.entities.ARAccount;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.Entity;
import com.customcharge.persistencelayer.enums.AccountNature;
import com.customcharge.persistencelayer.util.AccountVersionSupport;
import org.springframework.util.StringUtils;

import java.util.Locale;

final class AccountRedisAccountMapper {

    private AccountRedisAccountMapper() {}

    static Account toEntitySettingsAccount(
            String entityCode, String chargeType, ArAccountRedisChargeTypeData chargeData) {
        Account account = new Account();
        AccountRedisEntityIdSupport.setId(account, chargeData.getAccountId());
        account.setAccountNature(AccountNature.ENTITY_SETTINGS);
        account.setChargeType(normalizeChargeType(chargeType));

        Entity entity = new Entity();
        entity.setCode(entityCode.strip());
        account.setEntity(entity);

        account.setSplitEntry(chargeData.getSplitEntry());
        account.setSplitInvoice(chargeData.getSplitInvoice());
        account.setInvoiceLimit(chargeData.getInvoiceLimit());
        account.setInvoiceFrequent(chargeData.getInvoiceFrequency());
        account.setEligibleForBilling(chargeData.getEligibleForBilling());
        account.setTriggerInvoice(chargeData.getInvoiceTrigger());
        account.setSapCountryCode(chargeData.getSapCountryCode());
        account.setEntryLimit(chargeData.getEntryLimit());
        account.setDefaultCashAccount(chargeData.getDefaultCashAccount());
        account.setDefaultArAccountNumber(chargeData.getDefaultArAccountNumber());
        account.setAccount(chargeData.getAccount());
        account.setDefaultAccountNumber(chargeData.getDefaultAccountNumber());
        if (StringUtils.hasText(chargeData.getBillingCurrency())) {
            account.setBillingCurrency(chargeData.getBillingCurrency());
        }
        AccountVersionSupport.ensureInitialized(account);
        return account;
    }

    static Account toArChargeAccount(
            ArAccountRedisPayload payload, String chargeType, ArAccountRedisChargeTypeData chargeData) {
        Account account = new Account();
        AccountRedisEntityIdSupport.setId(account, chargeData.getAccountId());
        account.setAccountNature(AccountNature.AR);
        account.setChargeType(normalizeChargeType(chargeType));

        String entityCode = payload.getEntity() == null ? null : payload.getEntity().strip();
        Entity entity = new Entity();
        entity.setCode(entityCode);
        account.setEntity(entity);

        ARAccount arMaster = new ARAccount();
        AccountRedisEntityIdSupport.setId(arMaster, payload.getId());
        arMaster.setEntityCode(entityCode);
        arMaster.setAccountNumber(payload.getAccountNo());
        arMaster.setAccountName(payload.getAccountName());
        arMaster.setCountryCode(payload.getCountry());
        arMaster.setArAccountNumber(payload.getArAccountNo());
        arMaster.setAccountType(payload.getAccountType());
        arMaster.setStatus(payload.getStatus());
        arMaster.setBusinessType(payload.getBusinessType());
        arMaster.setBusinessSegment(payload.getBusinessSegment());
        arMaster.setRateCurrency(payload.getRateCurrency());
        arMaster.setBillingCurrency(payload.getBillingCurrency());
        account.setArAccount(arMaster);

        account.setArInternalAccountNumber(chargeData.getArAccountNo());
        account.setArInvoiceFrequency(chargeData.getInvoiceFrequency());
        account.setArPotentialDelay(chargeData.getPotentialDelay());
        account.setArInvoiceAmountLimit(chargeData.getInvoiceAmountLimit());
        account.setArInvoiceShipmentLimit(chargeData.getInvoiceShipmentLimit());
        account.setArInvoiceBreakBy(chargeData.getInvoiceBreakBy());
        if (StringUtils.hasText(chargeData.getCustomType())) {
            arMaster.setCustomType(chargeData.getCustomType());
        } else if (StringUtils.hasText(payload.getCustomType())) {
            arMaster.setCustomType(payload.getCustomType());
        }
        account.setEligibleForBilling(chargeData.getEligibleForBilling());
        account.setTriggerInvoice(chargeData.getInvoiceTrigger());
        account.setArAmountCode(chargeData.getAmountCode());
        if (StringUtils.hasText(chargeData.getBillingCurrency())) {
            account.setBillingCurrency(chargeData.getBillingCurrency());
        } else if (StringUtils.hasText(payload.getBillingCurrency())) {
            account.setBillingCurrency(payload.getBillingCurrency());
        }
        if (StringUtils.hasText(chargeData.getRateCurrency())) {
            account.setRateCurrency(chargeData.getRateCurrency());
        } else if (StringUtils.hasText(payload.getRateCurrency())) {
            account.setRateCurrency(payload.getRateCurrency());
        }
        AccountVersionSupport.ensureInitialized(account);
        return account;
    }

    private static String normalizeChargeType(String chargeType) {
        return chargeType == null ? "" : chargeType.strip().toUpperCase(Locale.ROOT);
    }
}
