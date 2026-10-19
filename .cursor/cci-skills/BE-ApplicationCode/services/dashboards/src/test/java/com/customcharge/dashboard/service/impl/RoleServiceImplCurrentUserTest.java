package com.customcharge.dashboard.service.impl;

import com.customcharge.dashboard.dto.RoleDTO;
import com.customcharge.persistencelayer.entities.Role;
import com.customcharge.persistencelayer.entities.User;
import com.customcharge.persistencelayer.exception.ResourceNotFoundException;
import com.customcharge.persistencelayer.repository.ModuleRepository;
import com.customcharge.persistencelayer.repository.PermissionRepository;
import com.customcharge.persistencelayer.repository.RolePermissionRepository;
import com.customcharge.persistencelayer.repository.RoleRepository;
import com.customcharge.persistencelayer.repository.UserRepository;
import com.customcharge.persistencelayer.security.entra.EntraRequestDeniedException;
import com.customcharge.persistencelayer.security.entra.EntraSecurityContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplCurrentUserTest {

    @Mock
    private RoleRepository roleRepository;
    @Mock
    private ModuleRepository moduleRepository;
    @Mock
    private ModuleLookupService moduleLookupService;
    @Mock
    private RolePermissionRepository rolePermRepository;
    @Mock
    private PermissionRepository permissionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ObjectProvider<RoleServiceImpl> selfProvider;

    @InjectMocks
    private RoleServiceImpl roleService;

    @Test
    void getRoleDetailsForCurrentUserReturnsAuthenticatedUsersRole() {
        lenient().when(selfProvider.getObject()).thenReturn(roleService);
        Role role = new Role();
        role.setId(7L);
        role.setCode("VIEWER");
        role.setName("Viewer");

        User user = new User();
        user.setEmail("user@example.com");
        user.setRole(role);

        when(roleRepository.findById(7L)).thenReturn(Optional.of(role));
        when(rolePermRepository.findByRoleIdWithDetails(7L)).thenReturn(Collections.emptyList());
        when(moduleLookupService.findAllModules()).thenReturn(Collections.emptyList());
        when(userRepository.countByRole_Id(7L)).thenReturn(1L);

        try (MockedStatic<EntraSecurityContext> securityContext = mockStatic(EntraSecurityContext.class)) {
            securityContext.when(EntraSecurityContext::requireCurrentUser).thenReturn(user);

            RoleDTO result = roleService.getRoleDetailsForCurrentUser();

            assertEquals(7L, result.getId());
            assertEquals("VIEWER", result.getCode());
        }
    }

    @Test
    void getRoleDetailsForCurrentUserThrowsWhenRoleMissing() {
        User user = new User();
        user.setEmail("user@example.com");
        user.setRole(null);

        try (MockedStatic<EntraSecurityContext> securityContext = mockStatic(EntraSecurityContext.class)) {
            securityContext.when(EntraSecurityContext::requireCurrentUser).thenReturn(user);

            assertThrows(ResourceNotFoundException.class, () -> roleService.getRoleDetailsForCurrentUser());
        }
    }

    @Test
    void getRoleDetailsForCurrentUserPropagatesUnauthorized() {
        try (MockedStatic<EntraSecurityContext> securityContext = mockStatic(EntraSecurityContext.class)) {
            securityContext.when(EntraSecurityContext::requireCurrentUser)
                    .thenThrow(new EntraRequestDeniedException(
                            org.springframework.http.HttpStatus.UNAUTHORIZED,
                            "UNAUTHORIZED",
                            "Authenticated user is required"));

            assertThrows(EntraRequestDeniedException.class, () -> roleService.getRoleDetailsForCurrentUser());
        }
    }
}
