package com.customcharge.shipment.service.charge;

import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.shipment.constants.ShipmentChargeTypeSupport;
import com.customcharge.shipment.constants.ShipmentConstants;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.customcharge.shipment.constants.ShipmentConstants.CHARGE_STATUS_EXEMPTED;
import static com.customcharge.shipment.constants.ShipmentConstants.DUE;
import static com.customcharge.shipment.constants.ShipmentConstants.PENDING_CALCULATION;

/**
 * Category helpers shared by charge line creation and CIF recalculation.
 */
public final class ChargeCategorySupport {

    private ChargeCategorySupport() {
    }

    public static String captureRateSheetCategory(Shipment shipment) {
        if (shipment == null) {
            return ShipmentConstants.CATEGORY_TYPE_HV;
        }
        if (isLowValue(shipment)) {
            return ShipmentConstants.CATEGORY_TYPE_LV;
        }
        if (!StringUtils.hasText(shipment.getCategoryType())) {
            return ShipmentConstants.CATEGORY_TYPE_HV;
        }
        return shipment.getCategoryType().strip();
    }

    public static boolean isLowValue(Shipment shipment) {
        if (shipment == null) {
            return false;
        }
        return isLowValueToken(shipment.getCategoryType()) || isLowValueToken(shipment.getProductType());
    }

    public static boolean isExempted(String categoryType) {
        if (!StringUtils.hasText(categoryType)) {
            return false;
        }
        return ShipmentConstants.CATEGORY_TYPE_EXEMPTED.equalsIgnoreCase(categoryType.strip());
    }

    public static boolean isHv(String categoryType) {
        if (!StringUtils.hasText(categoryType)) {
            return true;
        }
        return ShipmentConstants.CATEGORY_TYPE_HV.equalsIgnoreCase(categoryType.strip());
    }

    public static boolean isHvOrExempted(String categoryType) {
        return isHv(categoryType) || isExempted(categoryType);
    }

    public static boolean isLvToHvTransition(String previousCategoryType, String resolvedCategoryType) {
        return isLowValueToken(previousCategoryType) && isHv(resolvedCategoryType);
    }

    public static String chargeStatusForCategory(String categoryType) {
        return isExempted(categoryType) ? CHARGE_STATUS_EXEMPTED : PENDING_CALCULATION;
    }

    /**
     * Applies shipment-level charge statuses after all line items and header amounts are calculated.
     * ECD estimate drives ECD/DTC/DTA; ECS drives ECS/DTS; EC3 drives EC3/DT3.
     */
    public static void applyShipmentChargeStatuses(Shipment shipment, Collection<Charge> charges) {
        if (charges == null || charges.isEmpty()) {
            return;
        }
        String categoryType = shipment != null ? shipment.getCategoryType() : null;
        if (isExempted(categoryType)) {
            for (Charge charge : charges) {
                if (charge != null) {
                    charge.setStatus(CHARGE_STATUS_EXEMPTED);
                }
            }
            return;
        }
        if (!isDduOrDdp(shipment != null ? shipment.getShipmentType() : null)) {
            return;
        }

        Map<String, Charge> chargesByCode = indexChargesByCode(charges);
        applyDriverGroup(chargesByCode, ShipmentChargeTypeSupport.ECD,
                ShipmentChargeTypeSupport.DTC, ShipmentChargeTypeSupport.DTA);
        applyDriverGroup(chargesByCode, ShipmentChargeTypeSupport.ECS, ShipmentChargeTypeSupport.DTS);
        applyDriverGroup(chargesByCode, ShipmentChargeTypeSupport.EC3, ShipmentChargeTypeSupport.DT3);
    }

    public static String statusFromEstimateAmount(BigDecimal estimateAmount) {
        if (estimateAmount == null || estimateAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return PENDING_CALCULATION;
        }
        return DUE;
    }

    private static void applyDriverGroup(
            Map<String, Charge> chargesByCode, String driverCode, String... followerCodes) {
        Charge driverCharge = chargesByCode.get(ShipmentChargeTypeSupport.normalize(driverCode));
        if (driverCharge == null) {
            return;
        }
        String status = statusFromEstimateAmount(driverCharge.getEstimateAmount());
        driverCharge.setStatus(status);
        for (String followerCode : followerCodes) {
            Charge followerCharge = chargesByCode.get(ShipmentChargeTypeSupport.normalize(followerCode));
            if (followerCharge != null) {
                followerCharge.setStatus(status);
            }
        }
    }

    private static Map<String, Charge> indexChargesByCode(Collection<Charge> charges) {
        Map<String, Charge> indexed = new LinkedHashMap<>();
        for (Charge charge : charges) {
            if (charge == null || !StringUtils.hasText(charge.getChargeCode())) {
                continue;
            }
            indexed.putIfAbsent(ShipmentChargeTypeSupport.normalize(charge.getChargeCode()), charge);
        }
        return indexed;
    }

    private static boolean isDduOrDdp(String shipmentType) {
        if (!StringUtils.hasText(shipmentType)) {
            return false;
        }
        String normalized = shipmentType.strip().toUpperCase();
        return ShipmentConstants.SHIPMENT_TYPE_DDU.equals(normalized)
                || ShipmentConstants.SHIPMENT_TYPE_DDP.equals(normalized);
    }

    private static boolean isLowValueToken(String value) {
        return StringUtils.hasText(value)
                && ShipmentConstants.CATEGORY_TYPE_LV.equalsIgnoreCase(value.strip());
    }
}
