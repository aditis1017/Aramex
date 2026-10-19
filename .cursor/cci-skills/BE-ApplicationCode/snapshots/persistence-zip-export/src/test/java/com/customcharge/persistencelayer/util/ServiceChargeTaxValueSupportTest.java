package com.customcharge.persistencelayer.util;

import com.customcharge.persistencelayer.entities.ServiceCharge;
import com.customcharge.persistencelayer.entities.ServiceCodes;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceChargeTaxValueSupportTest {

    @Test
    void resolve_shouldReturnChargeTax_whenServiceChargeTaxValueSet() {
        ServiceCodes master = new ServiceCodes();
        master.setFetchTax(false);

        ServiceCharge charge = new ServiceCharge();
        charge.setServiceCodes(master);
        charge.setTaxValue(5.0);

        assertThat(ServiceChargeTaxValueSupport.resolve(charge)).isEqualTo(5.0);
    }

    @Test
    void resolve_shouldPreferChargeTax_overEntityTax_whenBothPresent() {
        com.customcharge.persistencelayer.entities.Entity entity =
                new com.customcharge.persistencelayer.entities.Entity();
        entity.setTaxValue(15.0);

        ServiceCodes master = new ServiceCodes();
        master.setFetchTax(true);
        master.setEntity(entity);

        ServiceCharge charge = new ServiceCharge();
        charge.setServiceCodes(master);
        charge.setTaxValue(7.5);

        assertThat(ServiceChargeTaxValueSupport.resolve(charge)).isEqualTo(7.5);
    }

    @Test
    void resolve_shouldReturnEntityTax_whenFetchTaxTrueAndChargeTaxAbsent() {
        com.customcharge.persistencelayer.entities.Entity entity =
                new com.customcharge.persistencelayer.entities.Entity();
        entity.setTaxValue(15.0);

        ServiceCodes master = new ServiceCodes();
        master.setFetchTax(true);
        master.setEntity(entity);

        ServiceCharge charge = new ServiceCharge();
        charge.setServiceCodes(master);

        assertThat(ServiceChargeTaxValueSupport.resolve(charge)).isEqualTo(15.0);
    }

    @Test
    void resolve_shouldReturnZero_whenFetchTaxFalseAndChargeTaxAbsent() {
        com.customcharge.persistencelayer.entities.Entity entity =
                new com.customcharge.persistencelayer.entities.Entity();
        entity.setTaxValue(15.0);

        ServiceCodes master = new ServiceCodes();
        master.setFetchTax(false);
        master.setEntity(entity);

        ServiceCharge charge = new ServiceCharge();
        charge.setServiceCodes(master);

        assertThat(ServiceChargeTaxValueSupport.resolve(charge)).isEqualTo(0.0);
    }

    @Test
    void resolve_shouldReturnZero_whenFetchTaxNullAndChargeTaxAbsent() {
        ServiceCodes master = new ServiceCodes();
        master.setFetchTax(null);

        ServiceCharge charge = new ServiceCharge();
        charge.setServiceCodes(master);

        assertThat(ServiceChargeTaxValueSupport.resolve(charge)).isEqualTo(0.0);
    }

    @Test
    void resolve_shouldReturnZero_whenChargeNull() {
        assertThat(ServiceChargeTaxValueSupport.resolve(null)).isEqualTo(0.0);
    }
}
