package com.customcharge.shipment.service.charge;

import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.util.DomEntityConfigResolver;
import com.customcharge.shipment.constants.ShipmentChargeTypeSupport;

import java.util.Locale;
import java.util.Set;

import static com.customcharge.shipment.constants.ShipmentConstants.SHIPMENT_TYPE_DDO;
import static com.customcharge.shipment.constants.ShipmentConstants.SHIPMENT_TYPE_DDU;

/**
 * Domestic ({@code productGroup = DOM}) shipment rules: DDU/DDO ECD and DTC charges bill at the
 * configured DOM entity (from consignee country) using the shipper account for DTC party AR lookup.
 */
public final class DomesticShipmentSupport {

    public static final String PRODUCT_GROUP_DOM = "DOM";

    private static final Set<String> DOM_BILLING_CHARGE_CODES =
            Set.of(ShipmentChargeTypeSupport.ECD, ShipmentChargeTypeSupport.DTC);

    private DomesticShipmentSupport() {
    }

    public static boolean isDomesticProductGroup(String productGroup) {
        return productGroup != null && PRODUCT_GROUP_DOM.equalsIgnoreCase(productGroup.strip());
    }

    public static boolean isDduOrDdoShipment(String shipmentType) {
        if (shipmentType == null || shipmentType.isBlank()) {
            return false;
        }
        String normalized = shipmentType.strip().toUpperCase(Locale.ROOT);
        return SHIPMENT_TYPE_DDU.equals(normalized) || SHIPMENT_TYPE_DDO.equals(normalized);
    }

    public static boolean usesRuhDduChargeBilling(Shipment shipment, String chargeCode) {
        if (shipment == null || !isDomesticProductGroup(shipment.getProductGroup())) {
            return false;
        }
        if (!isDduOrDdoShipment(shipment.getShipmentType())) {
            return false;
        }
        return DOM_BILLING_CHARGE_CODES.contains(ShipmentChargeTypeSupport.normalize(chargeCode));
    }

    public static String resolveDomEntityCode(String consigneeCountryCode) {
        return DomEntityConfigResolver.resolveConfiguredDomEntityCode(consigneeCountryCode);
    }

    public static String resolveBillingEntityCode(Shipment shipment, String chargeCode) {
        if (usesRuhDduChargeBilling(shipment, chargeCode)) {
            return resolveDomEntityCode(shipment.getConsigneeCountryCode());
        }
        return "";
    }

    public static String resolveDtcPartyAccountNumberForAccountType(Shipment shipment) {
        if (usesRuhDduChargeBilling(shipment, ShipmentChargeTypeSupport.DTC)) {
            return blankToEmpty(shipment.getShipperNumber());
        }
        return blankToEmpty(shipment.getConsigneeNumber());
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value.strip();
    }
}
