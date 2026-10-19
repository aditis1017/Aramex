package com.customcharge.dashboard.constants;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Canonical charge-type codes and groupings used across dashboard mappers, exports, and account seeding.
 */
public final class ChargeTypeConstants {

    public static final String ECD = "ECD";
    public static final String ECS = "ECS";
    public static final String EC3 = "EC3";
    public static final String DTS = "DTS";
    public static final String DT3 = "DT3";
    public static final String DTA = "DTA";
    public static final String DTC = "DTC";

    public static final Set<String> COST_AP = Set.of(ECD, ECS, EC3);
    public static final Set<String> ECS_EC3 = Set.of(ECS, EC3);
    public static final Set<String> ECD_ONLY = Set.of(ECD);
    public static final Set<String> REVENUE_AR = Set.of(DTC, DTA, DTS, DT3);
    /** Grid account from this charge's line-item AP number (ECD / ECS / EC3). */
    public static final Set<String> GRID_ACCOUNT_FROM_AP = Set.of(ECD, ECS, EC3);
    public static final Set<String> ENTITY_SETTINGS_COST = COST_AP;
    public static final Set<String> ENTITY_SETTINGS_REVENUE = REVENUE_AR;

    public static final List<String> ALL_SAMPLE_CHARGE_TYPES =
            List.of(ECS, ECD, EC3, DTS, DT3, DTA, DTC);

    /** Display order for shipment charge details API: ECD → DTC/DTA, ECS → DTS, EC3 → DT3. */
    public static final List<String> CHARGE_DETAILS_DISPLAY_ORDER =
            List.of(ECD, DTC, DTA, ECS, DTS, EC3, DT3);

    public static final String CHARGE_GROUP_COST_AP = "COST AP";
    public static final String CHARGE_GROUP_REVENUE_AR = "REVENUE AR";

    /**
     * Static linked types. ECD→DTA/DTC is resolved from {@link #revenueChargeLinkedToEcd(String)} instead.
     */
    public static final Map<String, Set<String>> LINKED_CHARGE_TYPES = Map.of(
            DTA, Set.of(ECD),
            DTC, Set.of(ECD),
            DTS, Set.of(ECS),
            ECS, Set.of(DTS),
            DT3, Set.of(EC3),
            EC3, Set.of(DT3)
    );

    public static final Set<String> DDU_SHIPMENT_TYPES = Set.of("DDU", "DDO");

    /**
     * Revenue charge paired with ECD: DTC for DDU/DDO shipments, DTA otherwise (e.g. DDP).
     * Aligns with {@code ShipmentTypeChargeCodes} in the shipment service.
     */
    public static String revenueChargeLinkedToEcd(String shipmentType) {
        String normalized = shipmentType == null ? "" : shipmentType.trim().toUpperCase(Locale.ROOT);
        if (DDU_SHIPMENT_TYPES.contains(normalized)) {
            return DTC;
        }
        return DTA;
    }

    /**
     * Revenue charge paired with a cost charge for shipment charge-details display mirroring.
     */
    public static Optional<String> pairedRevenueChargeType(String costChargeType, String shipmentType) {
        return switch (normalize(costChargeType)) {
            case ECD -> Optional.of(revenueChargeLinkedToEcd(shipmentType));
            case ECS -> Optional.of(DTS);
            case EC3 -> Optional.of(DT3);
            default -> Optional.empty();
        };
    }

    /**
     * When mirroring revenue actuals onto cost rows for display, use {@code preConvertedAmount}
     * from DTA/DTS/DT3 line items. DTC is copied as-is from {@code amount}.
     */
    public static boolean requiresPreConvertedAmountWhenMirroringActuals(String pairedRevenueType) {
        return Set.of(DTA, DTS, DT3).contains(normalize(pairedRevenueType));
    }

    private ChargeTypeConstants() {
    }

    public static String normalize(String chargeType) {
        if (chargeType == null || chargeType.isBlank()) {
            return "";
        }
        return chargeType.trim().toUpperCase(Locale.ROOT);
    }

    public static boolean isCostAp(String chargeCode) {
        return chargeCode != null && COST_AP.contains(normalize(chargeCode));
    }

    public static boolean isRevenueAr(String chargeCode) {
        return chargeCode != null && REVENUE_AR.contains(normalize(chargeCode));
    }

    public static boolean isEcsOrEc3(String chargeType) {
        return chargeType != null && ECS_EC3.contains(normalize(chargeType));
    }

    public static int chargeDetailsDisplayIndex(String chargeType) {
        int index = CHARGE_DETAILS_DISPLAY_ORDER.indexOf(normalize(chargeType));
        return index >= 0 ? index : Integer.MAX_VALUE;
    }

    /**
     * Returns only the requested charge type (no cost/revenue composite merging).
     */
    public static List<String> resolveMasterChargeTypes(String requestedChargeType) {
        return List.of(normalize(requestedChargeType));
    }
}
