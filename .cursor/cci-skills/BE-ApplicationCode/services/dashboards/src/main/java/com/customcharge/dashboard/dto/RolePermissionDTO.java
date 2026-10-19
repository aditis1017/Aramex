package com.customcharge.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolePermissionDTO {
    private Long moduleId;
    private String moduleName;
    /** Permission {@code code}s (e.g. {@code CUSTOMS_VIEW}). */
    private Set<String> permissionCodes;
}
