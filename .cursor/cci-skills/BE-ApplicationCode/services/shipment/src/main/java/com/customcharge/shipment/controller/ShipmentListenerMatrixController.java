package com.customcharge.shipment.controller;

import com.customcharge.shipment.dto.ShipmentListenerMatrixResponse;
import com.customcharge.shipment.service.ShipmentListenerMatrixService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class ShipmentListenerMatrixController {

    private final ShipmentListenerMatrixService shipmentListenerMatrixService;

    @GetMapping("/listeners")
    public ShipmentListenerMatrixResponse listenerMatrix() {
        return shipmentListenerMatrixService.captureMatrix();
    }
}
