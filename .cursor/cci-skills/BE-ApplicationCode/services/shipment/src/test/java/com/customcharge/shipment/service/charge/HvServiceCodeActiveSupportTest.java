package com.customcharge.shipment.service.charge;

import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.ServiceCharge;
import com.customcharge.persistencelayer.entities.ServiceCodes;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class HvServiceCodeActiveSupportTest {

    @Test
    void isIncludedInActiveLineItems_shouldIncludeRevenue_whenActiveAndNonBillable() {
        Account account = new Account();
        ServiceCodes master = serviceCode("REV_FEE", "DTC");
        ServiceCharge accountCharge = new ServiceCharge();
        accountCharge.setActive(true);
        accountCharge.setIsBillable(false);
        accountCharge.setServiceCodes(master);

        assertThat(HvServiceCodeActiveSupport.isIncludedInActiveLineItems(
                        account, "DTC", master, accountCharge))
                .isTrue();
    }

    @Test
    void isIncludedInActiveLineItems_shouldExclude_whenAccountChargeInactive() {
        Account account = new Account();
        ServiceCodes master = serviceCode("REV_FEE", "DTC");
        ServiceCharge accountCharge = new ServiceCharge();
        accountCharge.setActive(false);
        accountCharge.setIsBillable(true);
        accountCharge.setServiceCodes(master);

        assertThat(HvServiceCodeActiveSupport.isIncludedInActiveLineItems(
                        account, "DTC", master, accountCharge))
                .isFalse();
    }

    @Test
    void isIncludedInActiveLineItems_shouldInclude_whenNoAccountChargeConfigured() {
        Account account = new Account();
        ServiceCodes master = serviceCode("DUTY", "DTC");

        assertThat(HvServiceCodeActiveSupport.isIncludedInActiveLineItems(
                        account, "DTC", master, (ServiceCharge) null))
                .isTrue();
    }

    @Test
    void isIncludedInActiveLineItems_shouldUseEntitySettingsCharge_whenBillingChargeMissing() {
        Account account = new Account();
        ServiceCodes master = serviceCode("DUTY", "DTC");
        ServiceCharge entitySettingsCharge = new ServiceCharge();
        entitySettingsCharge.setActive(true);

        assertThat(HvServiceCodeActiveSupport.isIncludedInActiveLineItems(
                        account, "DTC", master, null, entitySettingsCharge))
                .isTrue();
    }

    @Test
    void isIncludedInActiveLineItems_shouldExclude_whenMasterDeleted() {
        Account account = new Account();
        ServiceCodes master = serviceCode("DUTY", "DTC");
        master.setIsDeleted(true);

        assertThat(HvServiceCodeActiveSupport.isIncludedInActiveLineItems(
                        account, "DTC", master, new ServiceCharge()))
                .isFalse();
    }

    @Test
    void inactiveServiceCodes_shouldCollectInactiveAccountCharges() {
        ServiceCodes master = serviceCode(" rev_fee ", "DTC");
        ServiceCharge inactive = new ServiceCharge();
        inactive.setActive(false);
        inactive.setServiceCodes(master);

        assertThat(HvServiceCodeActiveSupport.inactiveServiceCodes(List.of(inactive)))
                .containsExactly("REV_FEE");
    }

    @Test
    void inactiveServiceCodes_shouldReturnEmpty_whenCollectionNull() {
        assertThat(HvServiceCodeActiveSupport.inactiveServiceCodes(null)).isEmpty();
    }

    @Test
    void inactiveCodesFromInactiveMasters_shouldCollectInactiveMasters() {
        ServiceCodes inactive = serviceCode("DUTY", "ECD");
        inactive.setIsActive(false);

        assertThat(HvServiceCodeActiveSupport.inactiveCodesFromInactiveMasters(List.of(inactive)))
                .containsExactly("DUTY");
    }

    @Test
    void inactiveCodesFromInactiveMasters_shouldIgnoreActiveMasters() {
        ServiceCodes active = serviceCode("DUTY", "ECD");
        active.setIsActive(true);

        assertThat(HvServiceCodeActiveSupport.inactiveCodesFromInactiveMasters(List.of(active)))
                .isEmpty();
    }

    private static ServiceCodes serviceCode(String code, String chargeType) {
        ServiceCodes master = new ServiceCodes();
        ReflectionTestUtils.setField(master, "id", UUID.randomUUID());
        master.setServiceCode(code);
        master.setChargeType(chargeType);
        master.setIsActive(true);
        master.setIsDeleted(false);
        return master;
    }
}
