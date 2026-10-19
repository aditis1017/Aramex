package com.customcharge.dashboard.service;

import com.customcharge.dashboard.dto.RoleDTO;
import com.customcharge.dashboard.dto.RolePermissionUpdateRequestDTO;
import com.customcharge.persistencelayer.dto.RoleSummaryDTO;
import com.customcharge.persistencelayer.dto.response.MessageResponse;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Optional;

public interface RoleService {

    RoleDTO create(@Valid RoleDTO dto);

    List<RoleSummaryDTO> getRoleOverview(Integer page, Integer size);

    List<RoleDTO> findAll();

    boolean deleteById(Long id);

    Optional<RoleDTO> findById(Long id);

    Optional<RoleDTO> update(Long id, RoleDTO dto);

    RoleDTO getRoleDetails(Long roleId);

    /** Returns role details for the user authenticated on the current request (from JWT). */
    RoleDTO getRoleDetailsForCurrentUser();

    MessageResponse updatePermissions(Long roleId, RolePermissionUpdateRequestDTO request);

    MessageResponse duplicateRole(@Valid RoleDTO dto, Long roleId);
}
