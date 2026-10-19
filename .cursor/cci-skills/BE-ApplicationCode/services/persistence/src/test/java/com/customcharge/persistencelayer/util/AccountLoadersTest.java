package com.customcharge.persistencelayer.util;

import com.customcharge.persistencelayer.entities.Account;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AccountLoadersTest {

    @Test
    void selectFirstAccount_shouldReturnEmpty_whenNullOrEmpty() {
        assertThat(AccountLoaders.selectFirstAccount(null)).isEmpty();
        assertThat(AccountLoaders.selectFirstAccount(List.of())).isEmpty();
    }

    @Test
    void selectFirstAccount_shouldReturnFirstOnly_whenMultiple() {
        Account first = account("first");
        Account second = account("second");

        assertThat(AccountLoaders.selectFirstAccount(List.of(first, second))).containsExactly(first);
    }

    @Test
    void selectFirstAccount_shouldReturnSingle_whenOne() {
        Account only = account("only");
        assertThat(AccountLoaders.selectFirstAccount(List.of(only))).containsExactly(only);
    }

    private static Account account(String suffix) {
        Account account = new Account();
        org.springframework.test.util.ReflectionTestUtils.setField(account, "id", UUID.randomUUID());
        account.setChargeType(suffix);
        return account;
    }
}
