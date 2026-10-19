package com.customcharge.chargescalculator.service.batch;

import com.customcharge.chargescalculator.support.CalculatorTestDataFactory;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.ChargeLineItem;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.repository.ChargeLineItemRepository;
import com.customcharge.persistencelayer.repository.ChargeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RowChargeWriteBufferTest {

    @Mock
    private ChargeRepository chargeRepository;
    @Mock
    private ChargeLineItemRepository chargeLineItemRepository;

    private RowChargeWriteBuffer buffer;

    @BeforeEach
    void setUp() {
        buffer = new RowChargeWriteBuffer(chargeRepository, chargeLineItemRepository);
    }

    @AfterEach
    void tearDown() {
        buffer.close();
    }

    @Test
    void persistNewCharge_withoutScope_shouldSaveImmediately() {
        Shipment shipment = CalculatorTestDataFactory.shipment();
        Charge charge = new Charge();
        charge.setShipment(shipment);
        charge.setChargeCode("ECD");
        UUID chargeId = UUID.randomUUID();
        when(chargeRepository.save(charge)).thenAnswer(invocation -> {
            Charge saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", chargeId);
            return saved;
        });

        Charge saved = buffer.persistNewCharge(charge);

        assertThat(saved.getId()).isEqualTo(chargeId);
        verify(chargeRepository).save(charge);
    }

    @Test
    void flush_shouldDeleteBeforeSaveAndDeduplicateLineKeys() {
        buffer.open();
        Shipment shipment = CalculatorTestDataFactory.shipment();
        Charge charge = CalculatorTestDataFactory.charge(UUID.randomUUID(), shipment, "ECD");
        Account account = CalculatorTestDataFactory.account(UUID.randomUUID(), "ECD");

        ChargeLineItem orphan = CalculatorTestDataFactory.chargeLineItem(
                charge, account, "DUTY", BigDecimal.ONE);
        ChargeLineItem replacement = CalculatorTestDataFactory.chargeLineItem(
                charge, account, "DUTY", BigDecimal.TEN);

        buffer.enqueueLineDeletes(List.of(orphan));
        buffer.enqueueLineSaves(List.of(replacement));
        buffer.enqueueChargeUpdate(charge);
        buffer.flush();

        verify(chargeLineItemRepository).deleteAllById(any());
        ArgumentCaptor<List<ChargeLineItem>> saveCaptor = ArgumentCaptor.forClass(List.class);
        verify(chargeLineItemRepository).saveAll(saveCaptor.capture());
        assertThat(saveCaptor.getValue()).containsExactly(replacement);
        verify(chargeRepository).saveAll(List.of(charge));
    }

    @Test
    void close_shouldFlushOnceWhenNotExplicitlyFlushed() {
        buffer.open();
        Shipment shipment = CalculatorTestDataFactory.shipment();
        Charge charge = CalculatorTestDataFactory.charge(UUID.randomUUID(), shipment, "DTC");
        buffer.enqueueChargeUpdate(charge);

        buffer.close();

        verify(chargeRepository).saveAll(List.of(charge));
        verify(chargeLineItemRepository, never()).deleteAllById(any());
    }
}
