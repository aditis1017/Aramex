package com.customcharge.shipment.service.charge;

import com.customcharge.persistencelayer.entities.Charge;
import com.customcharge.persistencelayer.entities.Entity;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.repository.EntityRepository;
import com.customcharge.shipment.service.charge.processing.ShipmentProcessingContext;
import com.customcharge.shipment.service.charge.processing.ShipmentProcessingContextFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShipmentBillingEntitySupportTest {

    @Mock
    private EntityRepository entityRepository;
    @Mock
    private com.customcharge.persistencelayer.repository.CurrencyRepository currencyRepository;
    @Mock
    private com.customcharge.persistencelayer.repository.ServiceCodesRepository serviceCodesRepository;
    @Mock
    private com.customcharge.persistencelayer.repository.ServiceChargeRepository serviceChargeRepository;

    @InjectMocks
    private ShipmentBillingEntitySupport support;

    private ShipmentProcessingContextFactory contextFactory;

    @BeforeEach
    void setUp() {
        contextFactory = new ShipmentProcessingContextFactory(
                currencyRepository, entityRepository, serviceCodesRepository, serviceChargeRepository, null);
    }

    @AfterEach
    void tearDown() {
        contextFactory.end();
    }

    @Test
    void applyToCharge_shouldNoOp_whenInputsMissing() {
        support.applyToCharge(null, new Shipment());
        support.applyToCharge(new Charge(), null);

        verifyNoInteractions(entityRepository);
    }

    @Test
    void applyToCharge_shouldSkip_whenEntityAlreadySet() {
        Charge charge = new Charge();
        charge.setChargeCode("ECD");
        charge.setEntity(new Entity());

        support.applyToCharge(charge, shipment("RUH"));

        verifyNoInteractions(entityRepository);
    }

    @Test
    void applyToCharge_shouldResolveFromRepository_whenNoContext() {
        Charge charge = new Charge();
        charge.setChargeCode("ECD");
        Entity entity = entity("RUH");
        when(entityRepository.findByCodeIgnoreCase("RUH")).thenReturn(Optional.of(entity));

        support.applyToCharge(charge, shipment("RUH"));

        assertThat(charge.getEntity()).isSameAs(entity);
    }

    @Test
    void applyToCharge_shouldUseOriginEntity_forEcs() {
        Charge charge = new Charge();
        charge.setChargeCode("ECS");
        Entity entity = entity("JED");
        when(entityRepository.findByCodeIgnoreCase("JED")).thenReturn(Optional.of(entity));

        Shipment shipment = shipment("RUH");
        shipment.setOriginEntity("JED");

        support.applyToCharge(charge, shipment);

        assertThat(charge.getEntity()).isSameAs(entity);
        verify(entityRepository).findByCodeIgnoreCase("JED");
    }

    @Test
    void applyToCharge_shouldUseProcessingContext_whenBound() {
        Charge charge = new Charge();
        charge.setChargeCode("ECD");
        Shipment shipment = shipment("RUH");
        Entity entity = entity("RUH");
        contextFactory.begin(shipment, List.of("ECD"));
        when(entityRepository.findByCodeIgnoreCase("RUH")).thenReturn(Optional.of(entity));

        support.applyToCharge(charge, shipment);

        assertThat(charge.getEntity()).isSameAs(entity);
    }

    private static Shipment shipment(String destinationEntity) {
        Shipment shipment = new Shipment();
        shipment.setHAWBDestinationEntity(destinationEntity);
        return shipment;
    }

    private static Entity entity(String code) {
        Entity entity = new Entity();
        entity.setCode(code);
        return entity;
    }
}
