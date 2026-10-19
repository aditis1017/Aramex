package com.customcharge.dashboard.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UpdateUserRoleRequestDTO {
    @NotNull(message = "roleId is required")
    private Long roleId;
}
