package com.customcharge.dashboard.service;

import com.customcharge.persistencelayer.entities.Shipment;
import com.customcharge.persistencelayer.repository.ShipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HbCciMsg5ShipmentSaveService {

    private final ShipmentRepository shipmentRepository;

    public HbCciMsg5ShipmentSaveService(ShipmentRepository shipmentRepository) {
        this.shipmentRepository = shipmentRepository;
    }
}

