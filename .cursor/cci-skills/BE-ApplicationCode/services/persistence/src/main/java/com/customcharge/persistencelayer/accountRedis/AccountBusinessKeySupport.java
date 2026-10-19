package com.customcharge.persistencelayer.accountRedis;

import com.customcharge.persistencelayer.enums.AccountNature;

import java.util.Objects;
import java.util.Optional;

/**
 * Composes and parses Redis key bodies after {@value AccountRedisKeySupport#KEY_PREFIX}.
 * Nature is not part of the key; it is inferred when parsing (account number present vs absent).
 */
public final class AccountBusinessKeySupport {

    private AccountBusinessKeySupport() {}

    /**
     * Redis key body {@code accountNumber:entityCode} (after the {@code acc:} prefix).
     *
     * @param accountNumber business account number
     * @param entityCode entity code
     * @return key body without prefix
     */
    public static String composeWithAccountNumber(String accountNumber, String entityCode) {
        if (Objects.isNull(accountNumber) || accountNumber.isBlank()) {
            throw new IllegalArgumentException("accountNumber must not be blank");
        }
        if (Objects.isNull(entityCode) || entityCode.isBlank()) {
            throw new IllegalArgumentException("entityCode must not be blank");
        }
        return AccountRedisKeySupport.escapeSegment(accountNumber.trim())
                + ":"
                + AccountRedisKeySupport.escapeSegment(entityCode.trim());
    }

    /**
     * Legacy key body {@code entityCode:chargeType}; prefer {@link AccountRedisKeySupport#redisKeyForEntity}.
     *
     * @param entityCode entity code
     * @param chargeType charge type (may be blank)
     * @return key body without prefix
     */
    public static String composeEntitySettings(String entityCode, String chargeType) {
        if (Objects.isNull(entityCode) || entityCode.isBlank()) {
            throw new IllegalArgumentException("entityCode must not be blank");
        }
        String chargeSegment =
                AccountRedisKeySupport.escapeSegment(
                        chargeType == null || chargeType.isBlank() ? "" : chargeType.trim());
        return AccountRedisKeySupport.escapeSegment(entityCode.trim()) + ":" + chargeSegment;
    }

    /**
     * @deprecated Use {@link #composeWithAccountNumber} or {@link #composeEntitySettings}; nature is not stored in keys.
     */
    @Deprecated
    public static String compose(
            String accountNumber, String entityCode, AccountNature nature, String chargeType) {
        if (AccountNature.ENTITY_SETTINGS.equals(nature)) {
            return composeEntitySettings(entityCode, chargeType);
        }
        return composeWithAccountNumber(accountNumber, entityCode);
    }

    /**
     * @param keyBody Redis key body without the {@code acc:} prefix
     * @return parsed identity, or empty if the format is not recognized
     */
    public static Optional<AccountRedisIdentity> parse(String keyBody) {
        if (Objects.isNull(keyBody) || keyBody.isBlank()) {
            return Optional.empty();
        }
        String[] parts = keyBody.split(":", -1);
        if (parts.length == 1) {
            return Optional.of(new AccountRedisIdentity(
                    "", unescapeSegment(parts[0]), AccountNature.ENTITY_SETTINGS, null));
        }
        if (parts.length == 2) {
            String first = unescapeSegment(parts[0]);
            String second = unescapeSegment(parts[1]);
            if (second.isBlank()) {
                return Optional.of(new AccountRedisIdentity("", first, AccountNature.ENTITY_SETTINGS, null));
            }
            return Optional.of(new AccountRedisIdentity(first, second, null, null));
        }
        if (parts.length == 3) {
            String first = unescapeSegment(parts[0]);
            String second = unescapeSegment(parts[1]);
            String third = blankToNull(unescapeSegment(parts[2]));
            if (isLegacyNature(second)) {
                AccountNature nature = AccountNature.valueOf(second);
                return Optional.of(new AccountRedisIdentity(first, unescapeSegment(parts[1]), nature, third));
            }
            return Optional.of(
                    new AccountRedisIdentity("", first, AccountNature.ENTITY_SETTINGS, blankToNull(second)));
        }
        if (parts.length == 4 && isLegacyNature(parts[2])) {
            AccountNature nature = AccountNature.valueOf(parts[2]);
            return Optional.of(new AccountRedisIdentity(
                    unescapeSegment(parts[0]),
                    unescapeSegment(parts[1]),
                    nature,
                    blankToNull(unescapeSegment(parts[3]))));
        }
        return Optional.empty();
    }

    private static boolean isLegacyNature(String segment) {
        try {
            AccountNature n = AccountNature.valueOf(segment);
            return n == AccountNature.AP || n == AccountNature.AR || n == AccountNature.ENTITY_SETTINGS;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private static String unescapeSegment(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace('_', ':');
    }
}
