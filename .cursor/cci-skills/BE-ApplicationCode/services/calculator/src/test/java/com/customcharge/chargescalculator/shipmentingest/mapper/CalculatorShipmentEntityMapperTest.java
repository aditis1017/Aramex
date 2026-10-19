package com.customcharge.chargescalculator.shipmentingest.mapper;

import com.customcharge.chargescalculator.shipmentingest.masterdata.MasterDataCodeLookupService;
import com.customcharge.chargescalculator.support.CalculatorTestDataFactory;
import com.customcharge.persistencelayer.dto.ShipmentDto;
import com.customcharge.persistencelayer.entities.Shipment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalculatorShipmentEntityMapperTest {

    @Mock
    private MasterDataCodeLookupService masterDataCodeLookupService;

    @InjectMocks
    private CalculatorShipmentEntityMapper mapper;

    @Test
    void map_shouldCopyFieldsAndResolveMasterDataCodes() {
        ShipmentDto dto = CalculatorTestDataFactory.shipmentDto();
        dto.setShipmentType("DDP");
        dto.setCategoryType("CAT-1");
        dto.setPickupDate("2024-01-15T10:15:30");
        dto.setHawbAlertedDate("2024-01-16T11:00:00");
        when(masterDataCodeLookupService.resolveCode("PAYMENT_TYPE", "P"))
                .thenReturn(Optional.of("P"));
        when(masterDataCodeLookupService.resolveCode("PRODUCT_GROUP", "DOM"))
                .thenReturn(Optional.of("DOM"));

        Shipment shipment = new Shipment();
        mapper.map(dto, shipment);

        assertThat(shipment.getHawbNumber()).isEqualTo("AWB-1001");
        assertThat(shipment.getPaymentType()).isEqualTo("P");
        assertThat(shipment.getProductGroup()).isEqualTo("DOM");
        assertThat(shipment.getShipmentType()).isEqualTo("DDP");
        assertThat(shipment.getCategoryType()).isEqualTo("CAT-1");
        assertThat(shipment.getOriginEntity()).isEqualTo("RUH");
        assertThat(shipment.getHAWBDestinationEntity()).isEqualTo("DXB");
        assertThat(shipment.getPickupDate()).isNotNull();
        assertThat(shipment.getHAWBAlertedDate()).isNotNull();
    }

    @Test
    void map_shouldResolveBusinessTypeAndSkipBlankDates() {
        ShipmentDto dto = CalculatorTestDataFactory.shipmentDto();
        dto.setBusinessType("E-Commerce");
        dto.setPickupDate(null);
        dto.setHawbAlertedDate(null);
        when(masterDataCodeLookupService.resolveCode("PAYMENT_TYPE", "P"))
                .thenReturn(Optional.of("P"));
        when(masterDataCodeLookupService.resolveCode("PRODUCT_GROUP", "DOM"))
                .thenReturn(Optional.of("DOM"));
        when(masterDataCodeLookupService.resolveCode("BUSINESS_TYPE", "E-Commerce"))
                .thenReturn(Optional.of("ECOM"));

        Shipment shipment = new Shipment();
        mapper.map(dto, shipment);

        assertThat(shipment.getBusinessType()).isEqualTo("ECOM");
        assertThat(shipment.getConsigneeNumber()).isEqualTo("ACC-001");
        assertThat(shipment.getShipperNumber()).isEqualTo("12345");
        assertThat(shipment.getPickupDate()).isNull();
        assertThat(shipment.getHAWBAlertedDate()).isNull();
    }
}
