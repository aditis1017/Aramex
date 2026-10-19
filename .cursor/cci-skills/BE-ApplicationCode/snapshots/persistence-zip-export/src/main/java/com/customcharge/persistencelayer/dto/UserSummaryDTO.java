package com.customcharge.persistencelayer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryDTO {
    private Long userId;
    private String username;
    private String userEmail;
    private Long roleId;
    private String roleCode;
    private String roleDisplayName;
}
