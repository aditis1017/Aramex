package com.customcharge.persistencelayer.accountRedis;

import com.customcharge.persistencelayer.entities.Account;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Optional;

/**
 * Resolves {@link Account} charge-config rows from Redis account payloads.
 * Returns empty when Redis cannot satisfy the lookup (caller should use the database).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountRedisAccountLookupService {

    private final ObjectProvider<AccountRedisStringCache> accountRedisStringCache;
    private final ObjectProvider<AccountRedisReadListener> accountRedisReadListener;
    private final ObjectMapper objectMapper;

    public Optional<Account> tryResolveEntitySettings(String entityCode, String chargeType) {
        if (!StringUtils.hasText(entityCode) || !StringUtils.hasText(chargeType)) {
            return Optional.empty();
        }
        String redisKey = AccountRedisKeyResolver.redisKeyForEntitySettings(entityCode.strip());
        return readPayload(redisKey)
                .flatMap(payload -> chargeTypeData(payload, chargeType)
                        .map(chargeData -> AccountRedisAccountMapper.toEntitySettingsAccount(
                                entityCode.strip(), chargeType, chargeData)));
    }

    public Optional<Account> tryResolveArChargeAccount(
            String accountNumber, String entityCode, String chargeType) {
        if (!StringUtils.hasText(accountNumber) || !StringUtils.hasText(entityCode) || !StringUtils.hasText(chargeType)) {
            return Optional.empty();
        }
        String redisKey = AccountRedisKeySupport.redisKeyForAccount(accountNumber.strip(), entityCode.strip());
        return readPayload(redisKey)
                .flatMap(payload -> chargeTypeData(payload, chargeType)
                        .map(chargeData -> AccountRedisAccountMapper.toArChargeAccount(
                                payload, chargeType, chargeData)));
    }

    private Optional<ArAccountRedisChargeTypeData> chargeTypeData(
            ArAccountRedisPayload payload, String chargeType) {
        return resolveChargeEntry(payload.getCharges(), chargeType)
                .filter(chargeData -> chargeData.getAccountId() != null);
    }

    private Optional<ArAccountRedisPayload> readPayload(String redisKey) {
        AccountRedisReadListener listener = accountRedisReadListener.getIfAvailable();
        if (listener != null && listener.skipReads()) {
            return Optional.empty();
        }
        AccountRedisStringCache cache = accountRedisStringCache.getIfAvailable();
        if (cache == null || !StringUtils.hasText(redisKey)) {
            return Optional.empty();
        }
        try {
            Optional<String> json = cache.get(redisKey);
            if (json.isEmpty()) {
                if (log.isDebugEnabled()) {
                    log.debug("Account Redis cache miss for key {}", redisKey);
                }
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json.get(), ArAccountRedisPayload.class));
        } catch (Exception ex) {
            if (listener != null) {
                listener.onReadFailure(redisKey, ex);
            }
            log.warn(
                    "Failed to read account Redis payload for key {} ({}): {}",
                    redisKey,
                    ex.getClass().getSimpleName(),
                    ex.getMessage());
            return Optional.empty();
        }
    }

    private static Optional<ArAccountRedisChargeTypeData> resolveChargeEntry(
            Map<String, ArAccountRedisChargeTypeData> charges, String chargeType) {
        if (charges == null || !StringUtils.hasText(chargeType)) {
            return Optional.empty();
        }
        String chargeKey = AccountRedisChargeDataPopulator.chargeTypeKey(chargeType);
        ArAccountRedisChargeTypeData exact = charges.get(chargeKey);
        if (exact != null) {
            return Optional.of(exact);
        }
        for (Map.Entry<String, ArAccountRedisChargeTypeData> entry : charges.entrySet()) {
            if (chargeKey.equalsIgnoreCase(entry.getKey())) {
                return Optional.of(entry.getValue());
            }
        }
        return Optional.empty();
    }
}
