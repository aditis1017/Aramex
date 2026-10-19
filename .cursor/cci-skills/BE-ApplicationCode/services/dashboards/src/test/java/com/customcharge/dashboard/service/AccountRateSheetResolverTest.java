package com.customcharge.dashboard.service;

import com.customcharge.persistencelayer.entities.APAccount;
import com.customcharge.persistencelayer.entities.ARAccount;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.Entity;
import com.customcharge.persistencelayer.entities.RateSheet;
import com.customcharge.persistencelayer.enums.AccountNature;
import com.customcharge.persistencelayer.exception.BadRequestException;
import com.customcharge.persistencelayer.exception.ResourceNotFoundException;
import com.customcharge.persistencelayer.repository.APAccountRepository;
import com.customcharge.persistencelayer.repository.ARAccountRepository;
import com.customcharge.persistencelayer.repository.AccountRepository;
import com.customcharge.persistencelayer.repository.EntityRepository;
import com.customcharge.persistencelayer.repository.RateSheetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountRateSheetResolverTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private ARAccountRepository arAccountRepository;
    @Mock
    private APAccountRepository apAccountRepository;
    @Mock
    private EntityRepository entityRepository;
    @Mock
    private RateSheetRepository rateSheetRepository;

    @InjectMocks
    private AccountRateSheetResolver resolver;

    @Test
    void validateAccountContext_shouldRejectNullAccountNature() {
        assertThatThrownBy(() -> resolver.validateAccountContext(null, "ACC1", "DXB", "DTC"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void validateAccountContext_shouldRejectMissingEntity() {
        assertThatThrownBy(() -> resolver.validateAccountContext(AccountNature.AR, "ACC1", " ", "DTC"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void validateAccountContext_shouldRejectMissingChargeType() {
        assertThatThrownBy(() -> resolver.validateAccountContext(AccountNature.AR, "ACC1", "DXB", " "))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void validateAccountContext_shouldRejectArWithoutAccountNumber() {
        assertThatThrownBy(() -> resolver.validateAccountContext(AccountNature.AR, " ", "DXB", "DTC"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void validateAccountContext_shouldAllowEntitySettingsWithoutAccountNumber() {
        resolver.validateAccountContext(AccountNature.ENTITY_SETTINGS, null, "DXB", "ECD");
    }

    @Test
    void findExistingChargeConfigAccountId_shouldReturnEmptyForIncompleteInput() {
        assertThat(resolver.findExistingChargeConfigAccountId(null, "ACC1", "DXB", "DTC")).isEmpty();
        assertThat(resolver.findExistingChargeConfigAccountId(AccountNature.AR, "ACC1", " ", "DTC")).isEmpty();
        assertThat(resolver.findExistingChargeConfigAccountId(AccountNature.AR, " ", "DXB", "DTC")).isEmpty();
    }

    @Test
    void findExistingChargeConfigAccountId_shouldReturnArConfigId() {
        UUID configId = UUID.randomUUID();
        ARAccount master = new ARAccount();
        ReflectionTestUtils.setField(master, "id", UUID.randomUUID());

        Account config = new Account();
        ReflectionTestUtils.setField(config, "id", configId);

        when(arAccountRepository.findByAccountNumberAndEntityCode("ACC1", "DXB")).thenReturn(Optional.of(master));
        when(accountRepository.findByArAccountIdAndChargeType(master.getId(), "DTC")).thenReturn(Optional.of(config));

        assertThat(resolver.findExistingChargeConfigAccountId(AccountNature.AR, "ACC1", "DXB", "dtc"))
                .contains(configId);
    }

    @Test
    void findExistingChargeConfigAccountId_shouldReturnEntitySettingsConfigId() {
        UUID configId = UUID.randomUUID();
        Account config = new Account();
        ReflectionTestUtils.setField(config, "id", configId);

        when(accountRepository.findByAccountNumberIsNullAndEntityCodeAndAccountNatureAndChargeType(
                "DXB", AccountNature.ENTITY_SETTINGS, "ECD")).thenReturn(Optional.of(config));

        assertThat(resolver.findExistingChargeConfigAccountId(
                AccountNature.ENTITY_SETTINGS, null, "DXB", "ecd")).contains(configId);
    }

    @Test
    void findExistingChargeConfigAccountId_shouldReturnApConfigId() {
        UUID configId = UUID.randomUUID();
        APAccount master = new APAccount();
        ReflectionTestUtils.setField(master, "id", UUID.randomUUID());

        Account config = new Account();
        ReflectionTestUtils.setField(config, "id", configId);

        when(apAccountRepository.findByAccountNumberAndEntityCode("AP01", "DXB")).thenReturn(Optional.of(master));
        when(accountRepository.findByApAccountIdAndChargeType(master.getId(), "ECD")).thenReturn(Optional.of(config));

        assertThat(resolver.findExistingChargeConfigAccountId(AccountNature.AP, "AP01", "DXB", "ECD"))
                .contains(configId);
    }

    @Test
    void resolveOrCreateRateSheet_shouldReturnExistingRateSheet() {
        UUID accountId = UUID.randomUUID();
        Account config = new Account();
        ReflectionTestUtils.setField(config, "id", accountId);

        RateSheet existing = new RateSheet();
        ReflectionTestUtils.setField(existing, "id", UUID.randomUUID());

        Entity entity = new Entity();
        entity.setActive(true);
        entity.setCode("DXB");

        when(entityRepository.findByCodeIgnoreCase("DXB")).thenReturn(Optional.of(entity));
        when(accountRepository.findByAccountNumberIsNullAndEntityCodeAndAccountNatureAndChargeType(
                "DXB", AccountNature.ENTITY_SETTINGS, "ECD")).thenReturn(Optional.of(config));
        when(rateSheetRepository.findByAccount_Id(accountId)).thenReturn(Optional.of(existing));

        RateSheet result = resolver.resolveOrCreateRateSheet(
                AccountNature.ENTITY_SETTINGS, null, "DXB", "ECD");

        assertThat(result).isSameAs(existing);
    }

    @Test
    void resolveOrCreateRateSheet_shouldCreateRateSheetWhenMissing() {
        UUID accountId = UUID.randomUUID();
        Account config = new Account();
        ReflectionTestUtils.setField(config, "id", accountId);

        RateSheet created = new RateSheet();
        ReflectionTestUtils.setField(created, "id", UUID.randomUUID());

        Entity entity = new Entity();
        entity.setActive(true);
        entity.setCode("DXB");

        when(entityRepository.findByCodeIgnoreCase("DXB")).thenReturn(Optional.of(entity));
        when(accountRepository.findByAccountNumberIsNullAndEntityCodeAndAccountNatureAndChargeType(
                "DXB", AccountNature.ENTITY_SETTINGS, "ECD")).thenReturn(Optional.of(config));
        when(rateSheetRepository.findByAccount_Id(accountId)).thenReturn(Optional.empty());
        when(rateSheetRepository.save(any(RateSheet.class))).thenReturn(created);

        RateSheet result = resolver.resolveOrCreateRateSheet(
                AccountNature.ENTITY_SETTINGS, null, "DXB", "ECD");

        assertThat(result).isSameAs(created);
        verify(rateSheetRepository).save(any(RateSheet.class));
    }

    @Test
    void resolveOrCreateRateSheet_shouldCreateEntitySettingsAccountWhenMissing() {
        UUID accountId = UUID.randomUUID();
        Account createdConfig = new Account();
        ReflectionTestUtils.setField(createdConfig, "id", accountId);

        RateSheet createdRateSheet = new RateSheet();
        ReflectionTestUtils.setField(createdRateSheet, "id", UUID.randomUUID());

        Entity entity = new Entity();
        entity.setActive(true);
        entity.setCode("DXB");

        when(entityRepository.findByCodeIgnoreCase("DXB")).thenReturn(Optional.of(entity));
        when(accountRepository.findByAccountNumberIsNullAndEntityCodeAndAccountNatureAndChargeType(
                "DXB", AccountNature.ENTITY_SETTINGS, "ECD")).thenReturn(Optional.empty());
        when(accountRepository.save(any(Account.class))).thenReturn(createdConfig);
        when(rateSheetRepository.findByAccount_Id(accountId)).thenReturn(Optional.empty());
        when(rateSheetRepository.save(any(RateSheet.class))).thenReturn(createdRateSheet);

        RateSheet result = resolver.resolveOrCreateRateSheet(
                AccountNature.ENTITY_SETTINGS, null, "DXB", "ECD");

        assertThat(result).isSameAs(createdRateSheet);
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void requireAccountConfiguration_shouldThrowWhenConfigMissing() {
        when(arAccountRepository.findByAccountNumberAndEntityCode("ACC1", "DXB")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.requireAccountConfiguration(
                AccountNature.AR, "ACC1", "DXB", "DTC"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void resolveChargeConfigAccount_shouldReturnApConfig() {
        UUID configId = UUID.randomUUID();
        APAccount master = new APAccount();
        ReflectionTestUtils.setField(master, "id", UUID.randomUUID());

        Account config = new Account();
        ReflectionTestUtils.setField(config, "id", configId);

        when(apAccountRepository.findByAccountNumberAndEntityCode("AP01", "DXB")).thenReturn(Optional.of(master));
        when(accountRepository.findByApAccountIdAndChargeType(master.getId(), "ECD")).thenReturn(Optional.of(config));

        assertThat(resolver.resolveChargeConfigAccount(AccountNature.AP, "AP01", "DXB", "ecd"))
                .isSameAs(config);
    }

    @Test
    void resolveChargeConfigAccount_shouldThrowWhenApMasterMissing() {
        when(apAccountRepository.findByAccountNumberAndEntityCode("AP01", "DXB")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolveChargeConfigAccount(AccountNature.AP, "AP01", "DXB", "ECD"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void resolveChargeConfigAccount_shouldThrowForUnsupportedNature() {
        assertThatThrownBy(() -> resolver.resolveChargeConfigAccount(null, "ACC1", "DXB", "DTC"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void resolveOrCreateRateSheet_shouldRejectInactiveEntity() {
        Entity entity = new Entity();
        entity.setActive(false);
        entity.setCode("DXB");

        when(entityRepository.findByCodeIgnoreCase("DXB")).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> resolver.resolveOrCreateRateSheet(
                AccountNature.ENTITY_SETTINGS, null, "DXB", "ECD"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void resolveOrCreateRateSheet_shouldReturnExistingArRateSheet() {
        UUID accountId = UUID.randomUUID();
        ARAccount master = new ARAccount();
        ReflectionTestUtils.setField(master, "id", UUID.randomUUID());

        Account config = new Account();
        ReflectionTestUtils.setField(config, "id", accountId);

        Entity entity = new Entity();
        entity.setActive(true);
        entity.setCode("DXB");

        RateSheet existing = new RateSheet();
        ReflectionTestUtils.setField(existing, "id", UUID.randomUUID());

        when(entityRepository.findByCodeIgnoreCase("DXB")).thenReturn(Optional.of(entity));
        when(arAccountRepository.findByAccountNumberAndEntityCode("ACC1", "DXB")).thenReturn(Optional.of(master));
        when(accountRepository.findByArAccountIdAndChargeType(master.getId(), "DTC")).thenReturn(Optional.of(config));
        when(rateSheetRepository.findByAccount_Id(accountId)).thenReturn(Optional.of(existing));

        RateSheet result = resolver.resolveOrCreateRateSheet(AccountNature.AR, "ACC1", "DXB", "DTC");

        assertThat(result).isSameAs(existing);
    }

    @Test
    void resolveOrCreateRateSheet_shouldCreateArChargeConfigAndRateSheet() {
        UUID accountId = UUID.randomUUID();
        ARAccount master = new ARAccount();
        ReflectionTestUtils.setField(master, "id", UUID.randomUUID());

        Account createdConfig = new Account();
        ReflectionTestUtils.setField(createdConfig, "id", accountId);

        Entity entity = new Entity();
        entity.setActive(true);
        entity.setCode("DXB");

        RateSheet createdRateSheet = new RateSheet();
        ReflectionTestUtils.setField(createdRateSheet, "id", UUID.randomUUID());

        when(entityRepository.findByCodeIgnoreCase("DXB")).thenReturn(Optional.of(entity));
        when(arAccountRepository.findByAccountNumberAndEntityCode("ACC1", "DXB")).thenReturn(Optional.of(master));
        when(accountRepository.findByArAccountIdAndChargeType(master.getId(), "DTC")).thenReturn(Optional.empty());
        when(accountRepository.save(any(Account.class))).thenReturn(createdConfig);
        when(rateSheetRepository.findByAccount_Id(accountId)).thenReturn(Optional.empty());
        when(rateSheetRepository.save(any(RateSheet.class))).thenReturn(createdRateSheet);

        RateSheet result = resolver.resolveOrCreateRateSheet(AccountNature.AR, "ACC1", "DXB", "DTC");

        assertThat(result).isSameAs(createdRateSheet);
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void resolveChargeConfigAccount_shouldReturnArConfig() {
        UUID configId = UUID.randomUUID();
        ARAccount master = new ARAccount();
        ReflectionTestUtils.setField(master, "id", UUID.randomUUID());

        Account config = new Account();
        ReflectionTestUtils.setField(config, "id", configId);

        when(arAccountRepository.findByAccountNumberAndEntityCode("ACC1", "DXB")).thenReturn(Optional.of(master));
        when(accountRepository.findByArAccountIdAndChargeType(master.getId(), "DTC")).thenReturn(Optional.of(config));

        assertThat(resolver.resolveChargeConfigAccount(AccountNature.AR, "ACC1", "DXB", "DTC"))
                .isSameAs(config);
    }

    @Test
    void resolveChargeConfigAccount_shouldThrowWhenArConfigMissing() {
        ARAccount master = new ARAccount();
        ReflectionTestUtils.setField(master, "id", UUID.randomUUID());

        when(arAccountRepository.findByAccountNumberAndEntityCode("ACC1", "DXB")).thenReturn(Optional.of(master));
        when(accountRepository.findByArAccountIdAndChargeType(master.getId(), "DTC")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolveChargeConfigAccount(AccountNature.AR, "ACC1", "DXB", "DTC"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void requireAccountConfiguration_shouldPassWhenConfigExists() {
        ARAccount master = new ARAccount();
        ReflectionTestUtils.setField(master, "id", UUID.randomUUID());

        Account config = new Account();
        ReflectionTestUtils.setField(config, "id", UUID.randomUUID());

        when(arAccountRepository.findByAccountNumberAndEntityCode("ACC1", "DXB")).thenReturn(Optional.of(master));
        when(accountRepository.findByArAccountIdAndChargeType(master.getId(), "DTC")).thenReturn(Optional.of(config));

        resolver.requireAccountConfiguration(AccountNature.AR, "ACC1", "DXB", "DTC");
    }

    @Test
    void findExistingChargeConfigAccountId_shouldReturnEmptyForApBlankAccountNumber() {
        assertThat(resolver.findExistingChargeConfigAccountId(AccountNature.AP, " ", "DXB", "ECD"))
                .isEmpty();
    }

    @Test
    void resolveChargeConfigAccount_shouldThrowWhenEntitySettingsConfigMissing() {
        when(accountRepository.findByAccountNumberIsNullAndEntityCodeAndAccountNatureAndChargeType(
                "DXB", AccountNature.ENTITY_SETTINGS, "ECD")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolveChargeConfigAccount(
                AccountNature.ENTITY_SETTINGS, null, "DXB", "ECD"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void resolveChargeConfigAccount_shouldThrowWhenApAccountNumberBlank() {
        assertThatThrownBy(() -> resolver.resolveChargeConfigAccount(AccountNature.AP, " ", "DXB", "ECD"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void requireAccountConfiguration_shouldFailWhenChargeConfigMissing() {
        when(arAccountRepository.findByAccountNumberAndEntityCode("ACC1", "DXB")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.requireAccountConfiguration(
                AccountNature.AR, "ACC1", "DXB", "DTC"))
                .isInstanceOf(BadRequestException.class);
    }
}
