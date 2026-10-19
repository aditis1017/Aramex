package com.customcharge.chargescalculator.shipmentingest;

import com.customcharge.chargescalculator.shipmentingest.mapper.CalculatorShipmentEntityMapper;
import com.customcharge.chargescalculator.support.CalculatorTestDataFactory;
import com.customcharge.persistencelayer.dto.ShipmentDto;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.repository.ShipmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalculatorShipmentIngestionServiceTest {

    @Mock
    private ShipmentRepository shipmentRepository;
    @Mock
    private CalculatorShipmentEntityMapper calculatorShipmentEntityMapper;

    @InjectMocks
    private CalculatorShipmentIngestionService service;

    @Test
    void ingest_shouldApplyDefaultsMapAndSaveManualShipment() {
        ShipmentDto dto = CalculatorTestDataFactory.shipmentDto();
        Shipment saved = CalculatorTestDataFactory.shipment();
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(saved);

        Shipment result = service.ingest(dto);

        assertThat(result).isSameAs(saved);
        assertThat(dto.getShipmentType()).isEqualTo(IngestConstants.SHIPMENT_TYPE_DDU);
        assertThat(dto.getCategoryType()).isEqualTo(IngestConstants.CATEGORY_TYPE_HV);

        ArgumentCaptor<Shipment> shipmentCaptor = ArgumentCaptor.forClass(Shipment.class);
        verify(calculatorShipmentEntityMapper).map(eq(dto), shipmentCaptor.capture());
        verify(shipmentRepository).save(shipmentCaptor.getValue());
        assertThat(shipmentCaptor.getValue().getIsManualShipment()).isTrue();
    }
}
