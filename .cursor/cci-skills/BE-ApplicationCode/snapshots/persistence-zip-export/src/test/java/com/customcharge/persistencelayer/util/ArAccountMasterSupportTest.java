package com.customcharge.persistencelayer.util;

import com.customcharge.persistencelayer.entities.ARAccount;
import com.customcharge.persistencelayer.entities.Account;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ArAccountMasterSupportTest {

    @Test
    void resolveRateCurrency_shouldReturnMasterValue_whenPresent() {
        ARAccount master = new ARAccount();
        master.setRateCurrency("USD");

        assertThat(ArAccountMasterSupport.resolveRateCurrency(master)).isEqualTo("USD");
    }

    @Test
    void resolveRateCurrency_shouldReturnNull_whenMasterMissing() {
        assertThat(ArAccountMasterSupport.resolveRateCurrency(null)).isNull();
        assertThat(ArAccountMasterSupport.resolveRateCurrency(new ARAccount())).isNull();
    }

    @Test
    void resolveBillingCurrency_shouldReturnMasterValue_whenPresent() {
        ARAccount master = new ARAccount();
        master.setBillingCurrency("EUR");

        assertThat(ArAccountMasterSupport.resolveBillingCurrency(master)).isEqualTo("EUR");
    }

    @Test
    void resolveBillingCurrency_shouldReturnNull_whenMasterMissing() {
        assertThat(ArAccountMasterSupport.resolveBillingCurrency(null)).isNull();
        assertThat(ArAccountMasterSupport.resolveBillingCurrency(new ARAccount())).isNull();
    }

    @Test
    void syncMasterIdentity_shouldUpdateNameAndCountry() {
        ARAccount master = new ARAccount();
        ArAccountMasterSupport.syncMasterIdentity(master, " Updated ", " SA ");
        assertThat(master.getAccountName()).isEqualTo("Updated");
        assertThat(master.getCountryCode()).isEqualTo("SA");
    }

    @Test
    void isArChargeConfig_shouldDetectArConfig() {
        Account config = new Account();
        config.setAccountNature(com.customcharge.persistencelayer.enums.AccountNature.AR);
        config.setArAccount(new ARAccount());
        assertThat(ArAccountMasterSupport.isArChargeConfig(config)).isTrue();
        assertThat(ArAccountMasterSupport.isArChargeConfig(new Account())).isFalse();
    }

    @Test
    void masterAccountNumber_shouldReturnArMasterNumber() {
        ARAccount master = new ARAccount();
        master.setAccountNumber("8001");
        Account config = new Account();
        config.setAccountNature(com.customcharge.persistencelayer.enums.AccountNature.AR);
        config.setArAccount(master);
        assertThat(ArAccountMasterSupport.masterAccountNumber(config)).isEqualTo("8001");
    }

    @Test
    void chargeTypesEqual_shouldTreatBlankValuesAsEqual() {
        assertThat(ArAccountMasterSupport.chargeTypesEqual(null, " ")).isTrue();
        assertThat(ArAccountMasterSupport.chargeTypesEqual("DTC", "DTC")).isTrue();
        assertThat(ArAccountMasterSupport.chargeTypesEqual("DTC", "DTA")).isFalse();
    }

    @Test
    void masterEntityCode_shouldReturnEntityCodeForNonArConfig() {
        Account account = new Account();
        com.customcharge.persistencelayer.entities.Entity entity = new com.customcharge.persistencelayer.entities.Entity();
        entity.setCode("DXB");
        account.setEntity(entity);
        assertThat(ArAccountMasterSupport.masterEntityCode(account)).isEqualTo("DXB");
    }
}
