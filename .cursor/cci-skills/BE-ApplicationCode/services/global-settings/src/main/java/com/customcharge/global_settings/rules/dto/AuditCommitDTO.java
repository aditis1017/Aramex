package com.customcharge.global_settings.rules.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AuditCommitDTO {
    private String id; // optional (entity id)
    private String snapshotId;
    private String user;
    private LocalDateTime changeDate;
    private List<ChangedFieldDTO> changedData;
}
