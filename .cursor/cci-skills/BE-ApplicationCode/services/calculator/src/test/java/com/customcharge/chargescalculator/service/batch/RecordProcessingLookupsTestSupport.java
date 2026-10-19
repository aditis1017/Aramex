package com.customcharge.chargescalculator.service.batch;

import com.customcharge.persistencelayer.dto.ServiceChargeResponse;
import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.ChargeLineItem;
import com.customcharge.persistencelayer.entities.Entity;
import com.customcharge.persistencelayer.repository.ChargeLineItemRepository;
import com.customcharge.persistencelayer.repository.ChargeRepository;
import com.customcharge.persistencelayer.repository.EntityRepository;
import org.mockito.stubbing.Answer;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

public final class RecordProcessingLookupsTestSupport {

    private RecordProcessingLookupsTestSupport() {
    }

    public static void delegateToRepositories(
            RecordProcessingLookups recordProcessingLookups,
            ChargeRepository chargeRepository,
            ChargeLineItemRepository chargeLineItemRepository,
            EntityRepository entityRepository) {
        lenient().when(recordProcessingLookups.findCharge(any(UUID.class), any()))
                .thenAnswer((Answer<?>) invocation -> chargeRepository.findByShipment_IdAndChargeCode(
                        invocation.getArgument(0), invocation.getArgument(1)));
        lenient().when(recordProcessingLookups.findLineItems(any(UUID.class)))
                .thenAnswer((Answer<List<ChargeLineItem>>) invocation ->
                        chargeLineItemRepository.findByCharge_Id(invocation.getArgument(0)));
        if (entityRepository != null) {
            lenient().when(recordProcessingLookups.findEntity(any()))
                    .thenAnswer((Answer<?>) invocation -> entityRepository.findByCodeIgnoreCase(invocation.getArgument(0)));
        }
        lenient().doAnswer(invocation -> {
            Charge charge = invocation.getArgument(0);
            return null;
        }).when(recordProcessingLookups).registerCharge(any(Charge.class));
        lenient().doAnswer(invocation -> null).when(recordProcessingLookups).cacheLineItems(any(), any());
        lenient().when(recordProcessingLookups.entitySettingsAccounts(any(), any(), any()))
                .thenAnswer(invocation -> invocation.<Supplier<List<?>>>getArgument(2).get());
        lenient().when(recordProcessingLookups.partyArAccount(any(), any(), any(), any()))
                .thenAnswer(invocation -> invocation.<Supplier<java.util.Optional<com.customcharge.persistencelayer.entities.Account>>>getArgument(3).get());
        lenient().when(recordProcessingLookups.descriptionMappings(any(), any(), any(), any()))
                .thenAnswer(invocation -> invocation.<Supplier<List<?>>>getArgument(3).get());
        lenient().when(recordProcessingLookups.serviceCharges(any(), any(), any(), any()))
                .thenAnswer(invocation -> invocation.<Supplier<List<ServiceChargeResponse>>>getArgument(3).get());
        lenient().when(recordProcessingLookups.dtaAccounts(any(), any()))
                .thenAnswer(invocation -> invocation.<Supplier<List<?>>>getArgument(1).get());
        lenient().when(recordProcessingLookups.dtcBillingAccount(any(), any(), any()))
                .thenAnswer(invocation -> invocation.<Supplier<java.util.Optional<com.customcharge.persistencelayer.entities.Account>>>getArgument(2).get());
        lenient().when(recordProcessingLookups.arMaster(any(), any(), any()))
                .thenAnswer(invocation -> invocation.<Supplier<java.util.Optional<com.customcharge.persistencelayer.entities.ARAccount>>>getArgument(2).get());
    }
}
