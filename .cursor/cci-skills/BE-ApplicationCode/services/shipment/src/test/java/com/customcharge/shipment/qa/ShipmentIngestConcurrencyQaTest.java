package com.customcharge.shipment.qa;

import com.customcharge.persistencelayer.dto.ShipmentDto;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.shipment.observability.ShipmentProcessingPhaseTimer;
import com.customcharge.shipment.service.ChargeService;
import com.customcharge.shipment.service.PreparedNewShipment;
import com.customcharge.shipment.service.ShipmentIngestTransactionService;
import com.customcharge.shipment.service.ShipmentPersistenceService;
import com.customcharge.shipment.service.ShipmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * QA: TC-TX concurrency scenarios (in-process simulation with mocks).
 */
@ExtendWith(MockitoExtension.class)
class ShipmentIngestConcurrencyQaTest {

    @Mock
    private ShipmentPersistenceService shipmentPersistenceService;
    @Mock
    private ChargeService chargeService;
    @Mock
    private ShipmentProcessingPhaseTimer phaseTimer;
    @Mock
    private ShipmentIngestTransactionService shipmentIngestTransactionService;

    @InjectMocks
    private ShipmentService shipmentService;

    @BeforeEach
    void phaseTimerPassthrough() {
        lenient().doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(2)).run();
            return null;
        }).when(phaseTimer).run(any(), any(), any(Runnable.class));
    }

    @Test
    @DisplayName("TC-TX-002: Duplicate HAWB at prepare — no TX, optional backfill")
    void tcTx002_duplicateAtPrepare() {
        ShipmentDto message = dto("HAWB-DUP");
        when(shipmentPersistenceService.prepareNewShipment(message)).thenReturn(Optional.empty());
        when(shipmentPersistenceService.findShipmentNeedingCharges("HAWB-DUP")).thenReturn(Optional.empty());

        shipmentService.processShipment(message);

        verify(shipmentIngestTransactionService, times(0)).persistAndCreateCharges(any(), any());
    }

    @Test
    @DisplayName("TC-TX-003: Race at persist — TX empty then backfill charges")
    void tcTx003_raceAtPersistBackfill() {
        ShipmentDto message = dto("HAWB-RACE");
        Shipment existing = new Shipment();
        PreparedNewShipment prepared = new PreparedNewShipment(new Shipment(), "HAWB-RACE", List.of());
        when(shipmentPersistenceService.prepareNewShipment(message)).thenReturn(Optional.of(prepared));
        when(shipmentIngestTransactionService.persistAndCreateCharges(prepared, message)).thenReturn(Optional.empty());
        when(shipmentPersistenceService.findShipmentNeedingCharges("HAWB-RACE")).thenReturn(Optional.of(existing));

        shipmentService.processShipment(message);

        verify(chargeService).createCharges(existing);
    }

    @Test
    @DisplayName("TC-TX-004: Two different HAWBs — independent successful ingest")
    void tcTx004_twoDifferentHawbs() {
        ShipmentDto m1 = dto("HAWB-A");
        ShipmentDto m2 = dto("HAWB-B");
        PreparedNewShipment p1 = new PreparedNewShipment(new Shipment(), "HAWB-A", List.of());
        PreparedNewShipment p2 = new PreparedNewShipment(new Shipment(), "HAWB-B", List.of());
        when(shipmentPersistenceService.prepareNewShipment(m1)).thenReturn(Optional.of(p1));
        when(shipmentPersistenceService.prepareNewShipment(m2)).thenReturn(Optional.of(p2));
        when(shipmentIngestTransactionService.persistAndCreateCharges(p1, m1)).thenReturn(Optional.of(new Shipment()));
        when(shipmentIngestTransactionService.persistAndCreateCharges(p2, m2)).thenReturn(Optional.of(new Shipment()));

        shipmentService.processShipment(m1);
        shipmentService.processShipment(m2);

        verify(shipmentIngestTransactionService, times(2)).persistAndCreateCharges(any(), any());
    }

    @Test
    @DisplayName("TC-TX-005: Two threads same HAWB — both prepare; one wins TX (simulated)")
    void tcTx005_twoThreadsSameHawb() throws InterruptedException {
        ShipmentDto message = dto("HAWB-CONC");
        PreparedNewShipment prepared = new PreparedNewShipment(new Shipment(), "HAWB-CONC", List.of());
        when(shipmentPersistenceService.prepareNewShipment(message)).thenReturn(Optional.of(prepared));

        AtomicInteger txCalls = new AtomicInteger();
        when(shipmentIngestTransactionService.persistAndCreateCharges(prepared, message)).thenAnswer(invocation -> {
            if (txCalls.incrementAndGet() == 1) {
                return Optional.of(new Shipment());
            }
            return Optional.empty();
        });
        when(shipmentPersistenceService.findShipmentNeedingCharges("HAWB-CONC")).thenReturn(Optional.empty());

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        for (int i = 0; i < 2; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    shipmentService.processShipment(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();

        verify(shipmentIngestTransactionService, times(2)).persistAndCreateCharges(prepared, message);
        assertThat(txCalls.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("TC-TX-006: Same consignee/account on two HAWBs — both ingest (no shared lock in service)")
    void tcTx006_sameAccountTwoHawbs() {
        ShipmentDto m1 = dto("HAWB-S1");
        m1.setConsigneeNumber("AR-999");
        ShipmentDto m2 = dto("HAWB-S2");
        m2.setConsigneeNumber("AR-999");
        when(shipmentPersistenceService.prepareNewShipment(any())).thenAnswer(inv -> {
            ShipmentDto d = inv.getArgument(0);
            return Optional.of(new PreparedNewShipment(new Shipment(), d.getHawb(), List.of()));
        });
        when(shipmentIngestTransactionService.persistAndCreateCharges(any(), any()))
                .thenReturn(Optional.of(new Shipment()));

        shipmentService.processShipment(m1);
        shipmentService.processShipment(m2);

        verify(shipmentIngestTransactionService, times(2)).persistAndCreateCharges(any(), any());
    }

    @Test
    @DisplayName("TC-TX-007: Backfill when prior TX failed after insert (shipment without charges)")
    void tcTx007_backfillAfterPartialFailure() {
        ShipmentDto message = dto("HAWB-BF");
        Shipment orphan = new Shipment();
        when(shipmentPersistenceService.prepareNewShipment(message)).thenReturn(Optional.empty());
        when(shipmentPersistenceService.findShipmentNeedingCharges("HAWB-BF")).thenReturn(Optional.of(orphan));

        shipmentService.processShipment(message);

        verify(chargeService).createCharges(orphan);
    }

    private static ShipmentDto dto(String hawb) {
        ShipmentDto message = new ShipmentDto();
        message.setHawb(hawb);
        message.setIsShipmentFull(true);
        return message;
    }
}
