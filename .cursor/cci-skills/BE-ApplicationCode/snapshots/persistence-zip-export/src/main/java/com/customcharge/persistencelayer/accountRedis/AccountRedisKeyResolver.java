package com.customcharge.persistencelayer.accountRedis;

import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.enums.AccountNature;
import org.springframework.util.StringUtils;

final class AccountRedisKeyResolver {

    private AccountRedisKeyResolver() {}

    static String redisKeyFor(Account account) {
        if (account == null) {
            return null;
        }
        if (AccountNature.ENTITY_SETTINGS.equals(account.getAccountNature())) {
            return StringUtils.hasText(account.getEntityCode())
                    ? AccountRedisKeySupport.redisKeyForEntity(account.getEntityCode().trim())
                    : null;
        }
        String accountNumber = account.getAccountNumber();
        String entityCode = account.getEntityCode();
        if (!StringUtils.hasText(accountNumber) || !StringUtils.hasText(entityCode)) {
            return null;
        }
        return AccountRedisKeySupport.redisKeyForAccount(accountNumber.trim(), entityCode.trim());
    }

    static String redisKeyForEntitySettings(String entityCode) {
        if (!StringUtils.hasText(entityCode)) {
            return null;
        }
        return AccountRedisKeySupport.redisKeyForEntity(entityCode.trim());
    }
}
