package com.customcharge.dashboard.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class ShipmentAuditHistoryCommitDto {
    private String commitId;
    private String managedType;
    private String author;
    private Instant commitDate;
    private AccountAuditStateChangeDto stateChangeData;
}
