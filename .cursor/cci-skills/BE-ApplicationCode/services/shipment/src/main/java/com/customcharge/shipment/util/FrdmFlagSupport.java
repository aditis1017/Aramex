package com.customcharge.shipment.util;

import org.springframework.util.StringUtils;

public final class FrdmFlagSupport {

    public static final String FRDM_FLAG_YES = "Yes";
    public static final String FRDM_FLAG_NO = "No";
    private static final String FRDM_SERVICE_CODE = "FRDM";

    private FrdmFlagSupport() {
    }

    public static String resolve(String services) {
        if (!StringUtils.hasText(services)) {
            return FRDM_FLAG_NO;
        }
        for (String serviceCode : services.split(",")) {
            if (FRDM_SERVICE_CODE.equalsIgnoreCase(serviceCode.trim())) {
                return FRDM_FLAG_YES;
            }
        }
        return FRDM_FLAG_NO;
    }
}
