package com.customcharge.shipment.validation;

import com.customcharge.persistencelayer.dto.ShipmentDto;

public interface ShipmentValidator {

    void validate(ShipmentDto shipment);

    void validateAccountNumber(ShipmentDto shipment);
}
