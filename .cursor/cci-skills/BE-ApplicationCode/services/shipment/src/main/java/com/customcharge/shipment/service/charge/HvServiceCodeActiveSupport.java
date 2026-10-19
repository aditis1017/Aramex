package com.customcharge.shipment.service.charge;

import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.ServiceCharge;
import com.customcharge.persistencelayer.entities.ServiceCodes;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Activation rules for service-code masters and account {@link ServiceCharge} rows on HV/Exempted flows.
 * Revenue charges do not filter on {@code isBillable} (aligned with LV rate-sheet charge creation).
 */
public final class HvServiceCodeActiveSupport {

    private HvServiceCodeActiveSupport() {
    }

    public static boolean isIncludedInActiveLineItems(
            Account account,
            String requestedChargeType,
            ServiceCodes master,
            ServiceCharge charge) {
        return isIncludedInActiveLineItems(
                account, requestedChargeType, master, charge, null);
    }

    public static boolean isIncludedInActiveLineItems(
            Account account,
            String requestedChargeType,
            ServiceCodes master,
            ServiceCharge billingAccountCharge,
            ServiceCharge entitySettingsCharge) {
        if (master == null || Boolean.TRUE.equals(master.getIsDeleted())) {
            return false;
        }
        if (!ShipmentServiceCodeEligibilitySupport.isMasterEligibleForAccount(account, master)) {
            return false;
        }
        return isAccountChargeActive(billingAccountCharge, entitySettingsCharge);
    }

    private static boolean isAccountChargeActive(
            ServiceCharge billingAccountCharge, ServiceCharge entitySettingsCharge) {
        ServiceCharge configuredCharge = billingAccountCharge != null
                ? billingAccountCharge
                : entitySettingsCharge;
        if (configuredCharge == null) {
            return true;
        }
        return Boolean.TRUE.equals(configuredCharge.getActive());
    }

    /**
     * Normalized service codes with an account {@link ServiceCharge} row where {@code active=false}.
     */
    public static Set<String> inactiveServiceCodes(Collection<ServiceCharge> accountCharges) {
        Set<String> inactive = new LinkedHashSet<>();
        if (accountCharges == null) {
            return inactive;
        }
        for (ServiceCharge charge : accountCharges) {
            if (charge == null || Boolean.TRUE.equals(charge.getActive())) {
                continue;
            }
            ServiceCodes master = charge.getServiceCodes();
            if (master == null || !StringUtils.hasText(master.getServiceCode())) {
                continue;
            }
            inactive.add(ChargeServiceCodeSupport.normalize(master.getServiceCode()));
        }
        return inactive;
    }

    /**
     * Normalized service codes whose {@link ServiceCodes} master row has {@code isActive=false}.
     */
    public static Set<String> inactiveCodesFromInactiveMasters(Collection<ServiceCodes> masters) {
        Set<String> inactive = new LinkedHashSet<>();
        if (masters == null) {
            return inactive;
        }
        for (ServiceCodes master : masters) {
            if (master == null
                    || Boolean.TRUE.equals(master.getIsDeleted())
                    || Boolean.TRUE.equals(master.getIsActive())
                    || !StringUtils.hasText(master.getServiceCode())) {
                continue;
            }
            inactive.add(ChargeServiceCodeSupport.normalize(master.getServiceCode()));
        }
        return inactive;
    }
}
