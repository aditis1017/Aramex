package com.customcharge.persistencelayer.util;

import com.customcharge.persistencelayer.entities.ARAccount;
import com.customcharge.persistencelayer.entities.APAccount;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.Entity;
import com.customcharge.persistencelayer.enums.AccountNature;
import com.customcharge.persistencelayer.repository.AccountRepository;
import com.customcharge.persistencelayer.repository.EntityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountChargeConfigConcurrencyTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private EntityRepository entityRepository;

    @Test
    void findOrCreateChargeConfig_shouldReturnExistingArConfigWithoutSave() {
        ARAccount master = arMaster();
        Account existing = arConfig(master, "DTA");

        when(accountRepository.findByArAccountIdAndChargeType(master.getId(), "DTA"))
                .thenReturn(Optional.of(existing));

        Account result = ArAccountMasterSupport.findOrCreateChargeConfig(
                accountRepository, entityRepository, master, entity(), "DTA");

        assertThat(result).isSameAs(existing);
        verify(accountRepository, never()).save(any());
    }

    @Test
    void findOrCreateChargeConfig_shouldCreateArConfigWhenMissing() {
        ARAccount master = arMaster();
        Account created = arConfig(master, "DTS");
        Entity managedEntity = entity();

        when(accountRepository.findByArAccountIdAndChargeType(master.getId(), "DTS"))
                .thenReturn(Optional.empty());
        when(entityRepository.findByCodeIgnoreCase("DXB")).thenReturn(Optional.of(managedEntity));
        when(accountRepository.saveChargeConfigOrFetchExisting(any(Account.class), any()))
                .thenReturn(created);

        Account result = ArAccountMasterSupport.findOrCreateChargeConfig(
                accountRepository, entityRepository, master, entity(), "DTS");

        assertThat(result).isSameAs(created);
        verify(accountRepository).saveChargeConfigOrFetchExisting(any(Account.class), any());
    }

    @Test
    void findOrCreateChargeConfig_shouldUseManagedEntity_whenTransientEntityProvided() {
        ARAccount master = arMaster();
        Entity transientEntity = new Entity();
        transientEntity.setCode("DXB");
        Entity managedEntity = entity();
        ReflectionTestUtils.setField(managedEntity, "id", 1L);

        when(accountRepository.findByArAccountIdAndChargeType(master.getId(), "DTC"))
                .thenReturn(Optional.empty());
        when(entityRepository.findByCodeIgnoreCase("DXB")).thenReturn(Optional.of(managedEntity));
        when(accountRepository.saveChargeConfigOrFetchExisting(any(Account.class), any()))
                .thenAnswer(invocation -> {
                    Account saved = invocation.getArgument(0);
                    assertThat(saved.getEntity()).isSameAs(managedEntity);
                    return saved;
                });

        ArAccountMasterSupport.findOrCreateChargeConfig(
                accountRepository, entityRepository, master, transientEntity, "DTC");

        verify(entityRepository).findByCodeIgnoreCase("DXB");
    }

    @Test
    void findOrCreateChargeConfig_shouldFetchExistingArConfigWhenConcurrentInsertWins() {
        ARAccount master = arMaster();
        Account winner = arConfig(master, "DT3");

        when(accountRepository.findByArAccountIdAndChargeType(master.getId(), "DT3"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(winner));
        when(entityRepository.findByCodeIgnoreCase("DXB")).thenReturn(Optional.of(entity()));
        when(accountRepository.saveChargeConfigOrFetchExisting(any(Account.class), any()))
                .thenAnswer(invocation -> fetchExisting(invocation).orElseThrow(
                        () -> new DataIntegrityViolationException("duplicate key")));

        Account result = ArAccountMasterSupport.findOrCreateChargeConfig(
                accountRepository, entityRepository, master, entity(), "DT3");

        assertThat(result).isSameAs(winner);
    }

    @Test
    void findOrCreateChargeConfig_shouldRethrowWhenArConflictCannotBeResolved() {
        ARAccount master = arMaster();

        when(accountRepository.findByArAccountIdAndChargeType(master.getId(), "DTA"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.empty());
        when(entityRepository.findByCodeIgnoreCase("DXB")).thenReturn(Optional.of(entity()));
        when(accountRepository.saveChargeConfigOrFetchExisting(any(Account.class), any()))
                .thenAnswer(invocation -> fetchExisting(invocation).orElseThrow(
                        () -> new DataIntegrityViolationException("duplicate key")));

        assertThatThrownBy(() -> ArAccountMasterSupport.findOrCreateChargeConfig(
                accountRepository, entityRepository, master, entity(), "DTA"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findOrCreateChargeConfig_shouldFetchExistingApConfigWhenConcurrentInsertWins() {
        APAccount master = apMaster();
        Account winner = apConfig(master, "ECD");

        when(accountRepository.findByApAccountIdAndChargeType(master.getId(), "ECD"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(winner));
        when(accountRepository.saveChargeConfigOrFetchExisting(any(Account.class), any()))
                .thenAnswer(invocation -> fetchExisting(invocation).orElseThrow(
                        () -> new DataIntegrityViolationException("duplicate key")));

        Account result = ApAccountMasterSupport.findOrCreateChargeConfig(
                accountRepository, master, entity(), "ECD");

        assertThat(result).isSameAs(winner);
    }

    @SuppressWarnings("unchecked")
    private static Optional<Account> fetchExisting(org.mockito.invocation.InvocationOnMock invocation) {
        Supplier<Optional<Account>> fetchExisting = invocation.getArgument(1);
        return fetchExisting.get();
    }

    private static Entity entity() {
        Entity entity = new Entity();
        entity.setCode("DXB");
        return entity;
    }

    private static ARAccount arMaster() {
        ARAccount master = new ARAccount();
        ReflectionTestUtils.setField(master, "id", UUID.randomUUID());
        master.setEntityCode("DXB");
        master.setAccountNumber("CUST-1");
        return master;
    }

    private static APAccount apMaster() {
        APAccount master = new APAccount();
        ReflectionTestUtils.setField(master, "id", UUID.randomUUID());
        master.setEntityCode("DXB");
        master.setAccountNumber("AP-1");
        return master;
    }

    private static Account arConfig(ARAccount master, String chargeType) {
        Account account = new Account();
        account.setEntity(entity());
        account.setAccountNature(AccountNature.AR);
        account.setArAccount(master);
        account.setChargeType(chargeType);
        return account;
    }

    private static Account apConfig(APAccount master, String chargeType) {
        Account account = new Account();
        account.setEntity(entity());
        account.setAccountNature(AccountNature.AP);
        account.setApAccount(master);
        account.setChargeType(chargeType);
        return account;
    }
}
