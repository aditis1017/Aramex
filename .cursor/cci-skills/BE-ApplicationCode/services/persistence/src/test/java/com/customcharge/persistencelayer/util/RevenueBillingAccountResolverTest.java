package com.customcharge.persistencelayer.util;

import com.customcharge.persistencelayer.accountRedis.AccountRedisAccountLookupService;
import com.customcharge.persistencelayer.entities.ARAccount;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.Entity;
import com.customcharge.persistencelayer.enums.AccountNature;
import com.customcharge.persistencelayer.repository.ARAccountRepository;
import com.customcharge.persistencelayer.repository.AccountRepository;
import com.customcharge.persistencelayer.repository.EntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevenueBillingAccountResolverTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private ARAccountRepository arAccountRepository;
    @Mock
    private EntityRepository entityRepository;
    @Mock
    private ObjectProvider<AccountRedisAccountLookupService> accountRedisAccountLookupService;

    @InjectMocks
    private RevenueBillingAccountResolver resolver;

    @BeforeEach
    void setUp() {
        lenient().when(accountRedisAccountLookupService.getIfAvailable()).thenReturn(null);
    }

    @Test
    void resolveBillingAccount_shouldReturnEmpty_whenInputsMissing() {
        assertThat(resolver.resolveBillingAccount(" ", "DTC", "CONS-1")).isEmpty();
        assertThat(resolver.resolveBillingAccount("RUH", " ", "CONS-1")).isEmpty();
        verifyNoInteractions(accountRepository, arAccountRepository);
    }

    @Test
    void resolveBillingAccount_shouldReturnEmpty_forUnsupportedChargeType() {
        assertThat(resolver.resolveBillingAccount("RUH", "DTS", "SHIP-1")).isEmpty();
        verifyNoInteractions(arAccountRepository);
    }

    @Test
    void resolveBillingAccount_shouldReturnEntitySettings_whenNoArMasterFound() {
        Account entitySettings = entitySettings("RUH", null);
        when(accountRepository.findByAccountNumberIsNullAndEntityCodeAndAccountNatureAndChargeType(
                "RUH", AccountNature.ENTITY_SETTINGS, "DTC"))
                .thenReturn(Optional.of(entitySettings));
        when(arAccountRepository.findByAccountNumberAndEntityCode("CONS-1", "RUH"))
                .thenReturn(Optional.empty());

        assertThat(resolver.resolveBillingAccount("RUH", "DTC", "CONS-1")).containsSame(entitySettings);
    }

    @Test
    void resolveBillingAccount_shouldPreferConsigneeArForDtc() {
        Account entitySettings = entitySettings("RUH", "CASH-100");
        ARAccount consigneeMaster = arMaster("CONS-1", "RUH");
        Account existing = new Account();
        when(accountRepository.findByAccountNumberIsNullAndEntityCodeAndAccountNatureAndChargeType(
                "RUH", AccountNature.ENTITY_SETTINGS, "DTC"))
                .thenReturn(Optional.of(entitySettings));
        when(arAccountRepository.findByAccountNumberAndEntityCode("CONS-1", "RUH"))
                .thenReturn(Optional.of(consigneeMaster));
        when(accountRepository.findByArAccountIdAndChargeType(consigneeMaster.getId(), "DTC"))
                .thenReturn(Optional.of(existing));

        assertThat(resolver.resolveBillingAccount("RUH", "DTC", "CONS-1")).containsSame(existing);
        verify(arAccountRepository, never()).findByAccountNumberAndEntityCode("CASH-100", "RUH");
    }

    @Test
    void resolveBillingAccount_shouldUseDefaultCashAccount_whenConsigneeArMissing() {
        Account entitySettings = entitySettings("RUH", " CASH-100 ");
        ARAccount cashMaster = arMaster("CASH-100", "RUH");
        Account cashBillingAccount = new Account();
        when(accountRepository.findByAccountNumberIsNullAndEntityCodeAndAccountNatureAndChargeType(
                "RUH", AccountNature.ENTITY_SETTINGS, "DTC"))
                .thenReturn(Optional.of(entitySettings));
        when(arAccountRepository.findByAccountNumberAndEntityCode("CONS-1", "RUH"))
                .thenReturn(Optional.empty());
        when(arAccountRepository.findByAccountNumberAndEntityCode("CASH-100", "RUH"))
                .thenReturn(Optional.of(cashMaster));
        when(accountRepository.findByArAccountIdAndChargeType(cashMaster.getId(), "DTC"))
                .thenReturn(Optional.of(cashBillingAccount));

        assertThat(resolver.resolveBillingAccount("RUH", "DTC", "CONS-1")).containsSame(cashBillingAccount);
    }

    @Test
    void resolveBillingAccount_shouldCreateMinimalDtcAccount_whenConfigMissing() {
        Account entitySettings = entitySettings("RUH", null);
        ARAccount consigneeMaster = arMaster("CONS-1", "RUH");
        when(accountRepository.findByAccountNumberIsNullAndEntityCodeAndAccountNatureAndChargeType(
                "RUH", AccountNature.ENTITY_SETTINGS, "DTC"))
                .thenReturn(Optional.of(entitySettings));
        when(arAccountRepository.findByAccountNumberAndEntityCode("CONS-1", "RUH"))
                .thenReturn(Optional.of(consigneeMaster));
        when(accountRepository.findByArAccountIdAndChargeType(consigneeMaster.getId(), "DTC"))
                .thenReturn(Optional.empty());
        when(entityRepository.findByCodeIgnoreCase("RUH"))
                .thenReturn(Optional.of(entitySettings.getEntity()));
        when(accountRepository.saveChargeConfigOrFetchExisting(any(Account.class), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Account created = resolver.resolveBillingAccount("RUH", "DTC", "CONS-1").orElseThrow();

        assertThat(created.getEntity()).isSameAs(entitySettings.getEntity());
        assertThat(created.getArAccount()).isSameAs(consigneeMaster);
        assertThat(created.getAccountNature()).isEqualTo(AccountNature.AR);
        assertThat(created.getChargeType()).isEqualTo("DTC");
    }

    @Test
    void resolveBillingAccount_shouldCreateMinimalDtcAccount_whenEntitySettingsLoadedFromRedis() {
        Account redisEntitySettings = redisEntitySettings("RUH", "2001");
        ARAccount cashMaster = arMaster("2001", "RUH");
        Entity managedEntity = managedEntity("RUH");
        AccountRedisAccountLookupService lookupService = org.mockito.Mockito.mock(AccountRedisAccountLookupService.class);
        when(accountRedisAccountLookupService.getIfAvailable()).thenReturn(lookupService);
        when(lookupService.tryResolveEntitySettings("RUH", "DTC")).thenReturn(Optional.of(redisEntitySettings));
        when(lookupService.tryResolveArChargeAccount("CONS-1", "RUH", "DTC")).thenReturn(Optional.empty());
        when(lookupService.tryResolveArChargeAccount("2001", "RUH", "DTC")).thenReturn(Optional.empty());
        when(arAccountRepository.findByAccountNumberAndEntityCode("CONS-1", "RUH"))
                .thenReturn(Optional.empty());
        when(arAccountRepository.findByAccountNumberAndEntityCode("2001", "RUH"))
                .thenReturn(Optional.of(cashMaster));
        when(accountRepository.findByArAccountIdAndChargeType(cashMaster.getId(), "DTC"))
                .thenReturn(Optional.empty());
        when(entityRepository.findByCodeIgnoreCase("RUH")).thenReturn(Optional.of(managedEntity));
        when(accountRepository.saveChargeConfigOrFetchExisting(any(Account.class), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Account created = resolver.resolveBillingAccount("RUH", "DTC", "CONS-1").orElseThrow();

        assertThat(created.getEntity()).isSameAs(managedEntity);
        assertThat(created.getArAccount()).isSameAs(cashMaster);
        assertThat(created.getChargeType()).isEqualTo("DTC");
        verify(entityRepository).findByCodeIgnoreCase("RUH");
    }

    @Test
    void resolveBillingAccount_shouldCreateMinimalDtaAccount_whenEntitySettingsLoadedFromRedis() {
        Account redisEntitySettings = redisDtaEntitySettings("RUH", "AR-DTA-1");
        ARAccount arMaster = arMaster("AR-DTA-1", "RUH");
        Entity managedEntity = managedEntity("RUH");
        AccountRedisAccountLookupService lookupService = org.mockito.Mockito.mock(AccountRedisAccountLookupService.class);
        when(accountRedisAccountLookupService.getIfAvailable()).thenReturn(lookupService);
        when(lookupService.tryResolveEntitySettings("RUH", "DTA")).thenReturn(Optional.of(redisEntitySettings));
        when(lookupService.tryResolveArChargeAccount("AR-DTA-1", "RUH", "DTA")).thenReturn(Optional.empty());
        when(arAccountRepository.findByArAccountNumberAndEntityCode("AR-DTA-1", "RUH"))
                .thenReturn(Optional.of(arMaster));
        when(accountRepository.findByArAccountIdAndChargeType(arMaster.getId(), "DTA"))
                .thenReturn(Optional.empty());
        when(entityRepository.findByCodeIgnoreCase("RUH")).thenReturn(Optional.of(managedEntity));
        when(accountRepository.saveChargeConfigOrFetchExisting(any(Account.class), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Account created = resolver.resolveBillingAccount("RUH", "DTA", null).orElseThrow();

        assertThat(created.getEntity()).isSameAs(managedEntity);
        assertThat(created.getArAccount()).isSameAs(arMaster);
        assertThat(created.getChargeType()).isEqualTo("DTA");
    }

    @Test
    void resolveBillingAccount_shouldPreferDefaultArForDta() {
        Account entitySettings = dtaEntitySettings("RUH", "AR-DTA-1");
        ARAccount arMaster = arMaster("AR-DTA-1", "RUH");
        Account existing = new Account();
        when(accountRepository.findByAccountNumberIsNullAndEntityCodeAndAccountNatureAndChargeType(
                "RUH", AccountNature.ENTITY_SETTINGS, "DTA"))
                .thenReturn(Optional.of(entitySettings));
        when(arAccountRepository.findByArAccountNumberAndEntityCode("AR-DTA-1", "RUH"))
                .thenReturn(Optional.of(arMaster));
        when(accountRepository.findByArAccountIdAndChargeType(arMaster.getId(), "DTA"))
                .thenReturn(Optional.of(existing));

        assertThat(resolver.resolveBillingAccount("RUH", "DTA", null)).containsSame(existing);
    }

    @Test
    void resolveBillingAccount_shouldFallbackToEntitySettingsForDta_whenDefaultArMissing() {
        Account entitySettings = dtaEntitySettings("RUH", null);
        when(accountRepository.findByAccountNumberIsNullAndEntityCodeAndAccountNatureAndChargeType(
                "RUH", AccountNature.ENTITY_SETTINGS, "DTA"))
                .thenReturn(Optional.of(entitySettings));

        assertThat(resolver.resolveBillingAccount("RUH", "DTA", null)).containsSame(entitySettings);
        verifyNoInteractions(arAccountRepository);
    }

    @Test
    void resolveBillingAccount_shouldFallbackToEntitySettingsForDta_whenArMasterMissing() {
        Account entitySettings = dtaEntitySettings("RUH", "AR-DTA-1");
        when(accountRepository.findByAccountNumberIsNullAndEntityCodeAndAccountNatureAndChargeType(
                "RUH", AccountNature.ENTITY_SETTINGS, "DTA"))
                .thenReturn(Optional.of(entitySettings));
        when(arAccountRepository.findByArAccountNumberAndEntityCode("AR-DTA-1", "RUH"))
                .thenReturn(Optional.empty());

        assertThat(resolver.resolveBillingAccount("RUH", "DTA", null)).containsSame(entitySettings);
    }

    @Test
    void resolveBillingAccount_shouldUseRedis_whenArChargeConfigPresent() {
        Account entitySettings = entitySettings("RUH", "CASH-100");
        Account redisAccount = new Account();
        AccountRedisAccountLookupService lookupService = org.mockito.Mockito.mock(AccountRedisAccountLookupService.class);
        when(accountRedisAccountLookupService.getIfAvailable()).thenReturn(lookupService);
        when(lookupService.tryResolveEntitySettings("RUH", "DTC")).thenReturn(Optional.of(entitySettings));
        when(lookupService.tryResolveArChargeAccount("CONS-1", "RUH", "DTC")).thenReturn(Optional.of(redisAccount));

        assertThat(resolver.resolveBillingAccount("RUH", "DTC", "CONS-1")).containsSame(redisAccount);
        verifyNoInteractions(arAccountRepository);
        verify(accountRepository, never()).findByArAccountIdAndChargeType(any(), any());
    }

    private static Account redisEntitySettings(String entityCode, String defaultCashAccount) {
        Entity transientEntity = new Entity();
        transientEntity.setCode(entityCode);
        Account account = new Account();
        account.setEntity(transientEntity);
        account.setAccountNature(AccountNature.ENTITY_SETTINGS);
        account.setChargeType("DTC");
        account.setDefaultCashAccount(defaultCashAccount);
        return account;
    }

    private static Account redisDtaEntitySettings(String entityCode, String defaultArAccountNumber) {
        Entity transientEntity = new Entity();
        transientEntity.setCode(entityCode);
        Account account = new Account();
        account.setEntity(transientEntity);
        account.setAccountNature(AccountNature.ENTITY_SETTINGS);
        account.setChargeType("DTA");
        account.setDefaultArAccountNumber(defaultArAccountNumber);
        return account;
    }

    private static Entity managedEntity(String entityCode) {
        Entity entity = new Entity();
        entity.setCode(entityCode);
        ReflectionTestUtils.setField(entity, "id", 1L);
        return entity;
    }

    private static Account entitySettings(String entityCode, String defaultCashAccount) {
        Entity entity = new Entity();
        entity.setCode(entityCode);
        Account account = new Account();
        account.setEntity(entity);
        account.setAccountNature(AccountNature.ENTITY_SETTINGS);
        account.setChargeType("DTC");
        account.setDefaultCashAccount(defaultCashAccount);
        return account;
    }

    private static Account dtaEntitySettings(String entityCode, String defaultArAccountNumber) {
        Entity entity = new Entity();
        entity.setCode(entityCode);
        Account account = new Account();
        account.setEntity(entity);
        account.setAccountNature(AccountNature.ENTITY_SETTINGS);
        account.setChargeType("DTA");
        account.setDefaultArAccountNumber(defaultArAccountNumber);
        return account;
    }

    private static ARAccount arMaster(String accountNumber, String entityCode) {
        ARAccount arAccount = new ARAccount();
        ReflectionTestUtils.setField(arAccount, "id", UUID.randomUUID());
        arAccount.setEntityCode(entityCode);
        arAccount.setAccountNumber(accountNumber);
        arAccount.setAccountName("AR Master");
        return arAccount;
    }
}
