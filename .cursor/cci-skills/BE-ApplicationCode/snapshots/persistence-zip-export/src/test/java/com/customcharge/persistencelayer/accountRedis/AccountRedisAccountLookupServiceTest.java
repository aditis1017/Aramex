package com.customcharge.persistencelayer.accountRedis;

import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.enums.AccountNature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountRedisAccountLookupServiceTest {

    @Mock
    private ObjectProvider<AccountRedisStringCache> accountRedisStringCache;
    @Mock
    private ObjectProvider<AccountRedisReadListener> accountRedisReadListener;
    @Mock
    private AccountRedisStringCache cache;

    private AccountRedisAccountLookupService service;

    @BeforeEach
    void setUp() {
        when(accountRedisReadListener.getIfAvailable()).thenReturn(null);
        service = new AccountRedisAccountLookupService(
                accountRedisStringCache, accountRedisReadListener, new ObjectMapper());
    }

    @Test
    void tryResolveEntitySettings_shouldReturnAccount_whenChargeConfigPresentInRedis() throws Exception {
        UUID accountId = UUID.randomUUID();
        ArAccountRedisChargeTypeData chargeData = new ArAccountRedisChargeTypeData();
        chargeData.setAccountId(accountId);
        chargeData.setDefaultCashAccount("CASH-100");
        ArAccountRedisPayload payload = entityPayload("RUH", "DTC", chargeData);
        when(accountRedisStringCache.getIfAvailable()).thenReturn(cache);
        when(cache.get("acc:RUH")).thenReturn(Optional.of(new ObjectMapper().writeValueAsString(payload)));

        Optional<Account> result = service.tryResolveEntitySettings("RUH", "DTC");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(accountId);
        assertThat(result.get().getAccountNature()).isEqualTo(AccountNature.ENTITY_SETTINGS);
        assertThat(result.get().getChargeType()).isEqualTo("DTC");
        assertThat(result.get().getEntityCode()).isEqualTo("RUH");
        assertThat(result.get().getDefaultCashAccount()).isEqualTo("CASH-100");
        assertThat(result.get().getVersion()).isZero();
    }

    @Test
    void tryResolveArChargeAccount_shouldReturnAccount_whenChargeConfigPresentInRedis() throws Exception {
        UUID accountId = UUID.randomUUID();
        UUID arMasterId = UUID.randomUUID();
        ArAccountRedisChargeTypeData chargeData = new ArAccountRedisChargeTypeData();
        chargeData.setAccountId(accountId);
        ArAccountRedisPayload payload = arPayload("238296", "RUH", arMasterId, "DTC", chargeData);
        when(accountRedisStringCache.getIfAvailable()).thenReturn(cache);
        when(cache.get("acc:238296:RUH")).thenReturn(Optional.of(new ObjectMapper().writeValueAsString(payload)));

        Optional<Account> result = service.tryResolveArChargeAccount("238296", "RUH", "DTC");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(accountId);
        assertThat(result.get().getAccountNature()).isEqualTo(AccountNature.AR);
        assertThat(result.get().getChargeType()).isEqualTo("DTC");
        assertThat(result.get().getAccountNumber()).isEqualTo("238296");
        assertThat(result.get().getArAccount()).isNotNull();
        assertThat(result.get().getArAccount().getId()).isEqualTo(arMasterId);
        assertThat(result.get().getVersion()).isZero();
    }

    @Test
    void tryResolveEntitySettings_shouldReturnEmpty_whenRedisMisses() {
        when(accountRedisStringCache.getIfAvailable()).thenReturn(cache);
        when(cache.get("acc:RUH")).thenReturn(Optional.empty());

        assertThat(service.tryResolveEntitySettings("RUH", "DTC")).isEmpty();
        verify(cache).get("acc:RUH");
    }

    @Test
    void tryResolveEntitySettings_shouldSkipRedis_whenListenerRequestsSkip() {
        AccountRedisReadListener listener = org.mockito.Mockito.mock(AccountRedisReadListener.class);
        when(accountRedisReadListener.getIfAvailable()).thenReturn(listener);
        when(listener.skipReads()).thenReturn(true);

        assertThat(service.tryResolveEntitySettings("RUH", "DTC")).isEmpty();
        verify(cache, never()).get("acc:RUH");
    }

    @Test
    void tryResolveEntitySettings_shouldReturnEmpty_whenCacheUnavailable() {
        when(accountRedisStringCache.getIfAvailable()).thenReturn(null);

        assertThat(service.tryResolveEntitySettings("RUH", "DTC")).isEmpty();
        verify(cache, never()).get("acc:RUH");
    }

    private static ArAccountRedisPayload entityPayload(
            String entityCode, String chargeType, ArAccountRedisChargeTypeData chargeData) {
        ArAccountRedisPayload payload = new ArAccountRedisPayload();
        payload.setEntity(entityCode);
        Map<String, ArAccountRedisChargeTypeData> charges = new LinkedHashMap<>();
        charges.put(chargeType, chargeData);
        payload.setCharges(charges);
        return payload;
    }

    private static ArAccountRedisPayload arPayload(
            String accountNumber,
            String entityCode,
            UUID arMasterId,
            String chargeType,
            ArAccountRedisChargeTypeData chargeData) {
        ArAccountRedisPayload payload = new ArAccountRedisPayload();
        payload.setId(arMasterId);
        payload.setAccountNo(accountNumber);
        payload.setEntity(entityCode);
        payload.setAccountName("Test AR");
        Map<String, ArAccountRedisChargeTypeData> charges = new LinkedHashMap<>();
        charges.put(chargeType, chargeData);
        payload.setCharges(charges);
        return payload;
    }
}
