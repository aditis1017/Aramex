package com.customcharge.dashboard.controller;

import com.customcharge.dashboard.dto.ShipmentAuditHistoryCommitDto;
import com.customcharge.dashboard.dto.ShipmentAuditHistoryRequestDto;
import com.customcharge.dashboard.service.ShipmentAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shipment")
@RequiredArgsConstructor
public class ShipmentAuditController {

    private final ShipmentAuditService shipmentAuditService;

    @PostMapping("/audit/history")
    @PreAuthorize("hasAuthority('CUSTOMS_VIEW')")
    public Page<ShipmentAuditHistoryCommitDto> getAuditHistory(
            @RequestBody ShipmentAuditHistoryRequestDto request) {
        return shipmentAuditService.getAuditHistory(request);
    }
}
