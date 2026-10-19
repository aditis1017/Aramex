package com.customcharge.persistencelayer.util;

import com.customcharge.persistencelayer.accountRedis.AccountRedisChargeDataService;
import com.customcharge.persistencelayer.dto.ServiceChargeQueryCriteria;
import com.customcharge.persistencelayer.dto.ServiceChargeResponse;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.ServiceCharge;
import com.customcharge.persistencelayer.entities.ServiceCodes;
import com.customcharge.persistencelayer.enums.AccountNature;
import com.customcharge.persistencelayer.repository.AccountRepository;
import com.customcharge.persistencelayer.repository.ServiceChargeRepository;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntitySettingsServiceChargeFilterTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private ServiceChargeRepository serviceChargeRepository;
    @Mock
    private ObjectProvider<AccountRedisChargeDataService> accountRedisChargeDataServiceProvider;
    @Mock
    private AccountRedisChargeDataService redisChargeDataService;

    private EntitySettingsServiceChargeFilter filter;

    @BeforeEach
    void setUp() {
        filter = new EntitySettingsServiceChargeFilter(
                accountRepository, serviceChargeRepository, accountRedisChargeDataServiceProvider);
        lenient().when(accountRedisChargeDataServiceProvider.getIfAvailable()).thenReturn(null);
    }

    @Test
    void filter_shouldReturnUnchanged_forNonArAccount() {
        Account entitySettings = new Account();
        entitySettings.setAccountNature(AccountNature.ENTITY_SETTINGS);

        List<ServiceChargeResponse> responses = List.of(response("DUTY"));
        List<ServiceChargeResponse> filtered = filter.filterArResponsesByEntitySettings(
                ServiceChargeQueryCriteria.builder()
                        .entityCode("RUH")
                        .account(entitySettings)
                        .chargeTypes(List.of("DTC"))
                        .build(),
                responses);

        assertThat(filtered).containsExactlyElementsOf(responses);
    }

    @Test
    void filter_shouldRemoveCode_whenEntitySettingsChargeInactive_fromDb() {
        Account arAccount = arAccount("DTC");
        Account entitySettings = entitySettingsAccount("DTC");
        ServiceCodes master = master("DUTY", "DTC");
        ServiceCharge inactiveOnEntity = charge(master, false);

        when(accountRepository.findByAccountNumberIsNullAndEntityCodeAndAccountNatureAndChargeType(
                eq("RUH"), eq(AccountNature.ENTITY_SETTINGS), eq("DTC")))
                .thenReturn(Optional.of(entitySettings));
        when(serviceChargeRepository.findByAccountWithActiveServiceCodes(entitySettings, "RUH", "DTC"))
                .thenReturn(List.of(inactiveOnEntity));

        List<ServiceChargeResponse> filtered = filter.filterArResponsesByEntitySettings(
                ServiceChargeQueryCriteria.builder()
                        .entityCode("RUH")
                        .account(arAccount)
                        .chargeTypes(List.of("DTC"))
                        .build(),
                List.of(response("DUTY"), response("OF")));

        assertThat(filtered).extracting(ServiceChargeResponse::getServiceCode).containsExactly("OF");
    }

    @Test
    void filter_shouldKeepCode_whenEntitySettingsHasNoRow() {
        Account arAccount = arAccount("DTC");
        Account entitySettings = entitySettingsAccount("DTC");

        when(accountRepository.findByAccountNumberIsNullAndEntityCodeAndAccountNatureAndChargeType(
                eq("RUH"), eq(AccountNature.ENTITY_SETTINGS), eq("DTC")))
                .thenReturn(Optional.of(entitySettings));
        when(serviceChargeRepository.findByAccountWithActiveServiceCodes(entitySettings, "RUH", "DTC"))
                .thenReturn(List.of());

        List<ServiceChargeResponse> filtered = filter.filterArResponsesByEntitySettings(
                ServiceChargeQueryCriteria.builder()
                        .entityCode("RUH")
                        .account(arAccount)
                        .chargeTypes(List.of("DTC"))
                        .build(),
                List.of(response("DUTY")));

        assertThat(filtered).hasSize(1);
    }

    @Test
    void filter_shouldRemoveCode_whenEntitySettingsChargeInactive_fromRedis() {
        when(accountRedisChargeDataServiceProvider.getIfAvailable()).thenReturn(redisChargeDataService);

        Account arAccount = arAccount("DTC");
        Account entitySettings = entitySettingsAccount("DTC");
        ServiceCodes master = master("DUTY", "DTC");
        ServiceCharge inactiveOnEntity = charge(master, false);
        Map<UUID, ServiceCharge> entityCharges = new LinkedHashMap<>();
        entityCharges.put(master.getId(), inactiveOnEntity);

        when(accountRepository.findByAccountNumberIsNullAndEntityCodeAndAccountNatureAndChargeType(
                eq("RUH"), eq(AccountNature.ENTITY_SETTINGS), eq("DTC")))
                .thenReturn(Optional.of(entitySettings));
        when(redisChargeDataService.loadServiceChargesByMasterId(entitySettings, "RUH", "DTC"))
                .thenReturn(entityCharges);

        List<ServiceChargeResponse> filtered = filter.filterArResponsesByEntitySettings(
                ServiceChargeQueryCriteria.builder()
                        .entityCode("RUH")
                        .account(arAccount)
                        .chargeTypes(List.of("DTC"))
                        .build(),
                List.of(response("DUTY")));

        assertThat(filtered).isEmpty();
    }

    private static Account arAccount(String chargeType) {
        Account account = new Account();
        ReflectionTestUtils.setField(account, "id", UUID.randomUUID());
        account.setAccountNature(AccountNature.AR);
        account.setChargeType(chargeType);
        return account;
    }

    private static Account entitySettingsAccount(String chargeType) {
        Account account = new Account();
        ReflectionTestUtils.setField(account, "id", UUID.randomUUID());
        account.setAccountNature(AccountNature.ENTITY_SETTINGS);
        account.setChargeType(chargeType);
        return account;
    }

    private static ServiceCodes master(String serviceCode, String chargeType) {
        ServiceCodes master = new ServiceCodes();
        ReflectionTestUtils.setField(master, "id", UUID.randomUUID());
        master.setServiceCode(serviceCode);
        master.setChargeType(chargeType);
        return master;
    }

    private static ServiceCharge charge(ServiceCodes master, boolean active) {
        ServiceCharge charge = new ServiceCharge();
        charge.setServiceCodes(master);
        charge.setActive(active);
        return charge;
    }

    private static ServiceChargeResponse response(String serviceCode) {
        return new ServiceChargeResponse(
                null,
                serviceCode,
                null,
                null,
                null,
                true,
                null,
                null,
                true,
                "",
                true,
                "1",
                "",
                "",
                null,
                null);
    }
}
