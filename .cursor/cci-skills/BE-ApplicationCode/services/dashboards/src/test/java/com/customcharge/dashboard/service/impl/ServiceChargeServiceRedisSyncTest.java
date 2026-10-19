package com.customcharge.dashboard.service.impl;

import com.customcharge.dashboard.dto.ServiceChargeUpdateRequest;
import com.customcharge.dashboard.service.AccountService;
import com.customcharge.dashboard.service.ServiceChargeRateSheetUsageValidator;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.ARAccount;
import com.customcharge.persistencelayer.entities.Entity;
import com.customcharge.persistencelayer.entities.ServiceCharge;
import com.customcharge.persistencelayer.entities.ServiceCodes;
import com.customcharge.persistencelayer.enums.AccountNature;
import com.customcharge.persistencelayer.repository.ARAccountRepository;
import com.customcharge.persistencelayer.repository.AccountRepository;
import com.customcharge.persistencelayer.repository.EntityRepository;
import com.customcharge.persistencelayer.repository.RateSheetRepository;
import com.customcharge.persistencelayer.repository.ServiceChargeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceChargeServiceRedisSyncTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private ARAccountRepository arAccountRepository;
    @Mock
    private EntityRepository entityRepository;
    @Mock
    private RateSheetRepository rateSheetRepository;
    @Mock
    private ServiceChargeRepository serviceChargeRepository;
    @Mock
    private ServiceCodesService serviceCodesService;
    @Mock
    private ServiceChargeRateSheetUsageValidator rateSheetUsageValidator;
    @Mock
    private AccountService accountService;

    @InjectMocks
    private ServiceChargeService service;

    @Test
    void update_shouldSyncRedisWhenAccountIdIsPresent() {
        UUID accountId = UUID.randomUUID();
        UUID chargeId = UUID.randomUUID();

        Account account = new Account();
        ReflectionTestUtils.setField(account, "id", accountId);
        account.setAccountNature(AccountNature.AR);
        account.setChargeType("DTC");
        Entity entity = new Entity();
        entity.setCode("RUH");
        account.setEntity(entity);
        ARAccount arAccount = new ARAccount();
        arAccount.setAccountNumber("238296");
        account.setArAccount(arAccount);

        ServiceCodes master = new ServiceCodes();
        ReflectionTestUtils.setField(master, "id", UUID.randomUUID());
        master.setServiceCode("DUTY");

        ServiceCharge charge = new ServiceCharge();
        ReflectionTestUtils.setField(charge, "id", chargeId);
        charge.setAccount(account);
        charge.setServiceCodes(master);
        charge.setActive(true);

        ServiceChargeUpdateRequest request = new ServiceChargeUpdateRequest();
        request.setId(chargeId);
        request.setAccountId(accountId);
        request.setActive(true);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(serviceChargeRepository.findByIdWithActiveServiceCodes(chargeId)).thenReturn(Optional.of(charge));
        when(serviceChargeRepository.save(charge)).thenReturn(charge);

        service.update(List.of(request));

        verify(accountService).syncAccountToRedis(eq("238296"), eq(AccountNature.AR), eq("RUH"), isNull());
    }

    @Test
    void delete_shouldSyncRedisForChargeAccount() {
        UUID chargeId = UUID.randomUUID();

        Account account = new Account();
        ReflectionTestUtils.setField(account, "id", UUID.randomUUID());
        account.setAccountNature(AccountNature.ENTITY_SETTINGS);
        account.setChargeType("ECD");
        Entity entity = new Entity();
        entity.setCode("RUH");
        account.setEntity(entity);

        ServiceCodes master = new ServiceCodes();
        ReflectionTestUtils.setField(master, "id", UUID.randomUUID());
        master.setServiceCode("DUTY");

        ServiceCharge charge = new ServiceCharge();
        ReflectionTestUtils.setField(charge, "id", chargeId);
        charge.setAccount(account);
        charge.setServiceCodes(master);

        when(serviceChargeRepository.findByIdWithActiveServiceCodes(chargeId)).thenReturn(Optional.of(charge));

        service.delete(chargeId);

        verify(accountService).syncAccountToRedis(isNull(), eq(AccountNature.ENTITY_SETTINGS), eq("RUH"), isNull());
    }
}
