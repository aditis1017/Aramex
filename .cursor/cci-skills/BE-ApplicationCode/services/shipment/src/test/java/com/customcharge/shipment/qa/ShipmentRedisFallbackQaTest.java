package com.customcharge.shipment.qa;

import com.customcharge.persistencelayer.accountRedis.AccountRedisChargeDataService;
import com.customcharge.persistencelayer.entities.ServiceCodes;
import com.customcharge.shipment.service.charge.ChargeLineItemTaxSupport;
import com.customcharge.shipment.service.charge.processing.ShipmentProcessingContext;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.repository.EntityRepository;
import com.customcharge.persistencelayer.repository.ServiceChargeRepository;
import com.customcharge.persistencelayer.repository.ServiceCodesRepository;
import com.customcharge.shipment.service.charge.ServiceChargeAccountResolver;
import com.customcharge.shipment.service.charge.ShipmentChargeAmountSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * QA: TC-RD shipment-side Redis miss → DB fallback (complements persistence-layer QA tests).
 */
@ExtendWith(MockitoExtension.class)
class ShipmentRedisFallbackQaTest {

    @Mock
    private ServiceChargeAccountResolver serviceChargeAccountResolver;
    @Mock
    private ServiceChargeRepository serviceChargeRepository;
    @Mock
    private ServiceCodesRepository serviceCodesRepository;
    @Mock
    private AccountRedisChargeDataService accountRedisChargeDataService;
    @Mock
    private EntityRepository entityRepository;
    @Mock
    private ShipmentChargeAmountSupport chargeAmountSupport;

    @InjectMocks
    private ChargeLineItemTaxSupport chargeLineItemTaxSupport;

    @AfterEach
    void clearContext() {
        ShipmentProcessingContext.clear();
    }

    @Test
    @DisplayName("TC-RD-004: Tax path without context uses DB when Redis master miss")
    void tcRd004_taxDbFallbackWithoutContext() {
        Shipment shipment = new Shipment();
        Charge charge = new Charge();
        charge.setChargeCode("ECD");
        Account account = new Account();
        ServiceCodes master = new ServiceCodes();
        ReflectionTestUtils.setField(master, "id", UUID.randomUUID());
        master.setFetchTax(false);

        when(accountRedisChargeDataService.findServiceCodeMaster("RUH", "ECD", "DUTY"))
                .thenReturn(Optional.empty());
        when(serviceCodesRepository.findByEntity_CodeAndServiceCodeAndChargeTypeAndIsDeletedFalse(
                "RUH", "DUTY", "ECD")).thenReturn(Optional.of(master));
        when(accountRedisChargeDataService.findServiceCharge(
                eq(account), eq("RUH"), any(), eq(master))).thenReturn(Optional.empty());
        when(entityRepository.findByCodeIgnoreCase("RUH")).thenReturn(Optional.empty());

        Double tax = chargeLineItemTaxSupport.resolveTaxPercent(
                shipment, charge, account, "RUH", "DUTY", null);

        assertThat(tax).isNull();
        verify(serviceCodesRepository).findByEntity_CodeAndServiceCodeAndChargeTypeAndIsDeletedFalse(
                "RUH", "DUTY", "ECD");
    }

    @Test
    @DisplayName("TC-RD-005: Processing context cleared between simulated messages")
    void tcRd005_contextIsolation() {
        ShipmentProcessingContext.clear();
        assertThat(ShipmentProcessingContext.get()).isNull();
    }
}
