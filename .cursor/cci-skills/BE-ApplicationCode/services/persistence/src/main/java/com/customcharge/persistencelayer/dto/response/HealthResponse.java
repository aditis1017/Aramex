package com.customcharge.persistencelayer.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class HealthResponse {

    private String status;
    private LocalDateTime timestamp;
    private String application;
    private String applicationVersion;
    private Map<String, Object> details;
}
