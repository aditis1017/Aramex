package com.customcharge.shipment.service.cif;

import org.springframework.util.StringUtils;

public final class ChargeActualizedStatusSupport {

    private static final String ACTUALIZED = "ACTUALIZED";

    private ChargeActualizedStatusSupport() {
    }

    public static boolean isActualized(String status) {
        if (!StringUtils.hasText(status)) {
            return false;
        }
        String normalized = status.trim().toUpperCase().replace(' ', '_');
        return ACTUALIZED.equals(normalized);
    }
}
