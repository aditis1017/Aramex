package com.customcharge.dashboard.util;

import com.customcharge.persistencelayer.entities.ServiceCharge;
import com.customcharge.persistencelayer.entities.ServiceCodes;
import com.customcharge.persistencelayer.enums.AccountNature;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceCodeActiveSupportTest {

    @Test
    void resolveActive_shouldDecoupleCostFromRevenueOnEntitySettings() {
        ServiceCodes costMaster = master("ECD", false);
        ServiceCodes revenueMaster = master("DTC", true);

        assertThat(ServiceCodeActiveSupport.resolveActive(
                AccountNature.ENTITY_SETTINGS, costMaster, null)).isFalse();
        assertThat(ServiceCodeActiveSupport.resolveEditable(
                AccountNature.ENTITY_SETTINGS, costMaster)).isTrue();
        assertThat(ServiceCodeActiveSupport.resolveActive(
                AccountNature.ENTITY_SETTINGS, revenueMaster, null)).isTrue();
        assertThat(ServiceCodeActiveSupport.resolveEditable(
                AccountNature.ENTITY_SETTINGS, revenueMaster)).isTrue();
    }

    @Test
    void resolveActive_shouldDisableArWhenRevenueMasterInactive() {
        ServiceCodes revenueMaster = master("DTC", false);
        ServiceCharge charge = new ServiceCharge();
        charge.setActive(true);

        assertThat(ServiceCodeActiveSupport.resolveActive(AccountNature.AR, revenueMaster, charge))
                .isFalse();
        assertThat(ServiceCodeActiveSupport.resolveEditable(AccountNature.AR, revenueMaster))
                .isFalse();
    }

    @Test
    void resolveActive_shouldKeepArEditableWhenOnlyAccountChargeInactive() {
        ServiceCodes revenueMaster = master("DTC", true);
        ServiceCharge charge = new ServiceCharge();
        charge.setActive(false);

        assertThat(ServiceCodeActiveSupport.resolveActive(AccountNature.AR, revenueMaster, charge))
                .isFalse();
        assertThat(ServiceCodeActiveSupport.resolveEditable(AccountNature.AR, revenueMaster))
                .isTrue();
    }

    @Test
    void applyEntitySettingsActiveFlag_shouldUpdateRevenueMasterIsActive() {
        ServiceCodes master = master("DTC", true);

        assertThat(ServiceCodeActiveSupport.applyEntitySettingsActiveFlag(master, "DTC", false))
                .isTrue();
        assertThat(master.getIsActive()).isFalse();
    }

    private static ServiceCodes master(String chargeType, boolean active) {
        ServiceCodes master = new ServiceCodes();
        master.setChargeType(chargeType);
        master.setIsActive(active);
        master.setIsDeleted(false);
        return master;
    }
}
