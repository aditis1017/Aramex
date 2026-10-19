package com.customcharge.persistencelayer.audit.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuditStateChangeEntry {
    private JsonNode preState;
    private String type;
    private JsonNode currentState;
    private JsonNode changeParameter;
}
