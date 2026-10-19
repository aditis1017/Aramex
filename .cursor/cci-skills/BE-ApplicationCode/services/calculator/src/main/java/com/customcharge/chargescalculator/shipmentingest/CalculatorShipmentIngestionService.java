package com.customcharge.chargescalculator.shipmentingest;

import com.customcharge.chargescalculator.shipmentingest.mapper.CalculatorShipmentEntityMapper;
import com.customcharge.persistencelayer.dto.ShipmentDto;
import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.repository.ShipmentRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.customcharge.chargescalculator.shipmentingest.IngestConstants.CATEGORY_TYPE_HV;
import static com.customcharge.chargescalculator.shipmentingest.IngestConstants.SHIPMENT_TYPE_DDU;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class CalculatorShipmentIngestionService {

    ShipmentRepository shipmentRepository;
    CalculatorShipmentEntityMapper calculatorShipmentEntityMapper;

    @Transactional
    public Shipment ingest(ShipmentDto dto) {
        dto.setShipmentType(SHIPMENT_TYPE_DDU);
        dto.setCategoryType(CATEGORY_TYPE_HV);

        Shipment shipment = new Shipment();
        calculatorShipmentEntityMapper.map(dto, shipment);
        shipment.setIsManualShipment(true);
        return shipmentRepository.save(shipment);
    }
}
