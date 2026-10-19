package com.customcharge.persistencelayer.util;

import com.customcharge.persistencelayer.entities.ServiceCharge;
import com.customcharge.persistencelayer.entities.ServiceCodes;

public final class ServiceChargeTaxValueSupport {

    private ServiceChargeTaxValueSupport() {
    }

    public static Double resolve(ServiceCharge charge) {
        if (charge == null) {
            return 0.0;
        }
        if (charge.getTaxValue() != null) {
            return charge.getTaxValue();
        }
        ServiceCodes master = charge.getServiceCodes();
        if (master == null || !Boolean.TRUE.equals(master.getFetchTax())) {
            return 0.0;
        }
        if (master.getEntity() == null || master.getEntity().getTaxValue() == null) {
            return 0.0;
        }
        return master.getEntity().getTaxValue();
    }
}
