package com.customcharge.persistencelayer.accountRedis;

import com.customcharge.persistencelayer.enums.AccountNature;

import java.util.Objects;
import java.util.Optional;

/**
 * Redis account cache keys (nature is not part of the key):
 * <ul>
 *   <li>AP/AR: {@code acc:accountNumber:entityCode}</li>
 *   <li>Entity settings: {@code acc:entityCode}</li>
 * </ul>
 */
public final class AccountRedisKeySupport {

    public static final String KEY_PREFIX = "acc:";

    private AccountRedisKeySupport() {}

    public static String redisKeyForAccount(String accountNumber, String entityCode) {
        if (Objects.isNull(accountNumber) || accountNumber.isBlank()) {
            throw new IllegalArgumentException("accountNumber must not be blank");
        }
        if (Objects.isNull(entityCode) || entityCode.isBlank()) {
            throw new IllegalArgumentException("entityCode must not be blank");
        }
        return KEY_PREFIX + AccountBusinessKeySupport.composeWithAccountNumber(accountNumber, entityCode);
    }

    public static String redisKeyForEntity(String entityCode) {
        if (Objects.isNull(entityCode) || entityCode.isBlank()) {
            throw new IllegalArgumentException("entityCode must not be blank");
        }
        return KEY_PREFIX + AccountRedisKeySupport.escapeSegment(entityCode.trim());
    }

    /** @deprecated Use {@link #redisKeyForEntity(String)}; charge type lives in the value payload. */
    @Deprecated
    public static String redisKeyForEntitySettings(String entityCode, String chargeType) {
        return redisKeyForEntity(entityCode);
    }

    public static String redisKeyFor(
            String accountNumber, String entityCode, AccountNature nature, String chargeType) {
        if (AccountNature.ENTITY_SETTINGS.equals(nature)) {
            return redisKeyForEntity(entityCode);
        }
        return redisKeyForAccount(accountNumber, entityCode);
    }

    public static String escapeSegment(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().replace(":", "_");
    }

    public static Optional<AccountRedisIdentity> parseRedisKey(String redisKey) {
        if (Objects.isNull(redisKey) || redisKey.isBlank()) {
            return Optional.empty();
        }
        if (!redisKey.startsWith(KEY_PREFIX)) {
            return Optional.empty();
        }
        String rest = redisKey.substring(KEY_PREFIX.length());
        return AccountBusinessKeySupport.parse(rest);
    }
}
