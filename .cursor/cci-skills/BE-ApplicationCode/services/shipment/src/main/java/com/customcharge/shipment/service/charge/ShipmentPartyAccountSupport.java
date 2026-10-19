package com.customcharge.shipment.service.charge;

import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.shipment.constants.ShipmentChargeTypeSupport;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * Maps revenue charge types to the shipment party account number used for AR billing lookup
 * (aligned with calculator {@code ChargeLineSupport#resolvePartyAccountNumber}).
 */
public final class ShipmentPartyAccountSupport {

    private ShipmentPartyAccountSupport() {
    }

    public static Optional<String> resolvePartyAccountNumber(Shipment shipment, String chargeCode) {
        if (shipment == null || !StringUtils.hasText(chargeCode)) {
            return Optional.empty();
        }
        if (DomesticShipmentSupport.usesRuhDduChargeBilling(shipment, chargeCode)
                && ShipmentChargeTypeSupport.DTC.equals(ShipmentChargeTypeSupport.normalize(chargeCode))) {
            String shipperAccountNumber = blankToEmpty(shipment.getShipperNumber());
            return StringUtils.hasText(shipperAccountNumber) ? Optional.of(shipperAccountNumber) : Optional.empty();
        }

        String accountNumber = switch (ShipmentChargeTypeSupport.normalize(chargeCode)) {
            case ShipmentChargeTypeSupport.DTS -> blankToEmpty(shipment.getShipperNumber());
            case ShipmentChargeTypeSupport.DT3 -> blankToEmpty(shipment.getThirdPartyNumber());
            default -> blankToEmpty(shipment.getConsigneeNumber());
        };
        return StringUtils.hasText(accountNumber) ? Optional.of(accountNumber) : Optional.empty();
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value.strip();
    }
}
