package com.customcharge.persistencelayer.security.entra;

import com.customcharge.persistencelayer.entities.Permission;
import com.customcharge.persistencelayer.entities.Role;
import com.customcharge.persistencelayer.entities.User;
import com.customcharge.persistencelayer.entities.UserPermission;
import com.customcharge.persistencelayer.repository.RolePermissionRepository;
import com.customcharge.persistencelayer.repository.UserPermissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationUserAuthoritiesServiceTest {

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @Mock
    private UserPermissionRepository userPermissionRepository;

    @Mock
    private ObjectProvider<ApplicationUserAuthoritiesService> selfProvider;

    @InjectMocks
    private ApplicationUserAuthoritiesService service;

    @BeforeEach
    void setUp() {
        lenient().when(selfProvider.getObject()).thenReturn(service);
    }

    @Test
    void resolveAuthorities_appliesDenyOverride() {
        Role role = new Role();
        role.setId(10L);
        User user = new User();
        user.setId(20L);
        user.setRole(role);

        when(rolePermissionRepository.findPermissionCodesByRoleId(10L)).thenReturn(List.of("A_VIEW", "B_VIEW"));

        UserPermission deny = new UserPermission();
        Permission p = new Permission();
        p.setCode("B_VIEW");
        deny.setPermission(p);
        deny.setAllowed(false);
        when(userPermissionRepository.findAllByUserIdWithPermission(20L)).thenReturn(List.of(deny));

        List<String> codes = service.resolveAuthorities(user).stream().map(GrantedAuthority::getAuthority).toList();
        assertThat(codes).containsExactly("A_VIEW");
    }

    @Test
    void resolveAuthorityCodes_returnsEmptyWhenRoleMissing() {
        User user = new User();
        user.setId(30L);
        user.setRole(null);

        assertThat(service.resolveAuthorityCodes(user)).isEmpty();
    }

    @Test
    void resolveAuthorityCodes_reflectsRoleGrants() {
        Role role = new Role();
        role.setId(11L);
        User user = new User();
        user.setId(21L);
        user.setRole(role);

        when(rolePermissionRepository.findPermissionCodesByRoleId(11L)).thenReturn(List.of("C_VIEW", "D_EDIT"));
        when(userPermissionRepository.findAllByUserIdWithPermission(21L)).thenReturn(List.of());

        assertThat(service.resolveAuthorityCodes(user)).containsExactlyInAnyOrder("C_VIEW", "D_EDIT");
    }
}
