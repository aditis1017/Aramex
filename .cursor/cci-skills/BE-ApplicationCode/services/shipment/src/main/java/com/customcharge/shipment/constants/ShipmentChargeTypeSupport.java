package com.customcharge.shipment.constants;

import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.shipment.service.charge.DomesticShipmentSupport;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Charge-type groupings for shipment service-charge line items (aligned with dashboard {@code ChargeTypeConstants}).
 */
public final class ShipmentChargeTypeSupport {

    public static final String ECD = "ECD";
    public static final String ECS = "ECS";
    public static final String EC3 = "EC3";
    public static final String DTS = "DTS";
    public static final String DT3 = "DT3";
    public static final String DTA = "DTA";
    public static final String DTC = "DTC";

    public static final String ETS = "ETS";
    public static final String ES3 = "ES3";

    private static final Set<String> ENTITY_SETTINGS_COST = Set.of(ECD, ECS, EC3);
    private static final Set<String> REVENUE_AR = Set.of(DTC, DTA, DTS, DT3);
    private static final Set<String> ORIGIN_ENTITY_CHARGE_CODES = Set.of(ECS, DTS);
    private static final Set<String> THIRD_PARTY_ENTITY_CHARGE_CODES = Set.of(EC3, DT3);
    /** Currency from destination country lookup (currency table). */
    private static final Set<String> DESTINATION_COUNTRY_CURRENCY_CODES = Set.of(ECD, DTC);
    /** Fixed USD for these revenue / cost types. */
    private static final Set<String> USD_CURRENCY_CODES = Set.of(DTA, ECS, EC3);
    /** Currency from shipper country lookup (currency table). */
    private static final Set<String> SHIPPER_COUNTRY_CURRENCY_CODES = Set.of(DTS);
    /** Currency from third-party country lookup (currency table). */
    private static final Set<String> THIRD_PARTY_COUNTRY_CURRENCY_CODES = Set.of(DT3);
    /** CIF snapshot currency from destination country lookup. */
    private static final Set<String> CIF_DESTINATION_COUNTRY_CURRENCY_CODES = Set.of(ECD, DTC, DTA);
    /** CIF snapshot currency from shipper country lookup. */
    private static final Set<String> CIF_SHIPPER_COUNTRY_CURRENCY_CODES = Set.of(ECS, DTS);
    /** CIF snapshot currency from third-party country lookup. */
    private static final Set<String> CIF_THIRD_PARTY_COUNTRY_CURRENCY_CODES = Set.of(DT3);

    private ShipmentChargeTypeSupport() {
    }

    public static String normalize(String chargeType) {
        if (chargeType == null || chargeType.isBlank()) {
            return "";
        }
        return chargeType.trim().toUpperCase(Locale.ROOT);
    }

    public static boolean usesEntitySettingsAccountOnly(String chargeCode) {
        return chargeCode != null && ENTITY_SETTINGS_COST.contains(normalize(chargeCode));
    }

    public static boolean isRevenueCharge(String chargeCode) {
        return chargeCode != null && REVENUE_AR.contains(normalize(chargeCode));
    }

    public static boolean usesOriginEntity(String chargeCode) {
        return chargeCode != null && ORIGIN_ENTITY_CHARGE_CODES.contains(normalize(chargeCode));
    }

    public static boolean usesThirdPartyEntity(String chargeCode) {
        return chargeCode != null && THIRD_PARTY_ENTITY_CHARGE_CODES.contains(normalize(chargeCode));
    }

    public static boolean usesDestinationCountryCurrency(String chargeCode) {
        return chargeCode != null && DESTINATION_COUNTRY_CURRENCY_CODES.contains(normalize(chargeCode));
    }

    public static boolean usesUsdCurrency(String chargeCode) {
        return chargeCode != null && USD_CURRENCY_CODES.contains(normalize(chargeCode));
    }

    public static boolean usesShipperCountryCurrency(String chargeCode) {
        return chargeCode != null && SHIPPER_COUNTRY_CURRENCY_CODES.contains(normalize(chargeCode));
    }

    public static boolean usesThirdPartyCountryCurrency(String chargeCode) {
        return chargeCode != null && THIRD_PARTY_COUNTRY_CURRENCY_CODES.contains(normalize(chargeCode));
    }

    public static boolean usesCifDestinationCountryCurrency(String chargeCode) {
        return chargeCode != null && CIF_DESTINATION_COUNTRY_CURRENCY_CODES.contains(normalize(chargeCode));
    }

    public static boolean usesCifShipperCountryCurrency(String chargeCode) {
        return chargeCode != null && CIF_SHIPPER_COUNTRY_CURRENCY_CODES.contains(normalize(chargeCode));
    }

    public static boolean usesCifThirdPartyCountryCurrency(String chargeCode) {
        return chargeCode != null && CIF_THIRD_PARTY_COUNTRY_CURRENCY_CODES.contains(normalize(chargeCode));
    }

    public static Optional<String> copySourceChargeCode(String chargeCode) {
        return switch (normalize(chargeCode)) {
            case DTC -> Optional.of(ECD);
            case DTA -> Optional.of(ECD);
            case DTS -> Optional.of(ECS);
            case DT3 -> Optional.of(EC3);
            default -> Optional.empty();
        };
    }

    /** Cost (ECS/ECD/EC3) before revenue (DTS/DT3/DTA/DTC) when creating LV line items. */
    public static int processingOrder(String chargeCode) {
        if (usesEntitySettingsAccountOnly(chargeCode)) {
            return 0;
        }
        if (isRevenueCharge(chargeCode)) {
            return 1;
        }
        return 2;
    }

    /**
     * Entity used for account, service-code, and rate-sheet lookup when creating or calculating charges.
     */
    public static String resolveBillingEntityCode(Shipment shipment, String chargeCode) {
        if (shipment == null) {
            return "";
        }
        String domesticBillingEntity = DomesticShipmentSupport.resolveBillingEntityCode(shipment, chargeCode);
        if (!domesticBillingEntity.isBlank()) {
            return domesticBillingEntity;
        }
        if (usesThirdPartyEntity(chargeCode)) {
            return blankToEmpty(shipment.getThirdPartyEntity());
        }
        if (usesOriginEntity(chargeCode)) {
            return blankToEmpty(shipment.getOriginEntity());
        }
        return blankToEmpty(shipment.getHAWBDestinationEntity());
    }

    /**
     * Returns only the requested charge type (no cost/revenue composite merging).
     */
    public static List<String> resolveMasterChargeTypes(String chargeCode) {
        return List.of(normalize(chargeCode));
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value.strip();
    }
}
