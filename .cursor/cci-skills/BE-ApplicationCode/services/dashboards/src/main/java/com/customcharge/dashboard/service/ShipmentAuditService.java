package com.customcharge.dashboard.service;

import com.customcharge.dashboard.dto.ShipmentAuditHistoryCommitDto;
import com.customcharge.dashboard.dto.ShipmentAuditHistoryRequestDto;
import org.springframework.data.domain.Page;

public interface ShipmentAuditService {
    Page<ShipmentAuditHistoryCommitDto> getAuditHistory(ShipmentAuditHistoryRequestDto request);
}
