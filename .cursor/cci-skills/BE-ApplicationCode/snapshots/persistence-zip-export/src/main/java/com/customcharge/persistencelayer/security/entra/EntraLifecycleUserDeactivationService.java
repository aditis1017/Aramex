package com.customcharge.persistencelayer.security.entra;

import com.customcharge.persistencelayer.entities.User;
import com.customcharge.persistencelayer.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Marks application users inactive when Entra (or an Entra-connected automation) signals account disablement.
 */
@Service
@ConditionalOnEntraOidcSecurityEnabled
public class EntraLifecycleUserDeactivationService {

    static final String UPDATED_BY_ENTRA_LIFECYCLE = "ENTRA_LIFECYCLE";

    private final UserRepository userRepository;

    public EntraLifecycleUserDeactivationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * @param primaryEmail case-insensitive match against {@code users.email} (same key as Entra JWT primary lookup)
     * @return empty if no user row exists
     */
    @Transactional
    public Optional<User> deactivateByPrimaryEmail(String primaryEmail) {
        String key = primaryEmail == null ? "" : primaryEmail.trim();
        if (key.isEmpty()) {
            return Optional.empty();
        }
        Optional<User> existing = userRepository.findByEmailIgnoreCaseWithRole(key);
        if (existing.isEmpty()) {
            return Optional.empty();
        }
        User user = existing.get();
        if (user.isActive()) {
            user.setActive(false);
            user.setUpdatedBy(UPDATED_BY_ENTRA_LIFECYCLE);
            user.setUpdatedAt(Instant.now());
            userRepository.save(user);
        }
        return Optional.of(user);
    }
}
