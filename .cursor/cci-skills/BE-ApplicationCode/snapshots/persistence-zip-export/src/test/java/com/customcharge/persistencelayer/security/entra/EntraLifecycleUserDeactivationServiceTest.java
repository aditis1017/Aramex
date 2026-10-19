package com.customcharge.persistencelayer.security.entra;

import com.customcharge.persistencelayer.entities.Role;
import com.customcharge.persistencelayer.entities.User;
import com.customcharge.persistencelayer.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntraLifecycleUserDeactivationServiceTest {

    @Mock
    private UserRepository userRepository;

    private EntraLifecycleUserDeactivationService service;

    @BeforeEach
    void setUp() {
        service = new EntraLifecycleUserDeactivationService(userRepository);
    }

    @Test
    void deactivate_missingUser_returnsEmpty() {
        when(userRepository.findByEmailIgnoreCaseWithRole("nope@example.com")).thenReturn(Optional.empty());

        assertThat(service.deactivateByPrimaryEmail("nope@example.com")).isEmpty();
        verify(userRepository, never()).save(any());
    }

    @Test
    void deactivate_blankEmail_returnsEmpty() {
        assertThat(service.deactivateByPrimaryEmail("   ")).isEmpty();
        assertThat(service.deactivateByPrimaryEmail(null)).isEmpty();
        verify(userRepository, never()).findByEmailIgnoreCaseWithRole(any());
    }

    @Test
    void deactivate_activeUser_setsInactiveAndSaves() {
        Role role = new Role();
        ReflectionTestUtils.setField(role, "id", 1L);
        User user = new User();
        ReflectionTestUtils.setField(user, "id", 9L);
        user.setEmail("u@example.com");
        user.setActive(true);
        user.setRole(role);

        when(userRepository.findByEmailIgnoreCaseWithRole("u@example.com")).thenReturn(Optional.of(user));

        Optional<User> out = service.deactivateByPrimaryEmail("u@example.com");

        assertThat(out).isPresent();
        assertThat(out.get().isActive()).isFalse();
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getUpdatedBy()).isEqualTo(EntraLifecycleUserDeactivationService.UPDATED_BY_ENTRA_LIFECYCLE);
        assertThat(captor.getValue().getUpdatedAt()).isNotNull();
    }

    @Test
    void deactivate_alreadyInactive_returnsUserWithoutSave() {
        Role role = new Role();
        ReflectionTestUtils.setField(role, "id", 1L);
        User user = new User();
        ReflectionTestUtils.setField(user, "id", 9L);
        user.setEmail("u@example.com");
        user.setActive(false);
        user.setRole(role);

        when(userRepository.findByEmailIgnoreCaseWithRole("u@example.com")).thenReturn(Optional.of(user));

        Optional<User> out = service.deactivateByPrimaryEmail("u@example.com");

        assertThat(out).isPresent();
        verify(userRepository, never()).save(any());
    }
}
