package com.customcharge.shipment.observability;

import com.customcharge.persistencelayer.dto.ShipmentDto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ShipmentDestinationEntitySupportTest {

    @Test
    void resolve_shouldPreferDestinationEntityThenFallbacks() {
        ShipmentDto dto = new ShipmentDto();
        dto.setDestinationEntity("  AE ");
        dto.setConsigneeCountryCode("SA");

        assertThat(ShipmentDestinationEntitySupport.resolve(dto)).isEqualTo("AE");
    }

    @Test
    void resolve_shouldUseFallbackWhenDestinationEntityMissing() {
        ShipmentDto dto = new ShipmentDto();
        dto.setDestinationLocationCode("RUH");
        dto.setConsigneeCountryCode("SA");

        assertThat(ShipmentDestinationEntitySupport.resolve(dto)).isEqualTo("RUH");
    }

    @Test
    void resolve_shouldReturnUnknownWhenAllMissing() {
        assertThat(ShipmentDestinationEntitySupport.resolve(new ShipmentDto())).isEqualTo("unknown");
    }
}
