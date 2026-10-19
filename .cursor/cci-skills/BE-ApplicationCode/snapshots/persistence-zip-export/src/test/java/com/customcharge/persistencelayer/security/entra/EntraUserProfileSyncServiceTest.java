package com.customcharge.persistencelayer.security.entra;

import com.customcharge.persistencelayer.entities.Role;
import com.customcharge.persistencelayer.entities.User;
import com.customcharge.persistencelayer.repository.RoleRepository;
import com.customcharge.persistencelayer.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntraUserProfileSyncServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private EntraGuidEntityResolutionService entraGuidEntityResolutionService;

    private EntraUserProfileSyncService service;

    @BeforeEach
    void setUp() {
        lenient()
                .when(entraGuidEntityResolutionService.resolveForPersistence(anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        service = new EntraUserProfileSyncService(userRepository, roleRepository, entraGuidEntityResolutionService);
    }

    @Test
    void syncFromToken_persistsRoleWhenTokenMapsToKnownRole() {
        Role admin = new Role();
        ReflectionTestUtils.setField(admin, "id", 1L);
        admin.setCode("ADMIN");

        User user = new User();
        ReflectionTestUtils.setField(user, "id", 2L);
        user.setEmail("user@example.com");
        user.setActive(true);

        when(roleRepository.findByCodeIgnoreCase("ADMIN")).thenReturn(Optional.of(admin));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        JwtUserIdentity identity = new JwtUserIdentity(
                "sub",
                null,
                "user@example.com",
                "oid-1",
                null,
                "user@example.com",
                List.of("ADMIN"),
                List.of("group-a", "group-b"));

        User synced = service.syncFromToken(user, identity);

        assertThat(synced.getRole()).isEqualTo(admin);
        assertThat(synced.getGroups()).isEqualTo("[\"group-a\",\"group-b\"]");
        verify(userRepository).save(user);
    }

    @Test
    void syncFromToken_skipsSaveWhenRoleAlreadyMatches() {
        Role admin = new Role();
        ReflectionTestUtils.setField(admin, "id", 1L);
        admin.setCode("ADMIN");

        User user = new User();
        ReflectionTestUtils.setField(user, "id", 2L);
        user.setEmail("user@example.com");
        user.setRole(admin);

        JwtUserIdentity identity = new JwtUserIdentity(
                "sub",
                null,
                "user@example.com",
                "oid-1",
                null,
                "user@example.com",
                List.of("ADMIN"),
                List.of());

        User synced = service.syncFromToken(user, identity);

        assertThat(synced).isSameAs(user);
        verify(userRepository, never()).save(any());
    }

    @Test
    void syncFromToken_savesWhenGroupsChangeEvenIfRoleUnchanged() {
        Role admin = new Role();
        ReflectionTestUtils.setField(admin, "id", 1L);
        admin.setCode("ADMIN");

        User user = new User();
        ReflectionTestUtils.setField(user, "id", 2L);
        user.setEmail("user@example.com");
        user.setRole(admin);
        user.setGroups("[\"old-group\"]");

        when(roleRepository.findByCodeIgnoreCase("ADMIN")).thenReturn(Optional.of(admin));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        JwtUserIdentity identity = new JwtUserIdentity(
                "sub",
                null,
                "user@example.com",
                "oid-1",
                null,
                "user@example.com",
                List.of("ADMIN"),
                List.of("new-g"));

        User synced = service.syncFromToken(user, identity);

        assertThat(synced.getGroups()).isEqualTo("[\"new-g\"]");
        verify(userRepository).save(user);
    }

    @Test
    void syncFromToken_clearsGroupsWhenTokenHasNoneButUserHadGroups() {
        Role admin = new Role();
        ReflectionTestUtils.setField(admin, "id", 1L);
        admin.setCode("ADMIN");

        User user = new User();
        ReflectionTestUtils.setField(user, "id", 2L);
        user.setEmail("user@example.com");
        user.setRole(admin);
        user.setGroups("[\"stale\"]");

        when(roleRepository.findByCodeIgnoreCase("ADMIN")).thenReturn(Optional.of(admin));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        JwtUserIdentity identity = new JwtUserIdentity(
                "sub",
                null,
                "user@example.com",
                "oid-1",
                null,
                "user@example.com",
                List.of("ADMIN"),
                List.of());

        User synced = service.syncFromToken(user, identity);

        assertThat(synced.getGroups()).isNull();
        verify(userRepository).save(user);
    }
}
