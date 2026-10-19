package com.customcharge.global_settings.rules.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class AuditRequest {
    private String groupName;
    private Instant fromDate;
    private Instant toDate;
    private int page;
    private int size;
}
