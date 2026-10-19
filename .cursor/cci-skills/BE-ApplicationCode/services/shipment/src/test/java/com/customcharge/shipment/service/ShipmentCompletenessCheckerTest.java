package com.customcharge.shipment.service;

import com.customcharge.persistencelayer.dto.ShipmentDto;
import com.customcharge.shipment.service.rules.ShipmentFullnessRuleResolver;
import com.customcharge.shipment.service.rules.ShipmentMawbRuleResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ShipmentCompletenessCheckerTest {

    @Mock
    private ShipmentFullnessRuleResolver shipmentFullnessRuleResolver;
    @Mock
    private ShipmentMawbRuleResolver shipmentMawbRuleResolver;

    @InjectMocks
    private ShipmentCompletenessChecker checker;

    @Test
    void applyShipmentFilterRules_shouldDelegateToResolver() {
        ShipmentDto dto = new ShipmentDto();
        dto.setHawb("HAWB-1");

        checker.applyShipmentFilterRules(dto);

        verify(shipmentFullnessRuleResolver).applyShipmentFilterRules(dto);
    }

    @Test
    void applyMawbFilterRules_shouldDelegateToResolver() {
        ShipmentDto dto = new ShipmentDto();
        checker.applyMawbFilterRules(dto);
        verify(shipmentMawbRuleResolver).applyMawbFilterRules(dto);
    }
}
