package com.customcharge.persistencelayer.security.entra;

import com.customcharge.persistencelayer.entities.Role;
import com.customcharge.persistencelayer.entities.User;
import com.customcharge.persistencelayer.repository.RoleRepository;
import com.customcharge.persistencelayer.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntraUserValidationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private EntraUserProfileSyncService profileSyncService;

    @Mock
    private EntraGuidEntityResolutionService entraGuidEntityResolutionService;

    private EntraOidcSecurityProperties properties;

    private EntraUserValidationService service;

    @BeforeEach
    void setUp() {
        properties = new EntraOidcSecurityProperties();
        lenient()
                .when(entraGuidEntityResolutionService.resolveForPersistence(anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        service = new EntraUserValidationService(
                userRepository, roleRepository, properties, profileSyncService, entraGuidEntityResolutionService);
    }

    private Role appUserRole() {
        Role role = new Role();
        role.setCode("APP_USER");
        ReflectionTestUtils.setField(role, "id", 5L);
        return role;
    }

    @Test
    void loadAndValidate_success() {
        Role role = appUserRole();
        when(roleRepository.findByCodeIgnoreCase("APP_USER")).thenReturn(Optional.of(role));

        User user = new User();
        ReflectionTestUtils.setField(user, "id", 1L);
        user.setEmail("user@example.com");
        user.setUsername("user");
        user.setActive(true);
        user.setRole(role);

        when(userRepository.findByEmailIgnoreCaseWithRole("user@example.com")).thenReturn(Optional.of(user));
        when(profileSyncService.syncFromToken(any(User.class), any(JwtUserIdentity.class))).thenAnswer(inv -> inv.getArgument(0));

        JwtUserIdentity identity = new JwtUserIdentity(
                "sub",
                null,
                "user@example.com",
                null,
                null,
                "user@example.com",
                List.of("APP_USER"),
                List.of());

        User out = service.loadAndValidate(identity);

        assertThat(out.getEmail()).isEqualTo("user@example.com");
        assertThat(out.getRole().getCode()).isEqualTo("APP_USER");
        verify(profileSyncService).syncFromToken(user, identity);
        verify(userRepository, never()).save(any());
    }

    @Test
    void loadAndValidate_tokenRolesMissing() {
        JwtUserIdentity identity = new JwtUserIdentity(
                "sub",
                null,
                "missing@example.com",
                null,
                null,
                "missing@example.com",
                List.of(),
                List.of());

        assertThatThrownBy(() -> service.loadAndValidate(identity))
                .isInstanceOf(EntraRequestDeniedException.class)
                .extracting(ex -> ((EntraRequestDeniedException) ex).getHttpStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
        verify(userRepository, never()).findByEmailIgnoreCaseWithRole(any());
    }

    @Test
    void loadAndValidate_tokenRolesUnmapped_allowsExistingUserAndSyncsGroups() {
        when(roleRepository.findByCodeIgnoreCase("UNKNOWN_APP_ROLE")).thenReturn(Optional.empty());

        Role existingRole = appUserRole();
        User user = new User();
        ReflectionTestUtils.setField(user, "id", 1L);
        user.setEmail("user@example.com");
        user.setUsername("user");
        user.setActive(true);
        user.setRole(existingRole);

        when(userRepository.findByEmailIgnoreCaseWithRole("user@example.com")).thenReturn(Optional.of(user));
        when(profileSyncService.syncFromToken(any(User.class), any(JwtUserIdentity.class)))
                .thenAnswer(inv -> {
                    User u = inv.getArgument(0);
                    u.setGroups("[\"0b8ef38c-60c4-4466-ab15-8b10f2460031\"]");
                    return u;
                });

        JwtUserIdentity identity = new JwtUserIdentity(
                "sub",
                null,
                "user@example.com",
                null,
                null,
                "user@example.com",
                List.of("UNKNOWN_APP_ROLE"),
                List.of("0b8ef38c-60c4-4466-ab15-8b10f2460031"));

        User out = service.loadAndValidate(identity);

        assertThat(out.getRole().getCode()).isEqualTo("APP_USER");
        assertThat(out.getGroups()).isEqualTo("[\"0b8ef38c-60c4-4466-ab15-8b10f2460031\"]");
        verify(profileSyncService).syncFromToken(user, identity);
        verify(userRepository, never()).save(any());
    }

    @Test
    void loadAndValidate_tokenRolesUnmapped_jitCreatesUserWhenNotFound() {
        properties.setJitProvisioningRoleCode("VIEWER");

        Role viewerRole = new Role();
        viewerRole.setCode("VIEWER");
        ReflectionTestUtils.setField(viewerRole, "id", 9L);

        when(roleRepository.findByCodeIgnoreCase("UNKNOWN_APP_ROLE")).thenReturn(Optional.empty());
        when(roleRepository.findByCodeIgnoreCase("VIEWER")).thenReturn(Optional.of(viewerRole));
        when(userRepository.findByEmailIgnoreCaseWithRole("new@example.com")).thenReturn(Optional.empty());
        when(userRepository.existsByUsernameIgnoreCase(eq("new_example_com"))).thenReturn(false);
        when(userRepository.save(any(User.class)))
                .thenAnswer(inv -> {
                    User u = inv.getArgument(0);
                    ReflectionTestUtils.setField(u, "id", 42L);
                    return u;
                });
        when(profileSyncService.syncFromToken(any(User.class), any(JwtUserIdentity.class))).thenAnswer(inv -> inv.getArgument(0));

        JwtUserIdentity identity = new JwtUserIdentity(
                "sub-1",
                null,
                "new@example.com",
                null,
                null,
                "new@example.com",
                List.of("UNKNOWN_APP_ROLE"),
                List.of("2c5d95dc-56a4-49a6-9939-d0d59059e4ca"));

        User out = service.loadAndValidate(identity);

        assertThat(out.getId()).isEqualTo(42L);
        assertThat(out.isActive()).isTrue();
        assertThat(out.getRole().getCode()).isEqualTo("VIEWER");
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getGroups())
                .isEqualTo("[\"2c5d95dc-56a4-49a6-9939-d0d59059e4ca\"]");
        verify(profileSyncService).syncFromToken(any(User.class), eq(identity));
    }

    @Test
    void loadAndValidate_tokenRolesUnmapped_jitDeniedWhenProvisioningRoleNotConfigured() {
        when(roleRepository.findByCodeIgnoreCase("UNKNOWN_APP_ROLE")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCaseWithRole("new@example.com")).thenReturn(Optional.empty());

        JwtUserIdentity identity = new JwtUserIdentity(
                "sub-1",
                null,
                "new@example.com",
                null,
                null,
                "new@example.com",
                List.of("UNKNOWN_APP_ROLE"),
                List.of("group-a"));

        assertThatThrownBy(() -> service.loadAndValidate(identity))
                .isInstanceOf(EntraRequestDeniedException.class)
                .satisfies(ex -> {
                    EntraRequestDeniedException denied = (EntraRequestDeniedException) ex;
                    assertThat(denied.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(denied.getErrorCode()).isEqualTo("TOKEN_ROLE_UNMAPPED");
                });
        verify(userRepository, never()).save(any());
    }

    @Test
    void loadAndValidate_jitCreatesUserWhenNotFound() {
        Role role = appUserRole();
        when(roleRepository.findByCodeIgnoreCase("APP_USER")).thenReturn(Optional.of(role));
        when(userRepository.findByEmailIgnoreCaseWithRole("new@example.com")).thenReturn(Optional.empty());
        when(userRepository.existsByUsernameIgnoreCase(eq("new_example_com"))).thenReturn(false);
        when(userRepository.save(any(User.class)))
                .thenAnswer(inv -> {
                    User u = inv.getArgument(0);
                    ReflectionTestUtils.setField(u, "id", 42L);
                    return u;
                });
        when(profileSyncService.syncFromToken(any(User.class), any(JwtUserIdentity.class))).thenAnswer(inv -> inv.getArgument(0));

        JwtUserIdentity identity = new JwtUserIdentity(
                "sub-1",
                null,
                "new@example.com",
                null,
                null,
                "new@example.com",
                List.of("APP_USER"),
                List.of("11111111-1111-1111-1111-111111111111", "group-b"));

        User out = service.loadAndValidate(identity);

        assertThat(out.getId()).isEqualTo(42L);
        assertThat(out.isActive()).isTrue();
        assertThat(out.getRole().getCode()).isEqualTo("APP_USER");
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getGroups())
                .isEqualTo("[\"11111111-1111-1111-1111-111111111111\",\"group-b\"]");
        verify(profileSyncService).syncFromToken(any(User.class), eq(identity));
    }

    @Test
    void loadAndValidate_reactivatesInactiveUser() {
        Role role = appUserRole();
        when(roleRepository.findByCodeIgnoreCase("APP_USER")).thenReturn(Optional.of(role));

        User user = new User();
        ReflectionTestUtils.setField(user, "id", 1L);
        user.setEmail("user@example.com");
        user.setUsername("user_example_com");
        user.setActive(false);
        user.setRole(role);

        when(userRepository.findByEmailIgnoreCaseWithRole("user@example.com")).thenReturn(Optional.of(user));
        when(userRepository.existsByUsernameIgnoreCaseAndIdNot(eq("user_example_com"), eq(1L))).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(profileSyncService.syncFromToken(any(User.class), any(JwtUserIdentity.class))).thenAnswer(inv -> inv.getArgument(0));

        JwtUserIdentity identity = new JwtUserIdentity(
                "sub",
                null,
                "user@example.com",
                null,
                null,
                "user@example.com",
                List.of("APP_USER"),
                List.of());

        User out = service.loadAndValidate(identity);

        assertThat(out.isActive()).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    void loadAndValidate_requiredRoleMismatch() {
        properties.setRequiredApplicationRoleName("ADMIN");

        Role role = appUserRole();
        when(roleRepository.findByCodeIgnoreCase("APP_USER")).thenReturn(Optional.of(role));

        User user = new User();
        ReflectionTestUtils.setField(user, "id", 1L);
        user.setEmail("user@example.com");
        user.setActive(true);
        user.setRole(role);

        when(userRepository.findByEmailIgnoreCaseWithRole("user@example.com")).thenReturn(Optional.of(user));
        when(profileSyncService.syncFromToken(any(User.class), any(JwtUserIdentity.class))).thenAnswer(inv -> inv.getArgument(0));

        JwtUserIdentity identity = new JwtUserIdentity(
                "sub",
                null,
                "user@example.com",
                null,
                null,
                "user@example.com",
                List.of("APP_USER"),
                List.of());

        assertThatThrownBy(() -> service.loadAndValidate(identity))
                .isInstanceOf(EntraRequestDeniedException.class)
                .extracting(ex -> ((EntraRequestDeniedException) ex).getHttpStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }
}
