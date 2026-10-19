package com.customcharge.global_settings.rules.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class GlobalSettingAuditStateChangeDto {
    private JsonNode preState;
    private String type;
    private JsonNode currentState;
    private JsonNode changeParameter;
}
