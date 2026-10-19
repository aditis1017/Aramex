package com.customcharge.dashboard.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ModulePermissionRequestDTO {
    @NotNull(message = "moduleId is required")
    private Long moduleId;

    @NotNull(message = "permissions list is required (use empty list for none)")
    private List<String> permissions;
}
