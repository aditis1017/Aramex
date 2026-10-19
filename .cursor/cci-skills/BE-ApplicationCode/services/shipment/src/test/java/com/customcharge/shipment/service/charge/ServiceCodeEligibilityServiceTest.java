package com.customcharge.shipment.service.charge;

import com.customcharge.persistencelayer.accountRedis.AccountRedisChargeDataService;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.RateSheetItem;
import com.customcharge.persistencelayer.entities.ServiceCodes;
import com.customcharge.persistencelayer.enums.AccountNature;
import com.customcharge.persistencelayer.repository.ServiceCodesRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;

@ExtendWith(MockitoExtension.class)
class ServiceCodeEligibilityServiceTest {

    @Mock
    private ServiceCodesRepository serviceCodesRepository;
    @Mock
    private AccountRedisChargeDataService accountRedisChargeDataService;

    @InjectMocks
    private ServiceCodeEligibilityService service;

    @org.junit.jupiter.api.BeforeEach
    void setUpRedisFallback() {
        org.mockito.Mockito.lenient()
                .when(accountRedisChargeDataService.getEntityServiceCodeMasters(anyString(), anyString()))
                .thenReturn(Collections.emptyList());
        org.mockito.Mockito.lenient()
                .when(accountRedisChargeDataService.readEntitySettingsChargeTypeData(anyString(), anyString()))
                .thenReturn(java.util.Optional.empty());
    }

    @Test
    void isRateSheetItemEligible_shouldReturnFalse_whenInputsMissing() {
        assertThat(service.isRateSheetItemEligible(null, "RUH", "ECD", null)).isFalse();
        assertThat(service.isRateSheetItemEligible(new Account(), "", "ECD", new RateSheetItem())).isFalse();
        verifyNoInteractions(serviceCodesRepository);
    }

    @Test
    void isRateSheetItemEligible_shouldReturnFalse_whenServiceCodeMissing() {
        RateSheetItem item = new RateSheetItem();

        assertThat(service.isRateSheetItemEligible(new Account(), "RUH", "ECD", item)).isFalse();
    }

    @Test
    void isRateSheetItemEligible_shouldReturnTrue_forNonArAccount() {
        RateSheetItem item = new RateSheetItem();
        item.setServiceCode("DUTY");

        assertThat(service.isRateSheetItemEligible(new Account(), "RUH", "ECD", item)).isTrue();
        verifyNoInteractions(serviceCodesRepository);
    }

    @Test
    void isRateSheetItemEligible_shouldReturnFalse_whenArMasterMissing() {
        Account account = arAccount();
        RateSheetItem item = new RateSheetItem();
        item.setServiceCode("DUTY");
        when(serviceCodesRepository.findByEntityCodeAndChargeTypeAndIsDeletedFalse("RUH", "ECD"))
                .thenReturn(List.of());

        assertThat(service.isRateSheetItemEligible(account, "RUH", "ECD", item)).isFalse();
    }

    @Test
    void isRateSheetItemEligible_shouldReturnTrue_whenArMasterEligible() {
        Account account = arAccount();
        RateSheetItem item = new RateSheetItem();
        item.setServiceCode("DUTY");
        ServiceCodes master = activeMaster("DUTY", "ECD");
        when(serviceCodesRepository.findByEntityCodeAndChargeTypeAndIsDeletedFalse("RUH", "ECD"))
                .thenReturn(List.of(master));

        assertThat(service.isRateSheetItemEligible(account, "RUH", "ECD", item)).isTrue();
    }

    @Test
    void isRateSheetItemEligible_shouldReturnFalse_whenArMasterInactive() {
        Account account = arAccount();
        RateSheetItem item = new RateSheetItem();
        item.setServiceCode("DUTY");
        ServiceCodes master = activeMaster("DUTY", "ECD");
        master.setIsActive(false);
        when(serviceCodesRepository.findByEntityCodeAndChargeTypeAndIsDeletedFalse("RUH", "ECD"))
                .thenReturn(List.of(master));

        assertThat(service.isRateSheetItemEligible(account, "RUH", "ECD", item)).isFalse();
    }

    private static Account arAccount() {
        Account account = new Account();
        account.setAccountNature(AccountNature.AR);
        return account;
    }

    private static ServiceCodes activeMaster(String serviceCode, String chargeType) {
        ServiceCodes master = new ServiceCodes();
        master.setServiceCode(serviceCode);
        master.setChargeType(chargeType);
        master.setIsActive(true);
        master.setIsDeleted(false);
        return master;
    }
}
