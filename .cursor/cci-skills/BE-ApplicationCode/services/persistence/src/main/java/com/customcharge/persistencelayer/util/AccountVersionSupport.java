package com.customcharge.persistencelayer.util;

import com.customcharge.persistencelayer.entities.Account;

public final class AccountVersionSupport {

    private AccountVersionSupport() {
    }

    public static void ensureInitialized(Account account) {
        if (account != null && account.getVersion() == null) {
            account.setVersion(0L);
        }
    }
}
