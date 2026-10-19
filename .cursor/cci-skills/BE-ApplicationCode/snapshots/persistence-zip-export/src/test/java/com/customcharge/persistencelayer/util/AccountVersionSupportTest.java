package com.customcharge.persistencelayer.util;

import com.customcharge.persistencelayer.entities.Account;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccountVersionSupportTest {

    @Test
    void ensureInitialized_shouldSetZeroWhenVersionNull() {
        Account account = new Account();
        AccountVersionSupport.ensureInitialized(account);
        assertThat(account.getVersion()).isEqualTo(0L);
    }

    @Test
    void ensureInitialized_shouldLeaveExistingVersion() {
        Account account = new Account();
        account.setVersion(3L);
        AccountVersionSupport.ensureInitialized(account);
        assertThat(account.getVersion()).isEqualTo(3L);
    }

    @Test
    void ensureInitialized_shouldIgnoreNullAccount() {
        AccountVersionSupport.ensureInitialized(null);
    }
}
