package com.customcharge.shipment.service.charge;

import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.repository.CurrencyRepository;
import com.customcharge.persistencelayer.repository.EntityRepository;
import com.customcharge.persistencelayer.repository.ServiceChargeRepository;
import com.customcharge.persistencelayer.repository.ServiceCodesRepository;
import com.customcharge.shipment.service.charge.processing.ShipmentProcessingContext;
import com.customcharge.shipment.service.charge.processing.ShipmentProcessingContextFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ShipmentCurrencyContextFactoryTest {

    @Mock
    private CurrencyRepository currencyRepository;
    @Mock
    private EntityRepository entityRepository;
    @Mock
    private ServiceCodesRepository serviceCodesRepository;
    @Mock
    private ServiceChargeRepository serviceChargeRepository;

    @AfterEach
    void tearDown() {
        ShipmentProcessingContext.clear();
    }

    @Test
    void forShipment_shouldReturnContextCurrency_whenProcessingContextBound() {
        Shipment shipment = new Shipment();
        shipment.setConsigneeCountryCode("SA");
        ShipmentProcessingContextFactory factory = new ShipmentProcessingContextFactory(
                currencyRepository, entityRepository, serviceCodesRepository, serviceChargeRepository, null);
        ShipmentProcessingContext context = factory.begin(shipment, List.of("ECD"));
        ShipmentCurrencyContextFactory currencyFactory = new ShipmentCurrencyContextFactory(currencyRepository);

        ShipmentCurrencyContext result = currencyFactory.forShipment(shipment);

        assertThat(result).isSameAs(context.getCurrencyContext());
        factory.end();
    }

    @Test
    void forShipment_shouldCreateNewContext_whenProcessingContextMissing() {
        Shipment shipment = new Shipment();
        ShipmentCurrencyContextFactory currencyFactory = new ShipmentCurrencyContextFactory(currencyRepository);

        ShipmentCurrencyContext result = currencyFactory.forShipment(shipment);

        assertThat(result).isNotNull();
    }
}
