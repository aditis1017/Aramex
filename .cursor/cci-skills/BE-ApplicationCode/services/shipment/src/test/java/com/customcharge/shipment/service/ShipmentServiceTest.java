package com.customcharge.shipment.service;

import com.customcharge.persistencelayer.dto.ShipmentDto;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.shipment.observability.ShipmentProcessingPhaseTimer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShipmentServiceTest {

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
    void setUpPhaseTimerPassthrough() {
        lenient().doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(2)).run();
            return null;
        }).when(phaseTimer).run(anyString(), anyString(), any(Runnable.class));
    }

    @Test
    void processShipment_shouldCreateCharges_whenShipmentPersisted() {
        ShipmentDto message = new ShipmentDto();
        message.setHawb("HAWB-1");
        Shipment shipment = new Shipment();
        PreparedNewShipment prepared = new PreparedNewShipment(shipment, "HAWB-1", List.of());
        when(shipmentPersistenceService.prepareNewShipment(message)).thenReturn(Optional.of(prepared));
        when(shipmentIngestTransactionService.persistAndCreateCharges(prepared, message))
                .thenReturn(Optional.of(shipment));

        shipmentService.processShipment(message);

        verify(shipmentIngestTransactionService).persistAndCreateCharges(prepared, message);
        verify(chargeService, never()).createCharges(any());
    }

    @Test
    void processShipment_shouldSkipCharges_whenShipmentNotPersistedAndNoBackfill() {
        ShipmentDto message = new ShipmentDto();
        message.setHawb("HAWB-2");
        when(shipmentPersistenceService.prepareNewShipment(message)).thenReturn(Optional.empty());
        when(shipmentPersistenceService.findShipmentNeedingCharges("HAWB-2")).thenReturn(Optional.empty());

        shipmentService.processShipment(message);

        verify(shipmentIngestTransactionService, never()).persistAndCreateCharges(any(), any());
        verify(chargeService, never()).createCharges(any());
    }

    @Test
    void processShipment_shouldBackfillCharges_whenDuplicateHawbWithoutCharges() {
        ShipmentDto message = new ShipmentDto();
        message.setHawb("HAWB-3");
        Shipment existing = new Shipment();
        when(shipmentPersistenceService.prepareNewShipment(message)).thenReturn(Optional.empty());
        when(shipmentPersistenceService.findShipmentNeedingCharges("HAWB-3")).thenReturn(Optional.of(existing));

        shipmentService.processShipment(message);

        verify(chargeService).createCharges(existing);
        verify(shipmentIngestTransactionService, never()).persistAndCreateCharges(any(), any());
    }

    @Test
    void processShipment_shouldPropagateFailure_whenIngestTransactionFails() {
        ShipmentDto message = new ShipmentDto();
        message.setHawb("HAWB-4");
        Shipment shipment = new Shipment();
        PreparedNewShipment prepared = new PreparedNewShipment(shipment, "HAWB-4", List.of());
        when(shipmentPersistenceService.prepareNewShipment(message)).thenReturn(Optional.of(prepared));
        doThrow(new IllegalStateException("charge failed"))
                .when(shipmentIngestTransactionService)
                .persistAndCreateCharges(prepared, message);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> shipmentService.processShipment(message))
                .isInstanceOf(IllegalStateException.class);
    }
}
