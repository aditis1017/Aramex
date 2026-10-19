package com.customcharge.dashboard.util;

import com.customcharge.persistencelayer.entities.ServiceCharge;
import com.customcharge.persistencelayer.entities.ServiceCodes;
import com.customcharge.persistencelayer.util.ServiceChargeTaxValueSupport;

/**
 * Maps master {@link ServiceCodes} fields onto API DTOs while keeping {@link ServiceCharge} JSON shape unchanged.
 */
public final class ServiceChargeFieldMapper {

    private ServiceChargeFieldMapper() {
    }

    public static String serviceCode(ServiceCharge charge) {
        ServiceCodes master = master(charge);
        return master != null ? master.getServiceCode() : null;
    }

    public static String serviceDescription(ServiceCharge charge) {
        ServiceCodes master = master(charge);
        return master != null ? master.getServiceDescription() : null;
    }

    public static String taxCode(ServiceCharge charge) {
        ServiceCodes master = master(charge);
        return master != null ? master.getTaxCode() : null;
    }

    public static Double taxValue(ServiceCharge charge) {
        return ServiceChargeTaxValueSupport.resolve(charge);
    }

    private static ServiceCodes master(ServiceCharge charge) {
        return charge != null ? charge.getServiceCodes() : null;
    }
}
