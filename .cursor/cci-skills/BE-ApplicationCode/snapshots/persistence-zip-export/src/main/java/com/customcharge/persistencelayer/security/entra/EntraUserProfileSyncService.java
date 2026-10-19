package com.customcharge.persistencelayer.security.entra;

import com.customcharge.persistencelayer.entities.Role;
import com.customcharge.persistencelayer.entities.User;
import com.customcharge.persistencelayer.repository.RoleRepository;
import com.customcharge.persistencelayer.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * After JWT validation, maps Entra {@code roles} claims to a persisted {@link Role}, updates {@code users.role_id}
 * when the token implies a different application role, and refreshes {@code users.groups} from the token's
 * {@code groups} claim (JSON array in the column).
 */
@Service
@ConditionalOnEntraOidcSecurityEnabled
public class EntraUserProfileSyncService {

    private static final Logger log = LoggerFactory.getLogger(EntraUserProfileSyncService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EntraGuidEntityResolutionService entraGuidEntityResolutionService;

    public EntraUserProfileSyncService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            EntraGuidEntityResolutionService entraGuidEntityResolutionService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.entraGuidEntityResolutionService = entraGuidEntityResolutionService;
    }

    @Transactional
    public User syncFromToken(User user, JwtUserIdentity identity) {
        String newGroups =
                EntraTokenGroupsFormatter.toUserGroupsColumn(
                        identity.tokenGroups(), entraGuidEntityResolutionService::resolveForPersistence);
        boolean groupsChanged = !Objects.equals(user.getGroups(), newGroups);
        user.setGroups(newGroups);

        Optional<Role> mappedRole = resolveRoleFromToken(identity.tokenRoles());
        if (mappedRole.isEmpty()) {
            if (!identity.tokenRoles().isEmpty()) {
                log.debug(
                        "Entra profile sync: TOKEN_ROLE_UNKNOWN userId={} tokenRoles={}",
                        user.getId(),
                        identity.tokenRoles());
            }
            if (groupsChanged) {
                user.setUpdatedAt(Instant.now());
                return userRepository.save(user);
            }
            return user;
        }

        Role role = mappedRole.get();
        boolean roleChanged = user.getRole() == null || !Objects.equals(user.getRole().getId(), role.getId());
        if (roleChanged) {
            user.setRole(role);
        }
        if (!roleChanged && !groupsChanged) {
            return user;
        }

        user.setUpdatedAt(Instant.now());
        if (roleChanged) {
            log.debug(
                    "Entra profile sync: USER_ROLE_SYNCED userId={} roleId={} roleCode={}",
                    user.getId(),
                    role.getId(),
                    role.getCode());
        } else {
            log.debug("Entra profile sync: USER_GROUPS_SYNCED userId={}", user.getId());
        }

        User saved = userRepository.save(user);
        log.debug(
                "Synced application role from token for userId={} roleCode={} tokenRoles={}",
                saved.getId(),
                role.getCode(),
                identity.tokenRoles());
        return saved;
    }

    private Optional<Role> resolveRoleFromToken(List<String> tokenRoles) {
        for (String tokenRole : tokenRoles) {
            if (tokenRole == null || tokenRole.isBlank()) {
                continue;
            }
            Optional<Role> role = roleRepository.findByCodeIgnoreCase(tokenRole.trim());
            if (role.isPresent()) {
                return role;
            }
        }
        return Optional.empty();
    }
}
