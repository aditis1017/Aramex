package com.customcharge.shipment.validation.impl;

import com.customcharge.persistencelayer.dto.ShipmentDto;
import com.customcharge.shipment.validation.ShipmentValidator;
import jakarta.validation.ValidationException;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static com.customcharge.shipment.constants.ErrorMessages.INVALID_DDP_CONSIGNEE_ACCOUNT_REQUIRED;
import static com.customcharge.shipment.constants.ErrorMessages.SHIPMENT_IS_NULL;
import static com.customcharge.shipment.enums.DeliveryType.DDP;

@Component
public class ShipmentValidatorImpl implements ShipmentValidator {

    @Override
    public void validate(ShipmentDto shipment) {
        // Reserved for bean-validation based checks when enabled.
    }

    @Override
    public void validateAccountNumber(ShipmentDto shipment) {
        if (Objects.isNull(shipment)) {
            throw new ValidationException(SHIPMENT_IS_NULL);
        }

        if (Objects.equals(shipment.getShipmentType(), DDP.name())) {
            if (isBlank(shipment.getConsigneeAccountNumber())) {
                throw new ValidationException(INVALID_DDP_CONSIGNEE_ACCOUNT_REQUIRED);
            }
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
