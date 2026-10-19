package com.customcharge.persistencelayer.accountRedis;

import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.enums.AccountNature;
import com.customcharge.persistencelayer.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Builds aggregated Redis payload for entity-settings (Redis key {@code acc:entityCode}).
 */
@Component
@RequiredArgsConstructor
public class EntitySettingsRedisCacheBuilder {

    private final AccountRepository accountRepository;
    private final ArAccountRedisCacheBuilder arAccountRedisCacheBuilder;

    public ArAccountRedisPayload build(String entityCode) {
        ArAccountRedisPayload payload = new ArAccountRedisPayload();
        payload.setEntity(entityCode);
        List<Account> configs =
                accountRepository.findByEntityCodeAndAccountNature(entityCode, AccountNature.ENTITY_SETTINGS);
        if (!configs.isEmpty()) {
            Account first = configs.getFirst();
            payload.setCountry(first.getCountryCode());
        }
        payload.setCharges(arAccountRedisCacheBuilder.buildChargesMap(null, configs));
        return payload;
    }
}
