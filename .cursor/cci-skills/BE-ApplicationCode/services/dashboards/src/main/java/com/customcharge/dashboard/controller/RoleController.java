package com.customcharge.dashboard.controller;

import com.customcharge.dashboard.dto.RoleDTO;
import com.customcharge.dashboard.dto.RolePermissionUpdateRequestDTO;
import com.customcharge.dashboard.rbac.RouteAccessCookieService;
import com.customcharge.dashboard.service.RoleService;
import jakarta.servlet.http.HttpServletResponse;
import com.customcharge.persistencelayer.dto.RoleSummaryDTO;
import com.customcharge.persistencelayer.dto.response.MessageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/roles")
@Validated
public class RoleController {

    private final RoleService roleService;
    private final RouteAccessCookieService routeAccessCookieService;

    public RoleController(RoleService roleService, RouteAccessCookieService routeAccessCookieService) {
        this.roleService = roleService;
        this.routeAccessCookieService = routeAccessCookieService;
    }

    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('GLOBAL_SETTINGS_EDIT')")
    public List<RoleSummaryDTO> getRoleOverview(
            @RequestParam(required = false) @Min(0) Integer page,
            @RequestParam(required = false) @Positive Integer size) {
        return roleService.getRoleOverview(page, size);
    }

    @GetMapping("/getRoleDetails")
    @PreAuthorize("hasAuthority('GLOBAL_SETTINGS_EDIT')")
    public RoleDTO getRoleDetails(@RequestParam Long roleId) {
        return roleService.getRoleDetails(roleId);
    }

    /**
     * Returns the authenticated user's role details. Email is taken from the Bearer token —
     * callers cannot query another user's permissions.
     */
    @GetMapping("/getRoleDetailsByUserEmail")
    @PreAuthorize("isAuthenticated()")
    public RoleDTO getRoleDetailsByUserEmail(HttpServletResponse response) {
        RoleDTO role = roleService.getRoleDetailsForCurrentUser();
        routeAccessCookieService.attachRouteAccessCookie(response, role);
        return role;
    }

    @PutMapping("/permissions")
    @PreAuthorize("hasAuthority('GLOBAL_SETTINGS_EDIT')")
    public MessageResponse updatePermissions(
            @RequestParam Long roleId,
            @Valid @RequestBody RolePermissionUpdateRequestDTO request) {

        return roleService.updatePermissions(roleId, request);
    }

    @PutMapping("/duplicateRole")
    @PreAuthorize("hasAuthority('GLOBAL_SETTINGS_CREATE')")
    public MessageResponse duplicateRole(
            @RequestParam Long roleId,
            @Valid @RequestBody RoleDTO dto) {

        return roleService.duplicateRole(dto, roleId);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('GLOBAL_SETTINGS_CREATE')")
    public RoleDTO create(@Valid @RequestBody RoleDTO dto) {
        return roleService.create(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('GLOBAL_SETTINGS_EDIT')")
    public Optional<RoleDTO> update(@PathVariable Long id, @Valid @RequestBody RoleDTO dto) {
        return roleService.update(id, dto);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('GLOBAL_SETTINGS_EDIT')")
    public Optional<RoleDTO> findById(@PathVariable Long id) {
        return roleService.findById(id);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('GLOBAL_SETTINGS_EDIT')")
    public List<RoleDTO> findAll() {
        return roleService.findAll();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('GLOBAL_SETTINGS_DELETE')")
    public void deleteById(@PathVariable Long id) {
        roleService.deleteById(id);
    }
}
