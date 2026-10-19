package com.customcharge.shipment.service.charge;

import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.shipment.constants.ShipmentChargeTypeSupport;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.customcharge.shipment.constants.LogMessages.CHARGE_CURRENCY_NOT_RESOLVED;

@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ChargeCurrencyResolver {

    ShipmentCurrencyContextFactory currencyContextFactory;

    public String resolve(Shipment shipment, String chargeCode) {
        if (shipment == null || chargeCode == null || chargeCode.isBlank()) {
            return null;
        }
        String currency = currencyContextFactory.forShipment(shipment).chargeTargetCurrency(chargeCode);
        if (currency == null) {
            log.warn(
                    CHARGE_CURRENCY_NOT_RESOLVED,
                    chargeCode,
                    shipment.getHawbNumber());
        }
        return currency;
    }
}
