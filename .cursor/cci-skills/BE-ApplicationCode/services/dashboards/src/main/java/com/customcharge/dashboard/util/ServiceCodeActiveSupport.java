package com.customcharge.dashboard.util;

import com.customcharge.dashboard.constants.ChargeTypeConstants;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.ServiceCharge;
import com.customcharge.persistencelayer.entities.ServiceCodes;
import com.customcharge.persistencelayer.enums.AccountNature;

/**
 * Activation flags on {@link ServiceCodes} and derived {@code active} / {@code editable}
 * for service-charge fetch. Each master row is independent per charge type.
 */
public final class ServiceCodeActiveSupport {

    private ServiceCodeActiveSupport() {
    }

    /**
     * Whether a master (and optional account charge) is active enough for rate-sheet dropdowns and import.
     */
    public static boolean isIncludedInActiveDropdown(
            Account account,
            String requestedChargeType,
            ServiceCodes master,
            ServiceCharge charge) {
        AccountNature accountNature = account != null ? account.getAccountNature() : null;
        return resolveActive(accountNature, master, charge);
    }

    /**
     * Whether a row can be edited on service-charge fetch.
     * Entity settings rows stay editable so inactive codes can be turned back on.
     * AR rows: editable only when the master for the requested entity and charge type is active.
     */
    public static boolean resolveEditable(AccountNature accountNature, ServiceCodes master) {
        if (master == null || Boolean.TRUE.equals(master.getIsDeleted())) {
            return false;
        }
        if (AccountNature.AR.equals(accountNature)) {
            return Boolean.TRUE.equals(master.getIsActive());
        }
        return true;
    }

    /**
     * Effective active flag for fetch responses (entity + charge type scoped).
     * AR: master inactive → false; master active and no charge → true;
     * master active and charge present → charge {@code active}.
     */
    public static boolean resolveActive(
            AccountNature accountNature,
            ServiceCodes master,
            ServiceCharge charge) {
        if (master == null || Boolean.TRUE.equals(master.getIsDeleted())) {
            return false;
        }
        if (!Boolean.TRUE.equals(master.getIsActive())) {
            return false;
        }
        if (charge == null) {
            return true;
        }
        return Boolean.TRUE.equals(charge.getActive());
    }

    /**
     * @return {@code true} when a master flag was updated
     */
    public static boolean applyEntitySettingsActiveFlag(
            ServiceCodes master, String accountChargeType, Boolean active) {
        if (master == null || active == null) {
            return false;
        }
        String chargeType = ChargeTypeConstants.normalize(accountChargeType);
        if (ChargeTypeConstants.ENTITY_SETTINGS_COST.contains(chargeType)
                || ChargeTypeConstants.ENTITY_SETTINGS_REVENUE.contains(chargeType)) {
            master.setIsActive(active);
            return true;
        }
        return false;
    }
}
