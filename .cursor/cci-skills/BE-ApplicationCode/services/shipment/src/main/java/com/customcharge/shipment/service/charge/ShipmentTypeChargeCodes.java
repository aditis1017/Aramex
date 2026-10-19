package com.customcharge.shipment.service.charge;

import java.util.List;

/**
 * Maps resolved shipment type (e.g. DDU, DDP) to the charge type codes stored on {@code Charge.chargeCode}.
 */
public final class ShipmentTypeChargeCodes {

    private static final List<String> DDU_CHARGE_CODES = List.of("ECD", "DTC");
    private static final List<String> DDP_CHARGE_CODES = List.of("ECD", "DTA", "DT3", "DTS");

    private ShipmentTypeChargeCodes() {
    }

    /**
     * Resolves charge codes for a shipment type. For DDP, {@code paymentType} (PaymentType/CMT) selects
     * which charges are created: P → ECS, DTS; 3 → EC3, DT3.
     */
    public static List<String> codesForShipment(String shipmentTypeCode, String paymentType) {
        if (shipmentTypeCode == null || shipmentTypeCode.isBlank()) {
            return List.of();
        }
        return switch (shipmentTypeCode.trim().toUpperCase()) {
            case "DDU", "DDO" -> List.of("ECD", "DTC");
            case "DDP" -> codesForDdp(paymentType);
            default -> List.of();
        };
    }

    private static List<String> codesForDdp(String paymentType) {
        if (paymentType == null || paymentType.isBlank()) {
            return List.of();
        }
        return switch (paymentType.trim().toUpperCase()) {
            case "P" -> List.of("ECS", "ECD", "DTS", "DTA");
            case "3" -> List.of("EC3", "ECD", "DT3", "DTA");
            default -> List.of();
        };
    }
}
