package com.customcharge.chargescalculator.service.batch;

import com.customcharge.chargescalculator.constants.BatchProcessingConstants;
import com.customcharge.chargescalculator.shipmentingest.CalculatorShipmentIngestionService;
import com.customcharge.chargescalculator.shipmentingest.coredata.CoreDataShipmentApi;
import com.customcharge.chargescalculator.shipmentingest.coredata.CoreDataShipmentProperties;
import com.customcharge.chargescalculator.support.CalculatorTestDataFactory;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.exception.ResourceNotFoundException;
import com.customcharge.persistencelayer.repository.ShipmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShipmentLookupServiceTest {

    @Mock
    private ShipmentRepository shipmentRepository;
    @Mock
    private CoreDataShipmentProperties coreDataShipmentProperties;
    @Mock
    private ObjectProvider<CoreDataShipmentApi> coreDataShipmentApi;
    @Mock
    private CalculatorShipmentIngestionService calculatorShipmentIngestionService;

    @InjectMocks
    private ShipmentLookupService service;

    private Shipment shipment;

    @BeforeEach
    void setUp() {
        shipment = CalculatorTestDataFactory.shipment();
    }

    @Test
    void resolveByHawb_shouldReturnExistingShipment() {
        when(shipmentRepository.findByHawbNumber("AWB-1001")).thenReturn(Optional.of(shipment));

        assertThat(service.resolveByHawb("AWB-1001")).isSameAs(shipment);
    }

    @Test
    void resolveByHawb_shouldTrimAwbBeforeLookup() {
        when(shipmentRepository.findByHawbNumber("AWB-1001")).thenReturn(Optional.of(shipment));

        assertThat(service.resolveByHawb("  AWB-1001  ")).isSameAs(shipment);
    }

    @Test
    void resolveByHawb_shouldThrow_whenNotFound() {
        when(shipmentRepository.findByHawbNumber("AWB-1001")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolveByHawb("AWB-1001"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(BatchProcessingConstants.ERROR_SHIPMENT_NOT_FOUND);
    }

    @Test
    void resolveByHawb_shouldNotCallCoreData_whenNotFound() {
        when(shipmentRepository.findByHawbNumber("AWB-1001")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolveByHawb("AWB-1001"))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(coreDataShipmentProperties, coreDataShipmentApi, calculatorShipmentIngestionService);
    }
}
