package com.customcharge.shipment.constants;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.assertj.core.api.Assertions.assertThat;

class ShipmentConstantsTest {

    @Test
    void constants_shouldExposeExpectedValues() {
        assertThat(ShipmentConstants.BUSINESS_TYPE_E_COMMERCE).isEqualTo("E-Commerce");
        assertThat(ShipmentConstants.SHIPMENT_TYPE_DDU).isEqualTo("DDU");
        assertThat(ShipmentConstants.SHIPMENT_TYPE_PRIMARY_RULE_EVALUATION_CODES)
                .containsExactly("DDU", "DDO");
        assertThat(ShipmentConstants.SHIPMENT_TYPE_FALLBACK_RULE_EVALUATION_CODES)
                .containsExactly("DDP");
        assertThat(ShipmentConstants.FULL_SHIPMENT_DESTINATION_ENTITIES)
                .containsExactly("RUH", "JED", "DHA");
        assertThat(ShipmentConstants.CATEGORY_TYPE_HV).isEqualTo("HV");
    }

    @Test
    void constructor_shouldBePrivate() throws Exception {
        Constructor<ShipmentConstants> constructor = ShipmentConstants.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThat(constructor.newInstance()).isNotNull();
    }
}
