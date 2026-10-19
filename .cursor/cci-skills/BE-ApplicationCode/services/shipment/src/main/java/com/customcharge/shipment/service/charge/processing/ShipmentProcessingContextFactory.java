package com.customcharge.shipment.service.charge.processing;

import com.customcharge.persistencelayer.accountRedis.AccountRedisChargeDataService;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.repository.CurrencyRepository;
import com.customcharge.persistencelayer.repository.EntityRepository;
import com.customcharge.persistencelayer.repository.ServiceChargeRepository;
import com.customcharge.persistencelayer.repository.ServiceCodesRepository;
import com.customcharge.shipment.service.charge.ShipmentCurrencyContext;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ShipmentProcessingContextFactory {

    CurrencyRepository currencyRepository;
    EntityRepository entityRepository;
    ServiceCodesRepository serviceCodesRepository;
    ServiceChargeRepository serviceChargeRepository;
    AccountRedisChargeDataService accountRedisChargeDataService;

    public ShipmentProcessingContext begin(Shipment shipment, List<String> chargeCodes) {
        ShipmentCurrencyContext currencyContext = new ShipmentCurrencyContext(shipment, currencyRepository);
        ShipmentProcessingContext context = ShipmentProcessingContext.create(
                shipment,
                currencyContext,
                entityRepository,
                serviceCodesRepository,
                serviceChargeRepository,
                accountRedisChargeDataService);
        context.preloadForChargeCodes(chargeCodes);
        ShipmentProcessingContext.bind(context);
        return context;
    }

    public void end() {
        ShipmentProcessingContext.clear();
    }
}
