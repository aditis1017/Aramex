package com.customcharge.shipment.observability;

import com.customcharge.persistencelayer.dto.ShipmentDto;
import org.springframework.util.StringUtils;

/**
 * Resolves destination entity for metrics/tracking when XML omits {@code HAWBDestinationEntity} on details.
 */
public final class ShipmentDestinationEntitySupport {

    private ShipmentDestinationEntitySupport() {
    }

    public static String resolve(ShipmentDto message) {
        if (message == null) {
            return "unknown";
        }
        return ShipmentIngestMetrics.normalize(firstNonBlank(
                message.getDestinationEntity(),
                message.getHawbOriginEntity(),
                message.getDestinationLocationCode(),
                message.getConsigneeCountryCode(),
                message.getThirdPartyEntity()));
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }
}
