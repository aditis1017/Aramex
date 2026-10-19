package com.customcharge.dashboard.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.Instant;

@Data
public class AccountAuditHistoryCommitDto implements Serializable {
    private String commitId;
    private String managedType;
    private String author;
    private Instant commitDate;
    private AccountAuditStateChangeDto stateChangeData;
}