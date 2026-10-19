package com.customcharge.dashboard.service.impl;

import com.customcharge.dashboard.dto.RateConfigRequest;
import com.customcharge.dashboard.service.AccountRateSheetResolver;
import com.customcharge.dashboard.service.AccountService;
import com.customcharge.dashboard.service.RateSheetItemDuplicateValidator;
import com.customcharge.dashboard.service.RateSheetItemFieldValidation;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.ARAccount;
import com.customcharge.persistencelayer.entities.Entity;
import com.customcharge.persistencelayer.entities.RateSheet;
import com.customcharge.persistencelayer.entities.RateSheetItem;
import com.customcharge.persistencelayer.enums.AccountNature;
import com.customcharge.persistencelayer.repository.RateSheetItemRepository;
import com.customcharge.persistencelayer.repository.RateSheetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateSheetServiceRedisSyncTest {

    @Mock
    private RateSheetRepository rateSheetRepository;
    @Mock
    private RateSheetItemRepository rateSheetItemRepository;
    @Mock
    private RateSheetItemFieldValidation fieldValidation;
    @Mock
    private RateSheetItemDuplicateValidator duplicateValidator;
    @Mock
    private AccountService accountService;
    @Mock
    private AccountRateSheetResolver accountRateSheetResolver;
    @Mock
    private ServiceChargeService serviceChargeService;

    @InjectMocks
    private RateSheetService service;

    @Test
    void upsert_shouldSyncRedisAfterRateUpdate() {
        UUID rateSheetId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        Account account = new Account();
        ReflectionTestUtils.setField(account, "id", UUID.randomUUID());
        account.setAccountNature(AccountNature.AR);
        account.setChargeType("DTC");
        Entity entity = new Entity();
        entity.setCode("RUH");
        account.setEntity(entity);
        ARAccount arAccount = new ARAccount();
        arAccount.setAccountNumber("238296");
        account.setArAccount(arAccount);

        RateSheet rateSheet = new RateSheet();
        ReflectionTestUtils.setField(rateSheet, "id", rateSheetId);
        rateSheet.setAccount(account);

        RateSheetItem item = new RateSheetItem();
        ReflectionTestUtils.setField(item, "id", itemId);
        item.setRateSheet(rateSheet);
        item.setRates(new java.util.ArrayList<>());

        RateConfigRequest request = new RateConfigRequest();
        request.setRateSheetItemId(itemId);
        request.setRateType("Fixed");
        request.setValue(BigDecimal.valueOf(10.0));

        when(rateSheetItemRepository.findWithRatesById(itemId)).thenReturn(Optional.of(item));
        when(rateSheetRepository.findById(rateSheetId)).thenReturn(Optional.of(rateSheet));
        when(rateSheetItemRepository.save(item)).thenReturn(item);

        service.upsert(request);

        verify(accountService).syncAccountToRedis(eq("238296"), eq(AccountNature.AR), eq("RUH"), isNull());
    }
}
