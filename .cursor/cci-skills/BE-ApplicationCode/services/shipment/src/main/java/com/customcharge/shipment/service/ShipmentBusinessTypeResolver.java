package com.customcharge.shipment.service;

import com.customcharge.persistencelayer.dto.ShipmentDto;
import com.customcharge.persistencelayer.entities.ARAccount;
import com.customcharge.persistencelayer.entities.Shipment;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import static com.customcharge.shipment.constants.ShipmentConstants.AR_BUSINESS_TYPE_CORPORATE;
import static com.customcharge.shipment.constants.ShipmentConstants.AR_BUSINESS_TYPE_CORPORATE_CODE;
import static com.customcharge.shipment.constants.ShipmentConstants.AR_BUSINESS_TYPE_CORPORATE_LEGACY_CODE;
import static com.customcharge.shipment.constants.ShipmentConstants.AR_BUSINESS_TYPE_E_COMMERCE_CODE;
import static com.customcharge.shipment.constants.ShipmentConstants.AR_BUSINESS_TYPE_OIL_AND_GAS;
import static com.customcharge.shipment.constants.ShipmentConstants.AR_BUSINESS_TYPE_OIL_AND_GAS_CODE;
import static com.customcharge.shipment.constants.ShipmentConstants.AR_BUSINESS_TYPE_RETAIL_CASH;
import static com.customcharge.shipment.constants.ShipmentConstants.AR_BUSINESS_TYPE_RETAIL_CASH_CODE;
import static com.customcharge.shipment.constants.ShipmentConstants.AR_BUSINESS_TYPE_WHOLESALE;
import static com.customcharge.shipment.constants.ShipmentConstants.AR_BUSINESS_TYPE_WHOLESALE_CODE;
import static com.customcharge.shipment.constants.ShipmentConstants.BUSINESS_TYPE_E_COMMERCE;
import static com.customcharge.shipment.constants.ShipmentConstants.SHIPMENT_BUSINESS_TYPE_B2B;
import static com.customcharge.shipment.constants.ShipmentConstants.SHIPMENT_BUSINESS_TYPE_B2C;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ShipmentBusinessTypeResolver {

    private static final Set<String> B2B_BUSINESS_SEGMENTS = Set.of(
            normalize(AR_BUSINESS_TYPE_CORPORATE),
            normalize(AR_BUSINESS_TYPE_CORPORATE_CODE),
            normalize(AR_BUSINESS_TYPE_CORPORATE_LEGACY_CODE),
            normalize(AR_BUSINESS_TYPE_WHOLESALE),
            normalize(AR_BUSINESS_TYPE_WHOLESALE_CODE),
            normalize(AR_BUSINESS_TYPE_OIL_AND_GAS),
            normalize(AR_BUSINESS_TYPE_OIL_AND_GAS_CODE),
            normalize(AR_BUSINESS_TYPE_RETAIL_CASH),
            normalize(AR_BUSINESS_TYPE_RETAIL_CASH_CODE));

    private static final Set<String> B2C_BUSINESS_SEGMENTS = Set.of(
            normalize(BUSINESS_TYPE_E_COMMERCE),
            normalize(AR_BUSINESS_TYPE_E_COMMERCE_CODE));

    /**
     * Resolves keyword-driven {@code shipmentBusinessType} and AR-driven {@code accountBusinessType}.
     * Sets both on the shipment entity and on the message DTO when provided.
     */
    public String resolve(Shipment shipment, ARAccount arAccount, ShipmentDto message) {
        if (shipment == null) {
            return null;
        }

        String accountBusinessType = resolveArBusinessType(arAccount).orElse(null);
        shipment.setAccountBusinessType(accountBusinessType);
        if (message != null) {
            message.setAccountBusinessType(accountBusinessType);
        }

        String shipmentBusinessType = Boolean.TRUE.equals(shipment.getB2BShipment())
                ? SHIPMENT_BUSINESS_TYPE_B2B
                : SHIPMENT_BUSINESS_TYPE_B2C;
        shipment.setShipmentBusinessType(shipmentBusinessType);
        if (message != null) {
            message.setBusinessType(shipmentBusinessType);
        }

        return shipmentBusinessType;
    }

    public Optional<String> resolveArBusinessType(ARAccount arAccount) {
        if (arAccount == null) {
            return Optional.empty();
        }
        String segmentSource = StringUtils.hasText(arAccount.getBusinessSegment())
                ? arAccount.getBusinessSegment()
                : arAccount.getBusinessType();
        return mapBusinessSegment(segmentSource);
    }

    private static Optional<String> mapBusinessSegment(String businessSegment) {
        if (!StringUtils.hasText(businessSegment)) {
            return Optional.empty();
        }
        String normalized = normalize(businessSegment);
        if (B2B_BUSINESS_SEGMENTS.contains(normalized)) {
            return Optional.of(SHIPMENT_BUSINESS_TYPE_B2B);
        }
        if (B2C_BUSINESS_SEGMENTS.contains(normalized)) {
            return Optional.of(SHIPMENT_BUSINESS_TYPE_B2C);
        }
        return Optional.empty();
    }

    private static boolean isExplicitBusinessType(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String normalized = normalize(value);
        return normalized.equals(normalize(SHIPMENT_BUSINESS_TYPE_B2B))
                || normalized.equals(normalize(SHIPMENT_BUSINESS_TYPE_B2C));
    }

    private static String toCanonicalBusinessType(String businessType) {
        if (normalize(businessType).equals(normalize(SHIPMENT_BUSINESS_TYPE_B2C))) {
            return SHIPMENT_BUSINESS_TYPE_B2C;
        }
        return SHIPMENT_BUSINESS_TYPE_B2B;
    }

    private static String normalize(String value) {
        return value.strip()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\s_-]+", "");
    }
}
