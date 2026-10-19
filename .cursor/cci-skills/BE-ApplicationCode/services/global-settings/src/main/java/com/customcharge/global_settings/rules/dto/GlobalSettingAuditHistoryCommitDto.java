package com.customcharge.global_settings.rules.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class GlobalSettingAuditHistoryCommitDto {
    private String commitId;
    private String managedType;
    private String author;
    private Instant commitDate;
    private GlobalSettingAuditStateChangeDto stateChangeData;
}
