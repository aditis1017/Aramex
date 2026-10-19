package com.customcharge.dashboard.dto;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class AccountAuditHistoryRequestDto {
    private UUID accountId;
    private String entityCode;
    private Instant fromDate;
    private Instant toDate;
    private String managedType;
    private int page;
    private int size;
}
