package com.customcharge.persistencelayer.audit.model;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Setter
@Getter
public class AuditQueryRequest {
    private String groupName;
    private Optional<String> managedType = Optional.empty();
    private List<String> managedTypes = new ArrayList<>();
    private Map<String, String> commitPropertyFilters = new HashMap<>();
    private Instant fromDate;
    private Instant toDate;
    private int page;
    private int size;

}
