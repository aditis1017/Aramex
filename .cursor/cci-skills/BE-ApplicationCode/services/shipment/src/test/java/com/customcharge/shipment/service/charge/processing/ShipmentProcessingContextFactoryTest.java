package com.customcharge.shipment.service.charge.processing;

import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.repository.CurrencyRepository;
import com.customcharge.persistencelayer.repository.EntityRepository;
import com.customcharge.persistencelayer.repository.ServiceChargeRepository;
import com.customcharge.persistencelayer.repository.ServiceCodesRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShipmentProcessingContextFactoryTest {

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
    void begin_shouldBindContextAndPreload() {
        Shipment shipment = new Shipment();
        shipment.setHAWBDestinationEntity("RUH");
        shipment.setOriginEntity("JED");
        when(serviceCodesRepository.findActiveMastersByEntityCodeAndChargeTypes(any(), any()))
                .thenReturn(List.of());

        ShipmentProcessingContextFactory factory = new ShipmentProcessingContextFactory(
                currencyRepository, entityRepository, serviceCodesRepository, serviceChargeRepository, null);
        ShipmentProcessingContext context = factory.begin(shipment, List.of("ECD", "ECS"));

        assertThat(ShipmentProcessingContext.get()).isSameAs(context);
        verify(entityRepository).findByCodeIgnoreCase("RUH");
        factory.end();
        assertThat(ShipmentProcessingContext.get()).isNull();
    }
}
