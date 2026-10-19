package com.customcharge.persistencelayer.accountRedis;

import com.customcharge.persistencelayer.enums.AccountNature;

/**
 * Parsed identity from a Redis account key.
 * Keys omit nature, for example {@code acc:accountNumber:entityCode} or legacy {@code acc:entityCode:chargeType}.
 */
public record AccountRedisIdentity(
        String accountNumber, String entityCode, AccountNature nature, String chargeType) {}
