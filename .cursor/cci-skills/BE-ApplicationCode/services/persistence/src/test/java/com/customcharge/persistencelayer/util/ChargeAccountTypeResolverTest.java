package com.customcharge.persistencelayer.util;

import com.customcharge.persistencelayer.constants.ChargeAccountTypeConstants;
import com.customcharge.persistencelayer.entities.ARAccount;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.Entity;
import com.customcharge.persistencelayer.enums.AccountNature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChargeAccountTypeResolverTest {

    @Mock
    private RevenueBillingAccountResolver revenueBillingAccountResolver;

    @InjectMocks
    private ChargeAccountTypeResolver resolver;

    @Test
    void resolve_shouldReturnAccount_forAllDdpCharges() {
        assertThat(resolver.resolve("DDP", "ECD", "RUH", "CONS-1"))
                .contains(ChargeAccountTypeConstants.ACCOUNT);
        assertThat(resolver.resolve("DDP", "DTC", "RUH", "CONS-1"))
                .contains(ChargeAccountTypeConstants.ACCOUNT);
        assertThat(resolver.resolve("DDP", "DTA", "RUH", null))
                .contains(ChargeAccountTypeConstants.ACCOUNT);
    }

    @Test
    void resolve_shouldReturnAccount_forDduEcd() {
        assertThat(resolver.resolve("DDU", "ECD", "RUH", "CONS-1"))
                .contains(ChargeAccountTypeConstants.ACCOUNT);
        assertThat(resolver.resolve("DDO", " ecd ", "RUH", null))
                .contains(ChargeAccountTypeConstants.ACCOUNT);
    }

    @Test
    void resolve_shouldReturnAccount_forDtc_whenBillingAccountMatchesConsignee() {
        Account billingAccount = arChargeConfig("CONS-1");
        when(revenueBillingAccountResolver.resolveBillingAccount("RUH", "DTC", "CONS-1"))
                .thenReturn(Optional.of(billingAccount));

        assertThat(resolver.resolve("DDU", "DTC", "RUH", " CONS-1 "))
                .contains(ChargeAccountTypeConstants.ACCOUNT);
    }

    @Test
    void resolve_shouldReturnCash_forDtc_whenBillingAccountDiffersFromConsignee() {
        Account billingAccount = arChargeConfig("CASH-100");
        when(revenueBillingAccountResolver.resolveBillingAccount("RUH", "DTC", "CONS-1"))
                .thenReturn(Optional.of(billingAccount));

        assertThat(resolver.resolve("DDU", "DTC", "RUH", "CONS-1"))
                .contains(ChargeAccountTypeConstants.CASH);
    }

    @Test
    void resolve_shouldReturnCash_forDtc_whenBillingAccountUsesEntitySettingsFallback() {
        Account entitySettings = new Account();
        entitySettings.setAccountNature(AccountNature.ENTITY_SETTINGS);
        when(revenueBillingAccountResolver.resolveBillingAccount("RUH", "DTC", "CONS-1"))
                .thenReturn(Optional.of(entitySettings));

        assertThat(resolver.resolve("DDU", "DTC", "RUH", "CONS-1"))
                .contains(ChargeAccountTypeConstants.CASH);
    }

    @Test
    void resolve_shouldReturnEmpty_forUnsupportedShipmentType() {
        assertThat(resolver.resolve("LV", "DTC", "RUH", "CONS-1")).isEmpty();
    }

    private static Account arChargeConfig(String masterAccountNumber) {
        ARAccount master = new ARAccount();
        master.setAccountNumber(masterAccountNumber);
        Entity entity = new Entity();
        entity.setCode("RUH");
        Account account = new Account();
        account.setEntity(entity);
        account.setAccountNature(AccountNature.AR);
        account.setArAccount(master);
        account.setChargeType("DTC");
        return account;
    }
}
