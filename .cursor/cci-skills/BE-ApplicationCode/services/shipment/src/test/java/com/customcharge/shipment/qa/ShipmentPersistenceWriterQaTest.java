package com.customcharge.shipment.qa;

import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.repository.ShipmentRepository;
import com.customcharge.persistencelayer.repository.ShipmentRuleAssignmentRepository;
import com.customcharge.shipment.service.ShipmentPersistenceWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * QA: TC-TX-* persistence race / integrity (simulated).
 */
@ExtendWith(MockitoExtension.class)
class ShipmentPersistenceWriterQaTest {

    @Mock
    private ShipmentRepository shipmentRepository;
    @Mock
    private ShipmentRuleAssignmentRepository shipmentRuleAssignmentRepository;

    @InjectMocks
    private ShipmentPersistenceWriter writer;

    @Test
    @DisplayName("TC-TX-001: Normal insert persists shipment and rule assignments")
    void tcTx001_normalInsert() {
        Shipment shipment = new Shipment();
        when(shipmentRepository.save(shipment)).thenReturn(shipment);

        Optional<Shipment> result = writer.saveNewShipment(shipment, "HAWB-N1", List.of());

        assertThat(result).contains(shipment);
        verify(shipmentRepository).save(shipment);
    }

    @Test
    @DisplayName("TC-TX-008: Insert race — unique HAWB violation treated as duplicate skip")
    void tcTx008_raceDuplicateOnInsert() {
        Shipment shipment = new Shipment();
        when(shipmentRepository.save(shipment)).thenThrow(new DataIntegrityViolationException("uk_hawb"));
        when(shipmentRepository.existsByHawbNumber("HAWB-RACE")).thenReturn(true);

        Optional<Shipment> result = writer.saveNewShipment(shipment, "HAWB-RACE", List.of());

        assertThat(result).isEmpty();
        verify(shipmentRuleAssignmentRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("TC-TX-009: Non-HAWB integrity violation propagates")
    void tcTx009_nonDuplicateIntegrityViolationPropagates() {
        Shipment shipment = new Shipment();
        when(shipmentRepository.save(shipment)).thenThrow(new DataIntegrityViolationException("other_uk"));
        when(shipmentRepository.existsByHawbNumber("HAWB-X")).thenReturn(false);

        assertThatThrownBy(() -> writer.saveNewShipment(shipment, "HAWB-X", List.of()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
