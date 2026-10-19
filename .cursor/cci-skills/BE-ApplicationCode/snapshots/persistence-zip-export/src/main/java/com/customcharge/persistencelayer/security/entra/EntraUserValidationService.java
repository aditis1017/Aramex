package com.customcharge.persistencelayer.security.entra;

import com.customcharge.persistencelayer.entities.Role;
import com.customcharge.persistencelayer.entities.User;
import com.customcharge.persistencelayer.repository.RoleRepository;
import com.customcharge.persistencelayer.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Resolves or provisions {@link User} rows after JWT validation: just-in-time creation when unknown,
 * reactivation and profile refresh when inactive, then syncs role and {@code groups} from token claims and enforces policy.
 * When token {@code roles} claims do not map to a database role, {@code groups} are still synced and login is allowed;
 * role assignment and role policy checks follow the mapped-role path only when a token role matches {@code roles.code}.
 */
@Service
@ConditionalOnEntraOidcSecurityEnabled
public class EntraUserValidationService {

    private static final Logger log = LoggerFactory.getLogger(EntraUserValidationService.class);

    /** Marker password for Entra-only users (local password login must remain disabled for these rows). */
    static final String ENTRA_JIT_PASSWORD_PLACEHOLDER = "{noop}entra-jit-not-for-local-login";

    static final String JIT_CREATED_BY = "ENTRA_JIT";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EntraOidcSecurityProperties properties;
    private final EntraUserProfileSyncService profileSyncService;
    private final EntraGuidEntityResolutionService entraGuidEntityResolutionService;

    public EntraUserValidationService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            EntraOidcSecurityProperties properties,
            EntraUserProfileSyncService profileSyncService,
            EntraGuidEntityResolutionService entraGuidEntityResolutionService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.properties = properties;
        this.profileSyncService = profileSyncService;
        this.entraGuidEntityResolutionService = entraGuidEntityResolutionService;
    }

    /**
     * Resolves the Entra identity to an active application {@link User} with a valid role, creating or
     * reactivating the row when needed.
     */
    @Transactional
    public User loadAndValidate(JwtUserIdentity identity) {
        log.debug(
                "Entra user validation: USER_LOOKUP email={} tokenRoles={} tokenGroups={}",
                identity.primaryLookupKey(),
                identity.tokenRoles(),
                identity.tokenGroups());

        if (identity.tokenRoles().isEmpty()) {
            log.debug("Entra user validation: TOKEN_ROLES_MISSING email={}", identity.primaryLookupKey());
            throw new EntraRequestDeniedException(
                    HttpStatus.FORBIDDEN,
                    "TOKEN_ROLES_MISSING",
                    "Access token must include an application role claim to provision or authorize the user");
        }

        Optional<Role> roleFromToken = resolveRoleFromToken(identity.tokenRoles());
        if (roleFromToken.isEmpty()) {
            log.debug(
                    "Entra user validation: TOKEN_ROLE_UNMAPPED proceeding email={} tokenRoles={} tokenGroups={}",
                    identity.primaryLookupKey(),
                    identity.tokenRoles(),
                    identity.tokenGroups());
        }

        Optional<User> existing = userRepository.findByEmailIgnoreCaseWithRole(identity.primaryLookupKey());
        User user;
        if (existing.isEmpty()) {
            user = createUserFromToken(identity, roleFromToken);
            log.debug("Entra user validation: USER_JIT_CREATED userId={} email={}", user.getId(), user.getEmail());
        } else {
            user = existing.get();
            if (!user.isActive()) {
                user.setActive(true);
                applyTokenDetails(user, identity, roleFromToken);
                user.setUpdatedAt(Instant.now());
                user = userRepository.save(user);
                log.debug("Entra user validation: USER_REACTIVATED userId={} email={}", user.getId(), user.getEmail());
            }
        }

        if (!user.isActive()) {
            log.debug("Entra user validation: USER_INACTIVE userId={}", user.getId());
            throw new EntraRequestDeniedException(
                    HttpStatus.FORBIDDEN,
                    "USER_INACTIVE",
                    "User account is inactive");
        }

        user = profileSyncService.syncFromToken(user, identity);

        if (roleFromToken.isPresent()) {
            if (user.getRole() == null) {
                log.debug("Entra user validation: USER_MISSING_ROLE userId={}", user.getId());
                throw new EntraRequestDeniedException(
                        HttpStatus.FORBIDDEN,
                        "MISSING_APPLICATION_ROLE",
                        "User is not assigned an application role");
            }

            if (properties.getRequiredApplicationRoleName() != null
                    && !properties.getRequiredApplicationRoleName().equalsIgnoreCase(user.getRole().getCode())) {
                log.debug(
                        "Entra user validation: USER_INSUFFICIENT_ROLE required={} actual={}",
                        properties.getRequiredApplicationRoleName(),
                        user.getRole().getCode());
                throw new EntraRequestDeniedException(
                        HttpStatus.FORBIDDEN,
                        "INSUFFICIENT_ROLE",
                        "User does not have required application access");
            }
        }

        return user;
    }

    private User createUserFromToken(JwtUserIdentity identity, Optional<Role> roleFromToken) {
        User user = new User();
        user.setEmail(identity.primaryLookupKey().trim());
        user.setUsername(resolveUniqueUsername(identity, null));
        user.setPassword(ENTRA_JIT_PASSWORD_PLACEHOLDER);
        user.setRole(roleFromToken.orElseGet(this::resolveJitProvisioningRoleWhenTokenRoleUnmapped));
        applyTokenGroups(user, identity);
        user.setActive(true);
        user.setCreatedBy(JIT_CREATED_BY);
        user.setUpdatedAt(Instant.now());
        return userRepository.save(user);
    }

    private void applyTokenDetails(User user, JwtUserIdentity identity, Optional<Role> roleFromToken) {
        user.setEmail(identity.primaryLookupKey().trim());
        user.setUsername(resolveUniqueUsername(identity, user.getId()));
        roleFromToken.ifPresent(user::setRole);
        applyTokenGroups(user, identity);
    }

    private void applyTokenGroups(User user, JwtUserIdentity identity) {
        user.setGroups(
                EntraTokenGroupsFormatter.toUserGroupsColumn(
                        identity.tokenGroups(), entraGuidEntityResolutionService::resolveForPersistence));
    }

    /**
     * Derives a stable username from token claims; ensures uniqueness when possible without an extra round-trip.
     */
    private String resolveUniqueUsername(JwtUserIdentity identity, Long excludeUserId) {
        String candidate = resolveUsernameCandidate(identity);
        boolean taken =
                excludeUserId == null
                        ? userRepository.existsByUsernameIgnoreCase(candidate)
                        : userRepository.existsByUsernameIgnoreCaseAndIdNot(candidate, excludeUserId);
        if (!taken) {
            return candidate;
        }
        String suffix = Optional.ofNullable(identity.subject())
                .map(s -> s.replaceAll("[^a-zA-Z0-9_]", ""))
                .filter(s -> !s.isEmpty())
                .map(s -> s.length() > 24 ? s.substring(0, 24) : s)
                .orElse("sub");
        String combined = candidate + "_" + suffix;
        return combined.length() <= 100 ? combined : combined.substring(0, 100);
    }

    private static String resolveUsernameCandidate(JwtUserIdentity identity) {
        Optional<String> preferred = JwtUserIdentity.firstPresent(identity.preferredUsername());
        if (preferred.isPresent()) {
            return truncate(preferred.get(), 100);
        }
        String email = identity.primaryLookupKey().trim();
        String fromEmail = email.toLowerCase().replace('@', '_').replaceAll("[^a-z0-9_]+", "_");
        fromEmail = stripUnderscoreEdges(fromEmail);
        if (!fromEmail.isEmpty()) {
            return truncate(fromEmail, 100);
        }
        String sub = identity.subject();
        if (sub != null && !sub.isBlank()) {
            return truncate("user_" + sub.replaceAll("[^a-zA-Z0-9_]", "_"), 100);
        }
        return "entra_user";
    }

    private static String stripUnderscoreEdges(String s) {
        int start = 0;
        int end = s.length();
        while (start < end && s.charAt(start) == '_') {
            start++;
        }
        while (end > start && s.charAt(end - 1) == '_') {
            end--;
        }
        return s.substring(start, end);
    }

    private static String truncate(String s, int max) {
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max);
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

    /**
     * {@code users.role_id} is NOT NULL; when JIT-provisioning a user whose token roles do not map to {@code roles.code},
     * assign the configured provisioning role so the row can be saved. Token role policy checks remain skipped for this login.
     */
    private Role resolveJitProvisioningRoleWhenTokenRoleUnmapped() {
        String configuredCode = properties.getJitProvisioningRoleCode();
        if (configuredCode == null || configuredCode.isBlank()) {
            throw new EntraRequestDeniedException(
                    HttpStatus.FORBIDDEN,
                    "TOKEN_ROLE_UNMAPPED",
                    "Cannot provision user: token role is unmapped and jit-provisioning-role-code is not configured");
        }
        return roleRepository
                .findByCodeIgnoreCase(configuredCode.trim())
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "Configured jit-provisioning-role-code '"
                                                + configuredCode.trim()
                                                + "' was not found in the roles table"));
    }
}
