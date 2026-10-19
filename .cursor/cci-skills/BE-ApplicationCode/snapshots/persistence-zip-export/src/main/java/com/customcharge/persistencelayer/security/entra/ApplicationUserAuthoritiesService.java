package com.customcharge.persistencelayer.security.entra;

import com.customcharge.persistencelayer.cache.PersistenceCacheNames;
import com.customcharge.persistencelayer.entities.User;
import com.customcharge.persistencelayer.entities.UserPermission;
import com.customcharge.persistencelayer.repository.RolePermissionRepository;
import com.customcharge.persistencelayer.repository.UserPermissionRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Resolves effective permission codes for a {@link User} (role grants plus {@code user_permissions} overrides)
 * as Spring Security {@link GrantedAuthority} values for {@code @PreAuthorize("hasAuthority('…')")}.
 */
@Service
@ConditionalOnEntraOidcSecurityEnabled
public class ApplicationUserAuthoritiesService {

    private final RolePermissionRepository rolePermissionRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final ObjectProvider<ApplicationUserAuthoritiesService> selfProvider;

    public ApplicationUserAuthoritiesService(
            RolePermissionRepository rolePermissionRepository,
            UserPermissionRepository userPermissionRepository,
            ObjectProvider<ApplicationUserAuthoritiesService> selfProvider) {
        this.rolePermissionRepository = rolePermissionRepository;
        this.userPermissionRepository = userPermissionRepository;
        this.selfProvider = selfProvider;
    }

    @Transactional(readOnly = true)
    public List<GrantedAuthority> resolveAuthorities(User user) {
        List<String> codes = selfProvider.getObject().resolveAuthorityCodes(user);
        List<GrantedAuthority> authorities = new ArrayList<>(codes.size());
        for (String code : codes) {
            authorities.add(new SimpleGrantedAuthority(code));
        }
        return authorities;
    }

    /**
     * Cached permission codes only — {@link SimpleGrantedAuthority} is not deserializable by the dashboard
     * Redis cache {@code ObjectMapper} type validator. Key includes {@code role.id} so token-driven role sync
     * does not reuse authorities from a previous role.
     */
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = PersistenceCacheNames.USER_AUTHORITIES,
            key = "#user.id + '_' + (#user.role != null ? #user.role.id : 0)")
    public List<String> resolveAuthorityCodes(User user) {
        if (user.getRole() == null) {
            return List.of();
        }
        Set<String> codes = new LinkedHashSet<>(
                rolePermissionRepository.findPermissionCodesByRoleId(user.getRole().getId()));
        List<UserPermission> overrides = userPermissionRepository.findAllByUserIdWithPermission(user.getId());
        for (UserPermission up : overrides) {
            if (up.getPermission() == null || up.getPermission().getCode() == null) {
                continue;
            }
            String code = up.getPermission().getCode();
            if (Boolean.FALSE.equals(up.getAllowed())) {
                codes.remove(code);
            } else if (Boolean.TRUE.equals(up.getAllowed())) {
                codes.add(code);
            }
        }
        return new ArrayList<>(codes);
    }
}
