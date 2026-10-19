package com.customcharge.dashboard.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class AccountAuditStateChangeDto {
    private JsonNode preState;
    private String type;
    private JsonNode currentState;
    private JsonNode changeParameter;
}