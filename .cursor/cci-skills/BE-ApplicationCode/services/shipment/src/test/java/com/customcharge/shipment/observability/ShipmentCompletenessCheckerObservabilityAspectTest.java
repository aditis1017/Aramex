package com.customcharge.shipment.observability;

import com.customcharge.persistencelayer.dto.ShipmentDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ShipmentCompletenessCheckerObservabilityAspectTest {

    @Mock
    private AsyncShipmentIngestMetrics ingestMetrics;
    @Mock
    private ShipmentTrackingLogger trackingLogger;

    @InjectMocks
    private ShipmentCompletenessCheckerObservabilityAspect aspect;

    @Test
    void afterFilterRules_shouldRecordFilterPassedWhenShipmentFull() {
        ShipmentDto shipment = new ShipmentDto();
        shipment.setHawb("HAWB-1");
        shipment.setDestinationEntity("RUH");
        shipment.setIsShipmentFull(true);

        aspect.afterFilterRules(shipment);

        verify(ingestMetrics).recordFilterPassed("RUH");
        verify(trackingLogger).logFilterPassed("HAWB-1", "RUH");
    }

    @Test
    void afterFilterRules_shouldSkipWhenShipmentNotFull() {
        ShipmentDto shipment = new ShipmentDto();
        shipment.setHawb("HAWB-2");
        shipment.setDestinationEntity("JED");
        shipment.setIsShipmentFull(false);

        aspect.afterFilterRules(shipment);

        verify(ingestMetrics, never()).recordFilterPassed("JED");
        verify(trackingLogger, never()).logFilterPassed("HAWB-2", "JED");
    }
}
