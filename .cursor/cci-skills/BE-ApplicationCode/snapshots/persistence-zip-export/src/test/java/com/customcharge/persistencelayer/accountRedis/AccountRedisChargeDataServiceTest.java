package com.customcharge.persistencelayer.accountRedis;

import com.customcharge.persistencelayer.dto.ServiceChargeQueryCriteria;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.enums.AccountNature;
import com.customcharge.persistencelayer.repository.ServiceChargeRepository;
import com.customcharge.persistencelayer.repository.ServiceCodesRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountRedisChargeDataServiceTest {

    @Mock
    private ObjectProvider<AccountRedisStringCache> accountRedisStringCache;
    @Mock
    private ObjectProvider<AccountRedisReadListener> accountRedisReadListener;
    @Mock
    private AccountRedisStringCache cache;
    @Mock
    private ServiceCodesRepository serviceCodesRepository;
    @Mock
    private ServiceChargeRepository serviceChargeRepository;

    @Test
    void getServiceCharges_shouldFallbackToDb_whenChargeDataNotSynced() {
        Account account = entitySettingsAccount("US01", "DTA");
        when(accountRedisStringCache.getIfAvailable()).thenReturn(cache);
        when(cache.get("acc:US01")).thenReturn(Optional.of("{\"charges\":{\"DTA\":{\"chargeDataSynced\":false}}}"));
        when(serviceCodesRepository.findByEntityCodeAndChargeTypeAndIsDeletedFalse(eq("US01"), eq("DTA")))
                .thenReturn(List.of());

        AccountRedisChargeDataService service = newService();

        assertThat(service.getServiceCharges(account, "US01", "DTA")).isEmpty();
        verify(serviceCodesRepository).findByEntityCodeAndChargeTypeAndIsDeletedFalse("US01", "DTA");
    }

    @Test
    void tryResolveFromRedis_shouldReturnMastersWithoutDb_whenChargeDataSynced() throws Exception {
        Account account = entitySettingsAccount("RUH", "ECD");
        ArAccountRedisServiceCharge redisCharge = new ArAccountRedisServiceCharge();
        redisCharge.setServiceCodeMasterId(UUID.randomUUID());
        redisCharge.setServiceCode("Cost");
        redisCharge.setServiceDescription("Admin Fee");
        redisCharge.setType("D");
        redisCharge.setMasterActive(true);
        redisCharge.setActive(true);
        redisCharge.setIsBillable(true);
        redisCharge.setSplit("1");
        redisCharge.setDefaultAccountNumber("");

        ArAccountRedisPayload payload = syncedPayloadWithCharges("ECD", List.of(redisCharge));
        when(accountRedisStringCache.getIfAvailable()).thenReturn(cache);
        when(cache.get("acc:RUH")).thenReturn(Optional.of(new ObjectMapper().writeValueAsString(payload)));

        AccountRedisChargeDataService service = newService();

        Optional<List<com.customcharge.persistencelayer.dto.ServiceChargeResponse>> result =
                service.tryResolveFromRedis(ServiceChargeQueryCriteria.builder()
                        .entityCode("RUH")
                        .account(account)
                        .chargeTypes(List.of("ECD"))
                        .active(true)
                        .build());

        assertThat(result).isPresent();
        assertThat(result.get()).hasSize(1);
        assertThat(result.get().getFirst().getServiceCode()).isEqualTo("Cost");
        verify(serviceCodesRepository, never()).findActiveMastersByEntityCodeAndChargeTypes(any(), any());
        verify(serviceChargeRepository, never()).findByAccountAndServiceCodesIn(any(), any());
    }

    private AccountRedisChargeDataService newService() {
        when(accountRedisReadListener.getIfAvailable()).thenReturn(null);
        return new AccountRedisChargeDataService(
                accountRedisStringCache,
                accountRedisReadListener,
                new ObjectMapper(),
                null,
                null,
                serviceCodesRepository,
                serviceChargeRepository);
    }

    private static Account entitySettingsAccount(String entityCode, String chargeType) {
        Account account = new Account();
        ReflectionTestUtils.setField(account, "id", UUID.randomUUID());
        account.setAccountNature(AccountNature.ENTITY_SETTINGS);
        com.customcharge.persistencelayer.entities.Entity entity =
                new com.customcharge.persistencelayer.entities.Entity();
        entity.setCode(entityCode);
        ReflectionTestUtils.setField(account, "entity", entity);
        account.setChargeType(chargeType);
        return account;
    }

    private static ArAccountRedisPayload syncedPayloadWithCharges(List<ArAccountRedisServiceCharge> serviceCharges) {
        return syncedPayloadWithCharges("DTA", serviceCharges);
    }

    private static ArAccountRedisPayload syncedPayloadWithCharges(
            String chargeType, List<ArAccountRedisServiceCharge> serviceCharges) {
        ArAccountRedisChargeTypeData chargeData = new ArAccountRedisChargeTypeData();
        chargeData.setChargeDataSynced(true);
        chargeData.setServiceCharges(serviceCharges);
        chargeData.setRateSheetItems(List.of());
        Map<String, ArAccountRedisChargeTypeData> charges = new LinkedHashMap<>();
        charges.put(chargeType, chargeData);
        ArAccountRedisPayload payload = new ArAccountRedisPayload();
        payload.setCharges(charges);
        return payload;
    }
}
