package com.customcharge.shipment.service.charge;

import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.repository.CurrencyRepository;
import com.customcharge.shipment.service.charge.processing.ShipmentProcessingContext;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ShipmentCurrencyContextFactory {

    CurrencyRepository currencyRepository;

    public ShipmentCurrencyContext forShipment(Shipment shipment) {
        ShipmentProcessingContext context = ShipmentProcessingContext.get();
        if (context != null && context.getShipment() == shipment) {
            return context.getCurrencyContext();
        }
        return new ShipmentCurrencyContext(shipment, currencyRepository);
    }
}
