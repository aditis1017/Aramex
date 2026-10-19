package com.customcharge.dashboard.dto;

import com.customcharge.persistencelayer.entities.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RoleOptionDTO {
    private Long roleId;
    private String roleCode;
    private String displayName;

    public RoleOptionDTO(Role role) {
        this.roleId = role.getId();
        this.roleCode = role.getCode();
        this.displayName = role.getName() != null && !role.getName().isBlank()
                ? role.getName()
                : role.getDisplayName();
    }
}
