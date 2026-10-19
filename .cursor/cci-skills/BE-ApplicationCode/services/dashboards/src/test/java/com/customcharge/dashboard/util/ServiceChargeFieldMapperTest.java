package com.customcharge.dashboard.util;

import com.customcharge.persistencelayer.entities.ServiceCharge;
import com.customcharge.persistencelayer.entities.ServiceCodes;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceChargeFieldMapperTest {

    @Test
    void shouldReturnNull_whenChargeMissing() {
        assertThat(ServiceChargeFieldMapper.serviceCode(null)).isNull();
        assertThat(ServiceChargeFieldMapper.serviceDescription(null)).isNull();
        assertThat(ServiceChargeFieldMapper.taxCode(null)).isNull();
        assertThat(ServiceChargeFieldMapper.taxValue(null)).isEqualTo(0.0);
    }

    @Test
    void shouldReadFieldsFromMaster() {
        ServiceCodes master = new ServiceCodes();
        master.setServiceCode("SC01");
        master.setServiceDescription("Service One");
        master.setTaxCode("VAT");

        ServiceCharge charge = new ServiceCharge();
        charge.setServiceCodes(master);
        charge.setTaxValue(5.0);

        assertThat(ServiceChargeFieldMapper.serviceCode(charge)).isEqualTo("SC01");
        assertThat(ServiceChargeFieldMapper.serviceDescription(charge)).isEqualTo("Service One");
        assertThat(ServiceChargeFieldMapper.taxCode(charge)).isEqualTo("VAT");
        assertThat(ServiceChargeFieldMapper.taxValue(charge)).isEqualTo(5.0);
    }
}
