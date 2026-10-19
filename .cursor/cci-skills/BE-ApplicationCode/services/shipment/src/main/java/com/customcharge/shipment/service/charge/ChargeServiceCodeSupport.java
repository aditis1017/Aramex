package com.customcharge.shipment.service.charge;

import com.customcharge.persistencelayer.entities.RateSheetItem;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Optional;

public final class ChargeServiceCodeSupport {

    private ChargeServiceCodeSupport() {
    }

    public static String normalize(String serviceCode) {
        if (!StringUtils.hasText(serviceCode)) {
            return "";
        }
        return serviceCode.strip().toUpperCase(Locale.ROOT);
    }

    /**
     * Resolves the service-code key used to match rate sheet rows to charge line items.
     */
    public static Optional<String> fromRateSheetItem(RateSheetItem item) {
        if (item == null) {
            return Optional.empty();
        }
        if (StringUtils.hasText(item.getServiceCode())) {
            return Optional.of(item.getServiceCode().strip());
        }
        return Optional.empty();
    }
}
