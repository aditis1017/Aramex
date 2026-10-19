package com.customcharge.dashboard.util;

import com.customcharge.dashboard.constants.ChargeTypeConstants;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Resolves linked charge types that should be loaded together for the shipment charge dashboard.
 */
public final class RelatedChargeTypeResolver {

    private RelatedChargeTypeResolver() {
    }

    /**
     * Returns the requested charge type plus linked types for the given shipment type.
     * ECD links to DTC (DDU/DDO) or DTA (e.g. DDP), never both.
     */
    public static Set<String> expandChargeTypes(String requestedChargeType, String shipmentType) {
        String normalized = normalize(requestedChargeType);
        Set<String> expanded = new LinkedHashSet<>();
        expanded.add(normalized);
        expanded.addAll(linkedTypes(normalized, shipmentType));
        return expanded;
    }

    /**
     * From all charge codes present on a shipment, returns those that match the requested type
     * or its linked types for that shipment type.
     */
    public static Set<String> filterPresentChargeTypes(
            String requestedChargeType,
            String shipmentType,
            Set<String> shipmentChargeCodes) {
        Set<String> expanded = expandChargeTypes(requestedChargeType, shipmentType);
        Set<String> present = new LinkedHashSet<>();
        for (String code : shipmentChargeCodes) {
            String normalized = normalize(code);
            if (expanded.contains(normalized)) {
                present.add(normalized);
            }
        }
        return present;
    }

    private static Set<String> linkedTypes(String normalizedChargeType, String shipmentType) {
        if (ChargeTypeConstants.ECD.equals(normalizedChargeType)) {
            return Set.of(ChargeTypeConstants.revenueChargeLinkedToEcd(shipmentType));
        }
        return ChargeTypeConstants.LINKED_CHARGE_TYPES.getOrDefault(normalizedChargeType, Set.of());
    }

    public static String normalize(String chargeType) {
        return ChargeTypeConstants.normalize(chargeType);
    }
}
