package com.customcharge.shipment.service;

import com.customcharge.persistencelayer.dto.ShipmentDto;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.entities.ShipmentMawbDetails;
import com.customcharge.persistencelayer.repository.ShipmentMawbDetailsRepository;
import com.customcharge.persistencelayer.repository.ShipmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShipmentMawbLinkageServiceTest {

    @Mock
    private ShipmentMawbDetailsRepository shipmentMawbDetailsRepository;

    @Mock
    private ShipmentRepository shipmentRepository;

    @InjectMocks
    private ShipmentMawbLinkageService service;

    @Test
    void applyLinkage_shouldNoOp_whenExactDuplicateExists() {
        ShipmentDto dto = new ShipmentDto();
        dto.setHawb("H1");
        dto.setHawbOriginEntity("RUH");
        dto.setDestinationEntity("JED");
        dto.setMawbConsole("999");

        when(shipmentMawbDetailsRepository.findByHawbNumberAndMawbNumberAndSourceEntityAndTargetEntity(
                "H1", "999", "RUH", "JED")).thenReturn(Optional.of(new ShipmentMawbDetails()));

        service.applyLinkage(dto);

        verify(shipmentMawbDetailsRepository, never()).save(any());
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void applyLinkage_shouldSaveNewMawbButKeepFirstOnShipment_whenEarlierDetailsExist() {
        ShipmentDto dto = new ShipmentDto();
        dto.setHawb("H1");
        dto.setHawbOriginEntity("RUH");
        dto.setDestinationEntity("JED");
        dto.setMawbConsole("999");

        when(shipmentMawbDetailsRepository.findByHawbNumberAndMawbNumberAndSourceEntityAndTargetEntity(
                "H1", "999", "RUH", "JED")).thenReturn(Optional.empty());

        ShipmentMawbDetails first = new ShipmentMawbDetails();
        first.setMawbNumber("488012");
        when(shipmentMawbDetailsRepository.findFirstByHawbNumberAndSourceEntityAndTargetEntityOrderByCreatedTimeAsc(
                "H1", "RUH", "JED")).thenReturn(Optional.of(first));

        Shipment shipment = new Shipment();
        when(shipmentRepository.findByHawbNumberAndOriginEntityAndHAWBDestinationEntity("H1", "RUH", "JED"))
                .thenReturn(Optional.of(shipment));

        service.applyLinkage(dto);

        ArgumentCaptor<ShipmentMawbDetails> detailsCaptor = ArgumentCaptor.forClass(ShipmentMawbDetails.class);
        verify(shipmentMawbDetailsRepository).save(detailsCaptor.capture());
        assertThat(detailsCaptor.getValue().getMawbNumber()).isEqualTo("999");

        ArgumentCaptor<Shipment> shipmentCaptor = ArgumentCaptor.forClass(Shipment.class);
        verify(shipmentRepository).save(shipmentCaptor.capture());
        assertThat(shipmentCaptor.getValue().getMAWBConsole()).isEqualTo("488012");
    }

    @Test
    void applyLinkage_shouldSaveDetailsAndUpdateShipment_whenFirstEntry() {
        ShipmentDto dto = new ShipmentDto();
        dto.setHawb("H1");
        dto.setHawbOriginEntity("RUH");
        dto.setDestinationEntity("JED");
        dto.setMawbConsole("488012");

        when(shipmentMawbDetailsRepository.findByHawbNumberAndMawbNumberAndSourceEntityAndTargetEntity(
                "H1", "488012", "RUH", "JED")).thenReturn(Optional.empty());
        when(shipmentMawbDetailsRepository.findFirstByHawbNumberAndSourceEntityAndTargetEntityOrderByCreatedTimeAsc(
                "H1", "RUH", "JED")).thenReturn(Optional.empty());

        Shipment shipment = new Shipment();
        when(shipmentRepository.findByHawbNumberAndOriginEntityAndHAWBDestinationEntity("H1", "RUH", "JED"))
                .thenReturn(Optional.of(shipment));

        service.applyLinkage(dto);

        verify(shipmentMawbDetailsRepository).save(any(ShipmentMawbDetails.class));

        ArgumentCaptor<Shipment> shipmentCaptor = ArgumentCaptor.forClass(Shipment.class);
        verify(shipmentRepository).save(shipmentCaptor.capture());
        assertThat(shipmentCaptor.getValue().getMAWBConsole()).isEqualTo("488012");
    }

    @Test
    void applyFirstMawbConsoleFromDetails_shouldSetConsoleFromFirstDetail() {
        Shipment shipment = new Shipment();
        shipment.setHawbNumber("H1");
        shipment.setOriginEntity("RUH");
        shipment.setHAWBDestinationEntity("JED");

        ShipmentMawbDetails first = new ShipmentMawbDetails();
        first.setMawbNumber("488012");
        when(shipmentMawbDetailsRepository.findFirstByHawbNumberAndSourceEntityAndTargetEntityOrderByCreatedTimeAsc(
                "H1", "RUH", "JED")).thenReturn(Optional.of(first));

        service.applyFirstMawbConsoleFromDetails(shipment);

        assertThat(shipment.getMAWBConsole()).isEqualTo("488012");
    }
}
