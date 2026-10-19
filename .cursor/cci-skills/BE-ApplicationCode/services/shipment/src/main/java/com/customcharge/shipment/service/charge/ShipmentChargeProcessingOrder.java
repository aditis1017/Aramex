package com.customcharge.shipment.service.charge;

import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.shipment.constants.ShipmentChargeTypeSupport;

import java.util.Comparator;
import java.util.List;

/**
 * Dependency order for charge line creation (copy chain must run in sequence).
 */
public final class ShipmentChargeProcessingOrder {

    private static final List<String> DEPENDENCY_ORDER =
            List.of("ECD", "DTA", "DTC", "ECS", "EC3", "DTS", "DT3");

    private static final String PAYMENT_PREPAID = "P";
    private static final String PAYMENT_THIRD_PARTY = "3";

    private ShipmentChargeProcessingOrder() {
    }

    public static List<Charge> sort(List<Charge> charges) {
        return charges.stream()
                .sorted(Comparator.comparingInt(charge -> dependencyIndex(charge.getChargeCode())))
                .toList();
    }

    public static boolean shouldCreateLineItems(Shipment shipment, String chargeCode) {
        if (shipment == null || chargeCode == null || chargeCode.isBlank()) {
            return false;
        }
        String paymentType = shipment.getPaymentType() == null ? "" : shipment.getPaymentType().strip();
        return switch (ShipmentChargeTypeSupport.normalize(chargeCode)) {
            case ShipmentChargeTypeSupport.ECS, ShipmentChargeTypeSupport.DTS ->
                    PAYMENT_PREPAID.equalsIgnoreCase(paymentType);
            case ShipmentChargeTypeSupport.EC3, ShipmentChargeTypeSupport.DT3 ->
                    PAYMENT_THIRD_PARTY.equals(paymentType);
            default -> true;
        };
    }

    private static int dependencyIndex(String chargeCode) {
        String normalized = ShipmentChargeTypeSupport.normalize(chargeCode);
        int index = DEPENDENCY_ORDER.indexOf(normalized);
        return index >= 0 ? index : Integer.MAX_VALUE;
    }
}
