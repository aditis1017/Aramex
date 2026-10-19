package com.customcharge.persistencelayer.audit.model;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class AuditCommitBlock {
    private String commitId;
    private String managedType;
    private String author;
    private Instant commitDate;
    private List<AuditStateChangeEntry> stateChangeData = new ArrayList<>();
}
