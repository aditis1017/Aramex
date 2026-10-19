package com.customcharge.dashboard.dto;

import com.customcharge.dashboard.constants.RoleConstants;
import com.customcharge.persistencelayer.entities.Role;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoleDTO {

    private Long id;

    @Pattern(
            regexp = RoleConstants.ROLE_CODE_PATTERN,
            message = RoleConstants.ROLE_CODE_VALIDATION_MESSAGE)
    private String code;

    /** Legacy field; kept for API compatibility — prefer {@link #name}. */
    private String displayName;

    private String name;

    private String description;

    private Boolean systemRole;

    private String arabicName;

    private String arabicDescription;

    private String color;

    private Long userCount;
    private List<RolePermissionDTO> permissions;

    public RoleDTO(Role role, Long userCount, List<RolePermissionDTO> permissions) {
        this.id = role.getId();
        this.code = role.getCode();
        this.displayName = role.getDisplayName();
        this.name = role.getName() != null && !role.getName().isBlank() ? role.getName() : role.getDisplayName();
        this.description = role.getDescription();
        this.systemRole = role.isSystemRole();
        this.arabicName = role.getArabicName();
        this.arabicDescription = role.getArabicDescription();
        this.color = role.getColor();
        this.permissions = permissions;
        this.userCount = userCount;
    }
}
