package com.customcharge.dashboard.dto;

import com.customcharge.persistencelayer.entities.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserRoleDetailsDTO {

    private Long userId;
    private String username;
    private String email;
    private Long assignedRoleId;
    private List<RoleOptionDTO> roles;

    public UserRoleDetailsDTO(User user, List<RoleOptionDTO> roles) {
        this.userId = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.assignedRoleId = user.getRole() != null ? user.getRole().getId() : null;
        this.roles = roles;
    }
}
