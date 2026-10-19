package com.customcharge.dashboard.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class ShipmentAuditHistoryRequestDto {
    private String hawbNumber;
    private Instant fromDate;
    private Instant toDate;
    private String managedType;
    private int page;
    private int size;
}
