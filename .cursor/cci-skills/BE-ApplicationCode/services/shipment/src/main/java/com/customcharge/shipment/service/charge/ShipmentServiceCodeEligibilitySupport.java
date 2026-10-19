package com.customcharge.shipment.service.charge;

import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.ServiceCodes;
import com.customcharge.persistencelayer.enums.AccountNature;
import com.customcharge.persistencelayer.util.ServiceCodeMasterActiveSupport;

/**
 * Service-code master eligibility for shipment charge flows (AR consignee vs entity settings).
 */
public final class ShipmentServiceCodeEligibilitySupport {

    private ShipmentServiceCodeEligibilitySupport() {
    }

    public static boolean isArAccount(Account account) {
        return account != null && AccountNature.AR.equals(account.getAccountNature());
    }

    public static boolean isMasterEligibleForAccount(Account account, ServiceCodes master) {
        if (master == null || Boolean.TRUE.equals(master.getIsDeleted())) {
            return false;
        }
        return ServiceCodeMasterActiveSupport.isMasterActive(master);
    }
}
