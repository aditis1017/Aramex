package com.customcharge.dashboard.dto;

import com.customcharge.persistencelayer.entities.Module;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PermissionMatrixDTO {

    private Long moduleId;
    private String moduleName;
    /** Role {@code code} → permission {@code code}s granted for that module. */
    private Map<String, Set<String>> rolePermissions;

    public PermissionMatrixDTO(Module module, Map<String, Set<String>> rolePermissions) {
        this.moduleId = module.getId();
        this.moduleName = module.getDisplayName();
        this.rolePermissions = rolePermissions;
    }
}
