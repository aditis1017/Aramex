package com.customcharge.dashboard.service;

import com.customcharge.dashboard.dto.UpdateUserRoleRequestDTO;
import com.customcharge.dashboard.dto.UserRoleDetailsDTO;
import com.customcharge.persistencelayer.dto.UserSummaryDTO;
import com.customcharge.persistencelayer.dto.response.MessageResponse;

import java.util.List;

public interface UserRoleService {
    void createDefaultUsers();

    void createModules();

    List<UserSummaryDTO> getUserOverview();

    UserRoleDetailsDTO getUserRoleDetails(Long userId);

    MessageResponse updateUserRole(Long userId, UpdateUserRoleRequestDTO request);
}
