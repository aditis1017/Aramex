package com.customcharge.dashboard.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class RolePermissionUpdateRequestDTO {
    @NotNull(message = "modules list is required")
    @Valid
    private List<@NotNull(message = "each module entry must be non-null") ModulePermissionRequestDTO> modules;
}
