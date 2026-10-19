package com.customcharge.shipment.observability;

import com.customcharge.persistencelayer.dto.ShipmentDto;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.repository.ShipmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShipmentPersistenceObservabilityAspectTest {

    @Mock
    private AsyncShipmentIngestMetrics ingestMetrics;
    @Mock
    private ShipmentTrackingLogger trackingLogger;
    @Mock
    private ShipmentRepository shipmentRepository;

    @InjectMocks
    private ShipmentPersistenceObservabilityAspect aspect;

    @Test
    void afterPersist_shouldRecordCreatedWhenShipmentSaved() {
        ShipmentDto dto = new ShipmentDto();
        dto.setHawb("HAWB-1");
        dto.setDestinationEntity("RUH");

        aspect.afterPersist(dto, Optional.of(new Shipment()));

        verify(ingestMetrics).recordEntityCreated("RUH");
        verify(trackingLogger).logCreated("HAWB-1", "RUH");
        verify(trackingLogger, never()).logDuplicateSkipped("HAWB-1", "RUH");
    }

    @Test
    void afterPersist_shouldRecordDuplicateSkippedWhenHawbExists() {
        ShipmentDto dto = new ShipmentDto();
        dto.setHawb("HAWB-2");
        dto.setDestinationEntity("JED");
        when(shipmentRepository.existsByHawbNumber("HAWB-2")).thenReturn(true);

        aspect.afterPersist(dto, Optional.empty());

        verify(ingestMetrics, never()).recordEntityCreated("JED");
        verify(trackingLogger, never()).logCreated("HAWB-2", "JED");
        verify(trackingLogger).logDuplicateSkipped("HAWB-2", "JED");
    }

    @Test
    void afterPersist_shouldSkipWhenEmptyWithoutExistingHawb() {
        ShipmentDto dto = new ShipmentDto();

        aspect.afterPersist(dto, Optional.empty());

        verify(trackingLogger, never()).logCreated("HAWB-2", "JED");
        verify(trackingLogger, never()).logDuplicateSkipped("HAWB-2", "JED");
        verify(shipmentRepository, never()).existsByHawbNumber(org.mockito.ArgumentMatchers.anyString());
    }
}
