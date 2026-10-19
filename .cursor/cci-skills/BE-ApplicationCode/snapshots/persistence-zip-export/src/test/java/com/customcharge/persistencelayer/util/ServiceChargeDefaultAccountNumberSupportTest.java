package com.customcharge.persistencelayer.util;

import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.ServiceCharge;
import com.customcharge.persistencelayer.entities.ServiceCodes;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceChargeDefaultAccountNumberSupportTest {

    @Test
    void resolve_shouldPreferServiceChargeDefault() {
        Account account = new Account();
        account.setDefaultAccountNumber("ACCT-DEFAULT");

        ServiceCharge charge = new ServiceCharge();
        charge.setAccount(account);
        charge.setDefaultAccountNumber("SC-OVERRIDE");

        assertThat(ServiceChargeDefaultAccountNumberSupport.resolve(account, charge))
                .isEqualTo("SC-OVERRIDE");
    }

    @Test
    void resolve_shouldFallbackToAccountDefault_whenServiceChargeBlank() {
        Account account = new Account();
        account.setDefaultAccountNumber("ACCT-DEFAULT");

        ServiceCharge charge = new ServiceCharge();
        charge.setAccount(account);
        charge.setDefaultAccountNumber(" ");

        assertThat(ServiceChargeDefaultAccountNumberSupport.resolve(account, charge))
                .isEqualTo("ACCT-DEFAULT");
    }

    @Test
    void resolve_shouldFallbackToChargeAccount_whenAccountArgumentNull() {
        Account account = new Account();
        account.setDefaultAccountNumber("ACCT-DEFAULT");

        ServiceCharge charge = new ServiceCharge();
        charge.setAccount(account);

        assertThat(ServiceChargeDefaultAccountNumberSupport.resolve(null, charge))
                .isEqualTo("ACCT-DEFAULT");
    }

    @Test
    void resolve_shouldReturnNull_whenNoDefaultsConfigured() {
        assertThat(ServiceChargeDefaultAccountNumberSupport.resolve(null, null)).isNull();
        assertThat(ServiceChargeDefaultAccountNumberSupport.resolve(new Account(), new ServiceCharge()))
                .isNull();
    }

    @Test
    void toResponse_shouldExposeAccountDefaultForSyntheticCharge() {
        Account account = new Account();
        account.setDefaultAccountNumber("ACCT-DEFAULT");

        ServiceCharge charge = ServiceChargeResolver.defaultCharge(account, new ServiceCodes());

        assertThat(ServiceChargeResolver.toResponse(charge).getDefaultAccountNumber())
                .isEqualTo("ACCT-DEFAULT");
    }
}
