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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShipmentIngestTransactionServiceTest {

    @Mock
    private ShipmentPersistenceWriter shipmentPersistenceWriter;
    @Mock
    private ChargeService chargeService;
    @Mock
    private ShipmentProcessingPhaseTimer phaseTimer;

    @InjectMocks
    private ShipmentIngestTransactionService shipmentIngestTransactionService;

    @BeforeEach
    void setUpPhaseTimerPassthrough() {
        lenient().doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(2)).run();
            return null;
        }).when(phaseTimer).run(anyString(), anyString(), any(Runnable.class));
    }

    @Test
    void persistAndCreateCharges_shouldCreateCharges_whenSaveSucceeds() {
        ShipmentDto message = new ShipmentDto();
        message.setHawb("HAWB-1");
        Shipment shipment = new Shipment();
        PreparedNewShipment prepared = new PreparedNewShipment(shipment, "HAWB-1", List.of());
        when(shipmentPersistenceWriter.saveNewShipment(shipment, "HAWB-1", List.of()))
                .thenReturn(Optional.of(shipment));

        Optional<Shipment> result = shipmentIngestTransactionService.persistAndCreateCharges(prepared, message);

        assertThat(result).contains(shipment);
        verify(chargeService).createCharges(shipment);
    }

    @Test
    void persistAndCreateCharges_shouldSkipCharges_whenInsertRaceDuplicate() {
        ShipmentDto message = new ShipmentDto();
        message.setHawb("HAWB-2");
        Shipment shipment = new Shipment();
        PreparedNewShipment prepared = new PreparedNewShipment(shipment, "HAWB-2", List.of());
        when(shipmentPersistenceWriter.saveNewShipment(any(), eq("HAWB-2"), any())).thenReturn(Optional.empty());

        Optional<Shipment> result = shipmentIngestTransactionService.persistAndCreateCharges(prepared, message);

        assertThat(result).isEmpty();
        verify(chargeService, never()).createCharges(any());
    }
}
