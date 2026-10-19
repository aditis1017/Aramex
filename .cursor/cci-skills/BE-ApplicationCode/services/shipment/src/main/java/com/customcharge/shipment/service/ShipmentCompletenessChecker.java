package com.customcharge.shipment.service;

import com.customcharge.persistencelayer.dto.ShipmentDto;
import com.customcharge.shipment.service.rules.ShipmentFullnessRuleResolver;
import com.customcharge.shipment.service.rules.ShipmentMawbRuleResolver;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ShipmentCompletenessChecker {

    ShipmentFullnessRuleResolver shipmentFullnessRuleResolver;
    ShipmentMawbRuleResolver shipmentMawbRuleResolver;

    public void applyShipmentFilterRules(ShipmentDto shipment) {
        shipmentFullnessRuleResolver.applyShipmentFilterRules(shipment);
    }

    public void applyMawbFilterRules(ShipmentDto shipment) {
        shipmentMawbRuleResolver.applyMawbFilterRules(shipment);
    }
}
