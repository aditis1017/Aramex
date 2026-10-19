package com.customcharge.persistencelayer.util;

import com.customcharge.persistencelayer.dto.ServiceChargeQueryCriteria;
import com.customcharge.persistencelayer.dto.ServiceChargeResponse;
import com.customcharge.persistencelayer.entities.ARAccount;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.ServiceCharge;
import com.customcharge.persistencelayer.entities.ServiceCodes;
import com.customcharge.persistencelayer.enums.AccountNature;
import com.customcharge.persistencelayer.repository.AccountRepository;
import com.customcharge.persistencelayer.repository.ServiceChargeRepository;
import com.customcharge.persistencelayer.repository.ServiceCodesRepository;
import com.customcharge.persistencelayer.accountRedis.AccountRedisChargeDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceChargeResolverTest {

    @Mock
    private ServiceCodesRepository serviceCodesRepository;
    @Mock
    private ServiceChargeRepository serviceChargeRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private ObjectProvider<AccountRedisChargeDataService> accountRedisChargeDataService;

    private ServiceChargeResolver resolver;
    private Account account;
    private ServiceCodes master;

    @BeforeEach
    void setUp() {
        EntitySettingsServiceChargeFilter entitySettingsFilter = new EntitySettingsServiceChargeFilter(
                accountRepository, serviceChargeRepository, accountRedisChargeDataService);
        resolver = new ServiceChargeResolver(
                serviceCodesRepository,
                serviceChargeRepository,
                accountRedisChargeDataService,
                entitySettingsFilter);
        lenient().when(accountRedisChargeDataService.getIfAvailable()).thenReturn(null);
        account = new Account();
        ReflectionTestUtils.setField(account, "id", UUID.randomUUID());

        master = new ServiceCodes();
        ReflectionTestUtils.setField(master, "id", UUID.randomUUID());
        master.setServiceCode("SC01");
        master.setServiceDescription("Service One");
        master.setChargeType("Estimate");
        master.setIsDeleted(false);
    }

    @Test
    void resolve_shouldReturnSyntheticDefault_whenMasterExistsWithoutAccountCharge() {
        when(serviceCodesRepository.findActiveMastersByEntityCodeAndChargeTypes(eq("DXB"), any()))
                .thenReturn(List.of(master));
        when(serviceChargeRepository.findByAccountAndServiceCodesIn(eq(account), any()))
                .thenReturn(List.of());

        List<ServiceChargeResponse> result = resolver.resolve(
                ServiceChargeQueryCriteria.builder()
                        .entityCode("DXB")
                        .account(account)
                        .active(true)
                        .build());

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getServiceCode()).isEqualTo("SC01");
        assertThat(result.getFirst().getActive()).isTrue();
        assertThat(result.getFirst().getId()).isNull();
        assertThat(result.getFirst().getInfoAxsId()).isNull();
        assertThat(result.getFirst().getFetchTax()).isNull();
    }

    @Test
    void resolve_shouldExcludeServiceCode_whenAccountChargeInactive() {
        ServiceCharge inactiveCharge = accountCharge(master, false);

        when(serviceCodesRepository.findActiveMastersByEntityCodeAndChargeTypes(eq("DXB"), any()))
                .thenReturn(List.of(master));
        when(serviceChargeRepository.findByAccountAndServiceCodesIn(eq(account), any()))
                .thenReturn(List.of(inactiveCharge));

        List<ServiceChargeResponse> result = resolver.resolve(
                ServiceChargeQueryCriteria.builder()
                        .entityCode("DXB")
                        .account(account)
                        .active(true)
                        .build());

        assertThat(result).isEmpty();
    }

    @Test
    void resolve_shouldReturnAccountCharge_whenActive() {
        ServiceCharge activeCharge = accountCharge(master, true);
        UUID chargeId = UUID.randomUUID();
        ReflectionTestUtils.setField(activeCharge, "id", chargeId);

        when(serviceCodesRepository.findActiveMastersByEntityCodeAndChargeTypes(eq("DXB"), any()))
                .thenReturn(List.of(master));
        when(serviceChargeRepository.findByAccountAndServiceCodesIn(eq(account), any()))
                .thenReturn(List.of(activeCharge));

        List<ServiceChargeResponse> result = resolver.resolve(
                ServiceChargeQueryCriteria.builder()
                        .entityCode("DXB")
                        .account(account)
                        .active(true)
                        .build());

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(chargeId);
        assertThat(result.getFirst().getServiceCode()).isEqualTo("SC01");
    }

    @Test
    void resolve_shouldReturnEmpty_whenCriteriaInvalid() {
        assertThat(resolver.resolve(null)).isEmpty();
        assertThat(resolver.resolve(ServiceChargeQueryCriteria.builder().entityCode(" ").build())).isEmpty();
    }

    @Test
    void resolve_shouldReturnMastersWithoutAccount() {
        when(serviceCodesRepository.findActiveMastersByEntityCodeAndChargeTypes(eq("DXB"), any()))
                .thenReturn(List.of(master));

        List<ServiceChargeResponse> result = resolver.resolve(
                ServiceChargeQueryCriteria.builder().entityCode("DXB").build());

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getServiceCode()).isEqualTo("SC01");
    }

    @Test
    void resolve_shouldFilterInactive_whenActiveFalseRequested() {
        ServiceCharge activeCharge = accountCharge(master, true);
        when(serviceCodesRepository.findActiveMastersByEntityCodeAndChargeTypes(eq("DXB"), any()))
                .thenReturn(List.of(master));
        when(serviceChargeRepository.findByAccountAndServiceCodesIn(eq(account), any()))
                .thenReturn(List.of(activeCharge));

        List<ServiceChargeResponse> result = resolver.resolve(
                ServiceChargeQueryCriteria.builder()
                        .entityCode("DXB")
                        .account(account)
                        .active(false)
                        .build());

        assertThat(result).isEmpty();
    }

    @Test
    void resolve_shouldNormalizeChargeTypesFilter() {
        when(serviceCodesRepository.findActiveMastersByEntityCodeAndChargeTypes(eq("DXB"), any()))
                .thenReturn(List.of());

        List<String> chargeTypes = new ArrayList<>();
        chargeTypes.add(" Estimate ");
        chargeTypes.add(null);
        chargeTypes.add("");
        chargeTypes.add("Estimate");
        resolver.resolve(ServiceChargeQueryCriteria.builder()
                .entityCode("DXB")
                .chargeTypes(chargeTypes)
                .build());

        resolver.resolve(ServiceChargeQueryCriteria.builder()
                .entityCode("DXB")
                .chargeTypes(List.of())
                .build());
    }

    @Test
    void toResponse_shouldHandleNullCharge() {
        ServiceChargeResponse response = ServiceChargeResolver.toResponse(null);
        assertThat(response.getActive()).isTrue();
        assertThat(response.getServiceCode()).isNull();
    }

    @Test
    void resolve_shouldUseRedis_whenChargeDataSynced() {
        AccountRedisChargeDataService redisService = mock(AccountRedisChargeDataService.class);
        when(accountRedisChargeDataService.getIfAvailable()).thenReturn(redisService);
        ServiceChargeResponse redisResponse = ServiceChargeResolver.toResponse(
                ServiceChargeResolver.defaultCharge(account, master));
        when(redisService.tryResolveFromRedis(any())).thenReturn(Optional.of(List.of(redisResponse)));

        List<ServiceChargeResponse> result = resolver.resolve(
                ServiceChargeQueryCriteria.builder()
                        .entityCode("DXB")
                        .account(account)
                        .active(true)
                        .build());

        assertThat(result).hasSize(1);
        verify(serviceCodesRepository, never()).findActiveMastersByEntityCodeAndChargeTypes(any(), any());
        verify(serviceChargeRepository, never()).findByAccountAndServiceCodesIn(any(), any());
    }

    @Test
    void resolve_shouldExcludeArCode_whenEntitySettingsChargeInactive() {
        account.setAccountNature(AccountNature.AR);
        account.setChargeType("DTC");

        ServiceCodes duty = new ServiceCodes();
        ReflectionTestUtils.setField(duty, "id", UUID.randomUUID());
        duty.setServiceCode("DUTY");
        duty.setChargeType("DTC");
        duty.setIsDeleted(false);
        duty.setIsActive(true);

        ServiceCharge arCharge = accountCharge(duty, true);
        Account entitySettings = new Account();
        ReflectionTestUtils.setField(entitySettings, "id", UUID.randomUUID());
        entitySettings.setAccountNature(AccountNature.ENTITY_SETTINGS);

        ServiceCharge entityInactive = accountCharge(duty, false);

        when(serviceCodesRepository.findActiveMastersByEntityCodeAndChargeTypes(eq("RUH"), any()))
                .thenReturn(List.of(duty));
        when(serviceChargeRepository.findByAccountAndServiceCodesIn(eq(account), any()))
                .thenReturn(List.of(arCharge));
        when(accountRepository.findByAccountNumberIsNullAndEntityCodeAndAccountNatureAndChargeType(
                eq("RUH"), eq(AccountNature.ENTITY_SETTINGS), eq("DTC")))
                .thenReturn(Optional.of(entitySettings));
        when(serviceChargeRepository.findByAccountWithActiveServiceCodes(entitySettings, "RUH", "DTC"))
                .thenReturn(List.of(entityInactive));

        List<ServiceChargeResponse> result = resolver.resolve(
                ServiceChargeQueryCriteria.builder()
                        .entityCode("RUH")
                        .account(account)
                        .chargeTypes(List.of("DTC"))
                        .active(true)
                        .build());

        assertThat(result).isEmpty();
    }

    @Test
    void resolve_shouldNotResurrectInactiveChargeAsSyntheticDefault() {
        ServiceCharge inactiveCharge = accountCharge(master, false);

        when(serviceCodesRepository.findActiveMastersByEntityCodeAndChargeTypes(eq("DXB"), any()))
                .thenReturn(List.of(master));
        when(serviceChargeRepository.findByAccountAndServiceCodesIn(eq(account), any()))
                .thenReturn(List.of(inactiveCharge));

        List<ServiceChargeResponse> result = resolver.resolve(
                ServiceChargeQueryCriteria.builder()
                        .entityCode("DXB")
                        .account(account)
                        .build());

        assertThat(result).isEmpty();
    }

    private static ServiceCharge accountCharge(ServiceCodes master, boolean active) {
        ServiceCharge charge = new ServiceCharge();
        charge.setServiceCodes(master);
        charge.setActive(active);
        return charge;
    }
}
