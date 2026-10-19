package com.customcharge.persistencelayer.util;

import com.customcharge.persistencelayer.entities.APAccount;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.Entity;
import com.customcharge.persistencelayer.enums.AccountNature;
import com.customcharge.persistencelayer.repository.APAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApAccountMasterSupportTest {

    @Mock
    private APAccountRepository apAccountRepository;

    @Test
    void findOrCreateMaster_shouldReturnExisting() {
        APAccount existing = apMaster("AP01", "DXB");
        when(apAccountRepository.findByAccountNumberAndEntityCode("AP01", "DXB")).thenReturn(Optional.of(existing));

        assertThat(ApAccountMasterSupport.findOrCreateMaster(apAccountRepository, "DXB", "AP01", "Vendor"))
                .isSameAs(existing);
    }

    @Test
    void findOrCreateMaster_shouldCreateWhenMissing() {
        APAccount created = apMaster("AP02", "DXB");
        when(apAccountRepository.findByAccountNumberAndEntityCode("AP02", "DXB")).thenReturn(Optional.empty());
        when(apAccountRepository.save(any(APAccount.class))).thenReturn(created);

        APAccount result = ApAccountMasterSupport.findOrCreateMaster(apAccountRepository, " DXB ", " AP02 ", " Vendor ");

        assertThat(result).isSameAs(created);
        verify(apAccountRepository).save(any(APAccount.class));
    }

    @Test
    void syncMasterIdentity_shouldUpdateNameWhenPresent() {
        APAccount master = apMaster("AP01", "DXB");
        ApAccountMasterSupport.syncMasterIdentity(master, " Updated Name ");
        assertThat(master.getAccountName()).isEqualTo("Updated Name");
    }

    @Test
    void isApChargeConfig_shouldDetectApConfig() {
        Account config = new Account();
        config.setAccountNature(AccountNature.AP);
        config.setApAccount(apMaster("AP01", "DXB"));

        assertThat(ApAccountMasterSupport.isApChargeConfig(config)).isTrue();
        assertThat(ApAccountMasterSupport.isApChargeConfig(new Account())).isFalse();
    }

    @Test
    void masterAccountNumber_shouldReturnApMasterNumber() {
        Account config = new Account();
        config.setAccountNature(AccountNature.AP);
        config.setApAccount(apMaster("AP01", "DXB"));

        assertThat(ApAccountMasterSupport.masterAccountNumber(config)).isEqualTo("AP01");
        assertThat(ApAccountMasterSupport.masterAccountNumber(new Account())).isNull();
    }

    @Test
    void masterEntityCode_shouldPreferApMasterEntity() {
        APAccount master = apMaster("AP01", "DXB");
        Account config = new Account();
        config.setAccountNature(AccountNature.AP);
        config.setApAccount(master);
        Entity fallbackEntity = new Entity();
        fallbackEntity.setCode("FALLBACK");
        config.setEntity(fallbackEntity);

        assertThat(ApAccountMasterSupport.masterEntityCode(config)).isEqualTo("DXB");
        assertThat(ApAccountMasterSupport.masterEntityCode(new Account())).isNull();
    }

    private static APAccount apMaster(String accountNumber, String entityCode) {
        APAccount master = new APAccount();
        ReflectionTestUtils.setField(master, "id", UUID.randomUUID());
        master.setAccountNumber(accountNumber);
        master.setEntityCode(entityCode);
        master.setAccountName("Vendor");
        return master;
    }
}
