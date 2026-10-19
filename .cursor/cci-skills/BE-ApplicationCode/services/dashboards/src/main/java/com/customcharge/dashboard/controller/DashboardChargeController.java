package com.customcharge.dashboard.controller;

import com.customcharge.dashboard.dto.charge.ShipmentChargeDetailsResponse;
import com.customcharge.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard/shipment-charges")
@RequiredArgsConstructor
public class DashboardChargeController {

    private final DashboardService dashboardService;

    @GetMapping
    @PreAuthorize("hasAuthority('CUSTOMS_VIEW')")
    public ShipmentChargeDetailsResponse getChargeDetails(
            @RequestParam String hawbNumber,
            @RequestParam String chargeType) {
        return dashboardService.getShipmentChargeDetails(hawbNumber, chargeType);
    }
}
