package com.customcharge.persistencelayer.util;

import com.customcharge.persistencelayer.entities.ServiceCharge;
import com.customcharge.persistencelayer.entities.ServiceCodes;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceChargeTaxResolutionSupportTest {

    @Test
    void resolve_shouldPreferChargeTaxValue() {
        ServiceCharge charge = new ServiceCharge();
        charge.setTaxValue(12.0);
        ServiceCodes master = new ServiceCodes();
        master.setFetchTax(true);

        ServiceChargeTaxResolutionSupport.ResolvedTax tax =
                ServiceChargeTaxResolutionSupport.resolve(charge, master, 15.0);

        assertThat(tax.taxValue()).isEqualTo(12.0);
        assertThat(tax.taxCode()).isEqualTo("TAX-12");
    }

    @Test
    void resolve_shouldUseEntityTaxWhenFetchTaxEnabledAndNoChargeValue() {
        ServiceCodes master = new ServiceCodes();
        master.setFetchTax(true);

        ServiceChargeTaxResolutionSupport.ResolvedTax tax =
                ServiceChargeTaxResolutionSupport.resolve(null, master, 15.0);

        assertThat(tax.taxValue()).isEqualTo(15.0);
        assertThat(tax.taxCode()).isEqualTo("TAX-15");
    }

    @Test
    void resolve_shouldReturnOosWhenFetchTaxDisabledAndNoChargeValue() {
        ServiceCodes master = new ServiceCodes();
        master.setFetchTax(false);
        master.setTaxCode("VAT0");

        ServiceChargeTaxResolutionSupport.ResolvedTax tax =
                ServiceChargeTaxResolutionSupport.resolve(null, master, 15.0);

        assertThat(tax.taxValue()).isEqualTo(0.0);
        assertThat(tax.taxCode()).isEqualTo("OOS");
    }

    @Test
    void resolve_shouldReturnOosWhenChargeTaxValueIsZero() {
        ServiceCharge charge = new ServiceCharge();
        charge.setTaxValue(0.0);

        ServiceChargeTaxResolutionSupport.ResolvedTax tax =
                ServiceChargeTaxResolutionSupport.resolve(charge, null, 15.0);

        assertThat(tax.taxValue()).isEqualTo(0.0);
        assertThat(tax.taxCode()).isEqualTo("OOS");
    }

    @Test
    void resolve_shouldReturnOosWhenEntityTaxValueIsZero() {
        ServiceCodes master = new ServiceCodes();
        master.setFetchTax(true);

        ServiceChargeTaxResolutionSupport.ResolvedTax tax =
                ServiceChargeTaxResolutionSupport.resolve(null, master, 0.0);

        assertThat(tax.taxValue()).isEqualTo(0.0);
        assertThat(tax.taxCode()).isEqualTo("OOS");
    }

    @Test
    void formatTaxCode_shouldReturnOosWhenZeroOrNull() {
        assertThat(ServiceChargeTaxResolutionSupport.formatTaxCode(0.0)).isEqualTo("OOS");
        assertThat(ServiceChargeTaxResolutionSupport.formatTaxCode(null)).isEqualTo("OOS");
    }

    @Test
    void formatTaxCode_shouldStripTrailingZeros() {
        assertThat(ServiceChargeTaxResolutionSupport.formatTaxCode(15.0)).isEqualTo("TAX-15");
        assertThat(ServiceChargeTaxResolutionSupport.formatTaxCode(15.5)).isEqualTo("TAX-15.5");
    }
}
