package com.customcharge.shipment.service.charge;

import com.customcharge.persistencelayer.accountRedis.AccountRedisAccountLookupService;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.enums.AccountNature;
import com.customcharge.persistencelayer.repository.AccountRepository;
import com.customcharge.persistencelayer.util.RevenueBillingAccountResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceChargeAccountResolverTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private ObjectProvider<AccountRedisAccountLookupService> accountRedisAccountLookupService;
    @Mock
    private DtaDefaultArAccountResolver dtaDefaultArAccountResolver;
    @Mock
    private RevenueBillingAccountResolver revenueBillingAccountResolver;
    @Mock
    private ObjectProvider<PartyArAccountResolver> partyArAccountResolverProvider;
    @Mock
    private PartyArAccountResolver partyArAccountResolver;

    private ServiceChargeAccountResolver resolver;

    @BeforeEach
    void setUp() {
        lenient().when(accountRedisAccountLookupService.getIfAvailable()).thenReturn(null);
        lenient().when(partyArAccountResolverProvider.getObject()).thenReturn(partyArAccountResolver);
        resolver = new ServiceChargeAccountResolver(
                accountRepository,
                accountRedisAccountLookupService,
                dtaDefaultArAccountResolver,
                revenueBillingAccountResolver,
                partyArAccountResolverProvider);
    }

    @Test
    void resolve_shouldReturnEmptyWhenInputsMissing() {
        assertThat(resolver.resolve(new Shipment(), "", "DTC")).isEmpty();
        assertThat(resolver.resolve(new Shipment(), "DXB", "")).isEmpty();
        verifyNoInteractions(accountRepository);
    }

    @Test
    void resolve_shouldUseEntitySettingsForCostCharges() {
        Shipment shipment = new Shipment();
        Account account = new Account();

        when(accountRepository.findByAccountNumberIsNullAndEntityCodeAndAccountNatureAndChargeType(
                "DXB", AccountNature.ENTITY_SETTINGS, "ECD"))
                .thenReturn(Optional.of(account));

        assertThat(resolver.resolve(shipment, " DXB ", " ecd ")).containsSame(account);
    }

    @Test
    void resolve_shouldPreferUnifiedResolverForDtc() {
        Shipment shipment = new Shipment();
        shipment.setConsigneeNumber(" AR123 ");
        Account billingAccount = new Account();

        when(revenueBillingAccountResolver.resolveBillingAccount("RUH", "DTC", "AR123"))
                .thenReturn(Optional.of(billingAccount));

        assertThat(resolver.resolve(shipment, "RUH", "DTC")).containsSame(billingAccount);
        verify(revenueBillingAccountResolver).resolveBillingAccount("RUH", "DTC", "AR123");
    }

    @Test
    void resolve_shouldUseUnifiedResolverForDta() {
        Shipment shipment = new Shipment();
        shipment.setConsigneeNumber("CONSIGNEE-IGNORED");
        Account dtaAccount = new Account();

        when(dtaDefaultArAccountResolver.resolveOrCreate("RUH")).thenReturn(Optional.of(dtaAccount));

        assertThat(resolver.resolve(shipment, "RUH", "DTA")).containsSame(dtaAccount);
        verify(dtaDefaultArAccountResolver).resolveOrCreate("RUH");
    }

    @Test
    void resolveArBillingAccount_shouldUseConfiguredDefaultArAccountForDta() {
        Account dtaAccount = new Account();
        dtaAccount.setAccountNature(AccountNature.AR);
        dtaAccount.setArAccount(new com.customcharge.persistencelayer.entities.ARAccount());
        when(dtaDefaultArAccountResolver.resolveOrCreate("RUH")).thenReturn(Optional.of(dtaAccount));

        assertThat(resolver.resolveArBillingAccount(new Shipment(), "RUH", "DTA")).containsSame(dtaAccount);
    }

    @Test
    void resolveArBillingAccount_shouldReturnEmptyForDta_whenDefaultArAccountCannotBeResolved() {
        when(dtaDefaultArAccountResolver.resolveOrCreate("RUH")).thenReturn(Optional.empty());

        assertThat(resolver.resolveArBillingAccount(new Shipment(), "RUH", "DTA")).isEmpty();
    }

    @Test
    void resolve_shouldFallbackToUnifiedResolverForDtc_whenConsigneeArMissing() {
        Shipment shipment = new Shipment();
        shipment.setConsigneeNumber("AR123");
        Account entityAccount = new Account();

        when(revenueBillingAccountResolver.resolveBillingAccount("RUH", "DTC", "AR123"))
                .thenReturn(Optional.of(entityAccount));

        assertThat(resolver.resolve(shipment, "RUH", "DTC")).containsSame(entityAccount);

        verify(revenueBillingAccountResolver).resolveBillingAccount("RUH", "DTC", "AR123");
    }

    @Test
    void resolve_shouldUseShipperNumberForDts() {
        Shipment shipment = new Shipment();
        shipment.setConsigneeNumber("CONS-1");
        shipment.setShipperNumber(" SHIP-1 ");
        Account arAccount = new Account();

        when(accountRepository.findByAccountNumberAndEntityCodeAndAccountNatureAndChargeType(
                "SHIP-1", "JED", AccountNature.AR, "DTS"))
                .thenReturn(Optional.of(arAccount));

        assertThat(resolver.resolve(shipment, "JED", "DTS")).containsSame(arAccount);
    }

    @Test
    void resolve_shouldUseThirdPartyNumberForDt3() {
        Shipment shipment = new Shipment();
        shipment.setConsigneeNumber("CONS-1");
        shipment.setShipperNumber("SHIP-1");
        shipment.setThirdPartyNumber(" TP-1 ");
        Account arAccount = new Account();

        when(accountRepository.findByAccountNumberAndEntityCodeAndAccountNatureAndChargeType(
                "TP-1", "JED", AccountNature.AR, "DT3"))
                .thenReturn(Optional.of(arAccount));

        assertThat(resolver.resolve(shipment, "JED", "DT3")).containsSame(arAccount);
    }

    @Test
    void resolveArBillingAccount_shouldUsePartyResolverForDts() {
        Shipment shipment = new Shipment();
        shipment.setShipperNumber("SHIP-1");
        Account arAccount = new Account();
        when(partyArAccountResolver.resolveOrCreate(shipment, "JED", "DTS")).thenReturn(Optional.of(arAccount));

        assertThat(resolver.resolveArBillingAccount(shipment, "JED", "DTS")).containsSame(arAccount);
        verifyNoInteractions(accountRepository);
    }

    @Test
    void resolveBillingAccount_shouldUsePartyResolverForDts_whenArMasterExists() {
        Shipment shipment = new Shipment();
        shipment.setShipperNumber("SHIP-1");
        Account entitySettings = new Account();
        Account partyAccount = new Account();
        when(partyArAccountResolver.resolveOrCreate(shipment, "JED", "DTS")).thenReturn(Optional.of(partyAccount));

        assertThat(resolver.resolveBillingAccount(shipment, "JED", "DTS", entitySettings)).containsSame(partyAccount);
    }

    @Test
    void resolveBillingAccount_shouldFallbackToEntitySettingsForDts_whenArMasterMissing() {
        Shipment shipment = new Shipment();
        shipment.setShipperNumber("SHIP-1");
        Account entitySettings = new Account();
        when(partyArAccountResolver.resolveOrCreate(shipment, "JED", "DTS")).thenReturn(Optional.empty());

        assertThat(resolver.resolveBillingAccount(shipment, "JED", "DTS", entitySettings)).containsSame(entitySettings);
    }

    @Test
    void resolveBillingAccount_shouldUsePartyResolverForDt3_whenArMasterExists() {
        Shipment shipment = new Shipment();
        shipment.setThirdPartyNumber("TP-1");
        Account entitySettings = new Account();
        Account partyAccount = new Account();
        when(partyArAccountResolver.resolveOrCreate(shipment, "DXB", "DT3")).thenReturn(Optional.of(partyAccount));

        assertThat(resolver.resolveBillingAccount(shipment, "DXB", "DT3", entitySettings)).containsSame(partyAccount);
    }

    @Test
    void resolveBillingAccount_shouldPreferConsigneeArForDtc() {
        Shipment shipment = new Shipment();
        shipment.setConsigneeNumber("CONS-1");
        Account entitySettings = new Account();
        Account billingAccount = new Account();
        when(revenueBillingAccountResolver.resolveBillingAccount("RUH", "DTC", "CONS-1"))
                .thenReturn(Optional.of(billingAccount));

        assertThat(resolver.resolveBillingAccount(shipment, "RUH", "DTC", entitySettings))
                .containsSame(billingAccount);
    }

    @Test
    void resolveBillingAccount_shouldUseDefaultCashAccountForDtc_whenConsigneeArMissing() {
        Shipment shipment = new Shipment();
        shipment.setConsigneeNumber("CONS-1");
        Account entitySettings = new Account();
        Account defaultCashAccount = new Account();
        when(revenueBillingAccountResolver.resolveBillingAccount("RUH", "DTC", "CONS-1"))
                .thenReturn(Optional.of(defaultCashAccount));

        assertThat(resolver.resolveBillingAccount(shipment, "RUH", "DTC", entitySettings))
                .containsSame(defaultCashAccount);
    }

    @Test
    void resolveBillingAccount_shouldFallbackToEntitySettingsForDtc_whenDefaultCashAccountMissing() {
        Shipment shipment = new Shipment();
        Account entitySettings = new Account();
        when(revenueBillingAccountResolver.resolveBillingAccount("RUH", "DTC", null))
                .thenReturn(Optional.of(entitySettings));

        assertThat(resolver.resolveBillingAccount(shipment, "RUH", "DTC", entitySettings)).containsSame(entitySettings);
    }

    @Test
    void resolve_shouldFallbackToEntitySettingsWhenPartyArMissing() {
        Shipment shipment = new Shipment();
        shipment.setShipperNumber("SHIP-1");
        Account entityAccount = new Account();

        when(accountRepository.findByAccountNumberAndEntityCodeAndAccountNatureAndChargeType(
                "SHIP-1", "JED", AccountNature.AR, "DTS"))
                .thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumberIsNullAndEntityCodeAndAccountNatureAndChargeType(
                "JED", AccountNature.ENTITY_SETTINGS, "DTS"))
                .thenReturn(Optional.of(entityAccount));

        assertThat(resolver.resolve(shipment, "JED", "DTS")).containsSame(entityAccount);
    }

    @Test
    void resolveBillingAccount_shouldReturnEmptyWhenInputsMissing() {
        assertThat(resolver.resolveBillingAccount(new Shipment(), "", "DTC", new Account())).isEmpty();
    }

    @Test
    void resolveBillingAccount_shouldUseDtaResolver() {
        Account dtaAccount = new Account();
        when(dtaDefaultArAccountResolver.resolveOrCreate("RUH")).thenReturn(Optional.of(dtaAccount));

        assertThat(resolver.resolveBillingAccount(new Shipment(), "RUH", "DTA", new Account()))
                .containsSame(dtaAccount);
    }

    @Test
    void resolveBillingAccount_shouldReturnFallbackForCostCharges() {
        Account fallback = new Account();

        assertThat(resolver.resolveBillingAccount(new Shipment(), "RUH", "ECD", fallback))
                .containsSame(fallback);
    }

    @Test
    void resolveArBillingAccount_shouldReturnEmptyWhenInputsMissing() {
        assertThat(resolver.resolveArBillingAccount(new Shipment(), "", "DTC")).isEmpty();
    }

    @Test
    void resolveArBillingAccount_shouldUseConsigneeArForEcd() {
        Shipment shipment = new Shipment();
        shipment.setConsigneeNumber("CONS-1");
        Account arAccount = new Account();

        when(accountRepository.findByAccountNumberAndEntityCodeAndAccountNatureAndChargeType(
                "CONS-1", "RUH", AccountNature.AR, "ECD"))
                .thenReturn(Optional.of(arAccount));

        assertThat(resolver.resolveArBillingAccount(shipment, "RUH", "ECD")).containsSame(arAccount);
    }

    @Test
    void resolveArBillingAccount_shouldReturnEmptyWhenPartyAccountMissing() {
        Shipment shipment = new Shipment();

        assertThat(resolver.resolveArBillingAccount(shipment, "RUH", "ECD")).isEmpty();
    }

    @Test
    void resolveEntitySettings_shouldReturnEmptyWhenInputsMissing() {
        assertThat(resolver.resolveEntitySettings("", "ECD")).isEmpty();
    }

    @Test
    void resolveEntitySettings_shouldDelegateToRepository() {
        Account account = new Account();
        when(accountRepository.findByAccountNumberIsNullAndEntityCodeAndAccountNatureAndChargeType(
                "RUH", AccountNature.ENTITY_SETTINGS, "ECD"))
                .thenReturn(Optional.of(account));

        assertThat(resolver.resolveEntitySettings("RUH", "ECD")).containsSame(account);
    }
}
