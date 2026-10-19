package com.customcharge.shipment.validation.impl;

import com.customcharge.persistencelayer.dto.ShipmentDto;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShipmentValidatorImplTest {

    private final ShipmentValidatorImpl validator = new ShipmentValidatorImpl();

    @Test
    void validate_shouldNoOp() {
        assertThatCode(() -> validator.validate(new ShipmentDto())).doesNotThrowAnyException();
    }

    @Test
    void validateAccountNumber_shouldThrow_whenShipmentNull() {
        assertThatThrownBy(() -> validator.validateAccountNumber(null))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void validateAccountNumber_shouldNotRequireAccountsForDdu() {
        ShipmentDto shipment = new ShipmentDto();
        shipment.setShipmentType("DDU");

        assertThatCode(() -> validator.validateAccountNumber(shipment)).doesNotThrowAnyException();
    }

    @Test
    void validateAccountNumber_shouldRequireConsigneeForDdp() {
        ShipmentDto shipment = new ShipmentDto();
        shipment.setShipmentType("DDP");

        assertThatThrownBy(() -> validator.validateAccountNumber(shipment))
                .isInstanceOf(ValidationException.class);

        shipment.setConsigneeAccountNumber("C1");
        assertThatCode(() -> validator.validateAccountNumber(shipment)).doesNotThrowAnyException();
    }
}
