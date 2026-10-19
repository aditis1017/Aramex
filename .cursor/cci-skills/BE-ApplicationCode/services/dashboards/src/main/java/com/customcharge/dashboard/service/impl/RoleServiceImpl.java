package com.customcharge.dashboard.service.impl;

import com.customcharge.dashboard.constants.RoleConstants;
import com.customcharge.dashboard.constants.RoleLogMessages;
import com.customcharge.dashboard.constants.RoleMessageConstants;
import com.customcharge.dashboard.dto.ModulePermissionRequestDTO;
import com.customcharge.dashboard.dto.RoleDTO;
import com.customcharge.dashboard.dto.RolePermissionDTO;
import com.customcharge.dashboard.dto.RolePermissionUpdateRequestDTO;
import com.customcharge.dashboard.service.RoleService;
import com.customcharge.persistencelayer.dto.RoleSummaryDTO;
import com.customcharge.persistencelayer.dto.response.MessageResponse;
import com.customcharge.persistencelayer.entities.Module;
import com.customcharge.persistencelayer.entities.Permission;
import com.customcharge.persistencelayer.entities.Role;
import com.customcharge.persistencelayer.entities.RolePermission;
import com.customcharge.persistencelayer.entities.User;
import com.customcharge.persistencelayer.entities.RolePermissionId;
import com.customcharge.persistencelayer.exception.AbstractApiException;
import com.customcharge.persistencelayer.exception.BadRequestException;
import com.customcharge.persistencelayer.exception.ConflictException;
import com.customcharge.persistencelayer.exception.InternalServerException;
import com.customcharge.persistencelayer.exception.ResourceNotFoundException;
import com.customcharge.persistencelayer.exception.ValidationException;
import com.customcharge.persistencelayer.repository.ModuleRepository;
import com.customcharge.persistencelayer.repository.PermissionRepository;
import com.customcharge.persistencelayer.repository.RolePermissionRepository;
import com.customcharge.persistencelayer.repository.RoleRepository;
import com.customcharge.persistencelayer.repository.UserRepository;
import com.customcharge.persistencelayer.security.entra.EntraRequestDeniedException;
import com.customcharge.persistencelayer.security.entra.EntraSecurityContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.customcharge.dashboard.cache.DashboardCacheNames;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final ModuleRepository moduleRepository;
    private final ModuleLookupService moduleLookupService;
    private final RolePermissionRepository rolePermRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final ObjectProvider<RoleServiceImpl> selfProvider;

    @Override
    @Transactional(readOnly = false)
    @Caching(evict = {
            @CacheEvict(cacheNames = DashboardCacheNames.ROLES_FIND_ALL, allEntries = true),
            @CacheEvict(cacheNames = DashboardCacheNames.ROLE_OVERVIEW, allEntries = true),
            @CacheEvict(cacheNames = DashboardCacheNames.PERMISSION_MATRIX, allEntries = true)
    })
    public RoleDTO create(@Valid RoleDTO dto) {
        String name = effectiveName(dto);
        if (name == null) {
            throw new BadRequestException(RoleMessageConstants.ROLE_NAME_REQUIRED);
        }
        dto.setName(name);

        String code;
        if (dto.getCode() != null && !dto.getCode().isBlank()) {
            code = dto.getCode().trim();
            validateRoleCodeFormat(code);
            if (roleRepository.findByCode(code).isPresent()) {
                log.warn(RoleLogMessages.ROLE_CREATE_REJECTED_CODE_EXISTS, code);
                throw new ValidationException(RoleMessageConstants.ROLE_ALREADY_EXISTS);
            }
        } else {
            code = ensureUniqueCode(deriveCodeFromName(name));
        }

        Role role = new Role();
        role.setCode(code);
        applyRoleScalarsFromDto(role, dto);
        try {
            roleRepository.save(role);
        } catch (DataIntegrityViolationException ex) {
            log.warn(RoleLogMessages.ROLE_CREATE_INTEGRITY_FAILURE, code, ex);
            throw new ConflictException(RoleMessageConstants.ROLE_CODE_ALREADY_EXISTS);
        }
        log.info(RoleLogMessages.ROLE_CREATED, role.getId(), role.getCode());
        return toDto(role);
    }

    @Override
    @Cacheable(
            cacheNames = DashboardCacheNames.ROLE_OVERVIEW,
            key = "{#page ?: 0, #size ?: 'ALL'}")
    public List<RoleSummaryDTO> getRoleOverview(Integer page, Integer size) {
        Pageable pageable = size == null
                ? Pageable.unpaged()
                : PageRequest.of(page == null ? 0 : page, size);
        return roleRepository.fetchRoleSummary(pageable);
    }

    @Override
    @Cacheable(cacheNames = DashboardCacheNames.ROLES_FIND_ALL)
    public List<RoleDTO> findAll() {
        return roleRepository.findAll().stream().map(this::toDto).toList();
    }

    private RoleDTO toDto(Role role) {
        RoleDTO dto = new RoleDTO();
        dto.setId(role.getId());
        dto.setCode(role.getCode());
        dto.setDisplayName(role.getDisplayName());
        dto.setName(role.getName() != null && !role.getName().isBlank() ? role.getName() : role.getDisplayName());
        dto.setDescription(role.getDescription());
        dto.setSystemRole(role.isSystemRole());
        dto.setArabicName(role.getArabicName());
        dto.setArabicDescription(role.getArabicDescription());
        dto.setColor(role.getColor());
        return dto;
    }

    /**
     * Builds a {@link RoleDTO} with per-module permission codes (same shape as {@link #getRoleDetails(Long)}).
     */
    private RoleDTO toRoleDtoWithPermissions(Role role) {
        Long roleId = role.getId();
        List<RolePermission> rolePermissions = Optional
                .ofNullable(rolePermRepository.findByRoleIdWithDetails(roleId))
                .orElse(Collections.emptyList());

        List<Module> allModules = moduleLookupService.findAllModules();

        Long userCount = userRepository.countByRole_Id(roleId);

        Map<Long, Set<String>> permissionMap = rolePermissions.stream()
                .filter(Objects::nonNull)
                .filter(rp -> rp.getPermission() != null && rp.getPermission().getModule() != null)
                .collect(Collectors.groupingBy(
                        rp -> rp.getPermission().getModule().getId(),
                        Collectors.mapping(
                                rp -> rp.getPermission().getCode(),
                                Collectors.toCollection(HashSet::new))));
        List<RolePermissionDTO> moduleDTOs = allModules.stream()
                .filter(Objects::nonNull)
                .map(module -> RolePermissionDTO.builder()
                        .moduleId(module.getId())
                        .moduleName(module.getDisplayName())
                        .permissionCodes(new HashSet<>(permissionMap.getOrDefault(
                                module.getId(),
                                Collections.emptySet())))
                        .build())
                .toList();
        return new RoleDTO(role, userCount, moduleDTOs);
    }

    @Override
    @Transactional(readOnly = false)
    @Caching(evict = {
            @CacheEvict(cacheNames = DashboardCacheNames.ROLES_FIND_ALL, allEntries = true),
            @CacheEvict(cacheNames = DashboardCacheNames.ROLE_BY_ID, key = "#id"),
            @CacheEvict(cacheNames = DashboardCacheNames.ROLE_DETAILS, key = "#id"),
            @CacheEvict(cacheNames = DashboardCacheNames.ROLE_OVERVIEW, allEntries = true),
//            @CacheEvict(cacheNames = DashboardCacheNames.PERMISSION_MATRIX, allEntries = true),
//            @CacheEvict(cacheNames = DashboardCacheNames.ROUTE_ACCESS_COOKIE, key = "#id"),
//            @CacheEvict(cacheNames = PersistenceCacheNames.USER_AUTHORITIES, allEntries = true)
    })
    public boolean deleteById(Long id) {
        return false;
    }

    @Override
    public Optional<RoleDTO> findById(Long id) {
        if (Objects.isNull(id)) {
            return Optional.empty();
        }
        try {
            return Optional.of(selfProvider.getObject().getRoleDetails(id));
        } catch (ResourceNotFoundException ex) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = false)
    @Caching(evict = {
            @CacheEvict(cacheNames = DashboardCacheNames.ROLES_FIND_ALL, allEntries = true),
            @CacheEvict(cacheNames = DashboardCacheNames.ROLE_BY_ID, key = "#id"),
            @CacheEvict(cacheNames = DashboardCacheNames.ROLE_DETAILS, key = "#id"),
            @CacheEvict(cacheNames = DashboardCacheNames.ROLE_OVERVIEW, allEntries = true),
//            @CacheEvict(cacheNames = DashboardCacheNames.PERMISSION_MATRIX, allEntries = true),
//            @CacheEvict(cacheNames = DashboardCacheNames.ROUTE_ACCESS_COOKIE, key = "#id")
    })
    public Optional<RoleDTO> update(Long id, RoleDTO dto) {
        if (Objects.isNull(id)) {
            throw new BadRequestException(RoleMessageConstants.ROLE_ID_CANNOT_BE_NULL);
        }
        if (Objects.isNull(dto)) {
            throw new BadRequestException(RoleMessageConstants.ROLE_PAYLOAD_CANNOT_BE_NULL);
        }
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RoleMessageConstants.ROLE_NOT_FOUND));

        if (dto.getCode() != null && !dto.getCode().isBlank()) {
            String newCode = dto.getCode().trim();
            validateRoleCodeFormat(newCode);
            roleRepository.findByCode(newCode)
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new ValidationException(RoleMessageConstants.ROLE_CODE_ALREADY_IN_USE);
                    });
            role.setCode(newCode);
        }

        if (dto.getName() != null && !dto.getName().isBlank()) {
            role.setName(dto.getName().trim());
        }
        if (dto.getDescription() != null) {
            role.setDescription(dto.getDescription());
        }
        if (dto.getArabicName() != null) {
            role.setArabicName(dto.getArabicName().isBlank() ? null : dto.getArabicName().trim());
        }
        if (dto.getArabicDescription() != null) {
            role.setArabicDescription(dto.getArabicDescription().isBlank() ? null : dto.getArabicDescription().trim());
        }
        if (dto.getColor() != null) {
            role.setColor(dto.getColor().isBlank() ? null : dto.getColor().trim());
        }
        if (dto.getSystemRole() != null) {
            role.setSystemRole(dto.getSystemRole());
        }

        try {
            roleRepository.save(role);
        } catch (DataIntegrityViolationException ex) {
            log.warn(RoleLogMessages.ROLE_UPDATE_INTEGRITY_FAILURE, id, dto.getCode(), ex);
            throw new ConflictException(RoleMessageConstants.ROLE_CODE_ALREADY_EXISTS);
        }
        log.info(RoleLogMessages.ROLE_UPDATED, id);
        return Optional.of(toRoleDtoWithPermissions(role));
    }

    @Override
    @Cacheable(cacheNames = DashboardCacheNames.ROLE_DETAILS, key = "#roleId")
    public RoleDTO getRoleDetails(Long roleId) {
        if (Objects.isNull(roleId)) {
            throw new BadRequestException(RoleMessageConstants.ROLE_ID_CANNOT_BE_NULL);
        }

        try {

            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(RoleMessageConstants.ROLE_NOT_FOUND_FOR_ID_PREFIX + roleId)
                    );

            return toRoleDtoWithPermissions(role);

        } catch (AbstractApiException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error(RoleLogMessages.ROLE_DETAILS_LOAD_FAILED, roleId, ex);
            throw new InternalServerException(RoleMessageConstants.UNABLE_TO_LOAD_ROLE_DETAILS);
        }
    }

    @Override
    public RoleDTO getRoleDetailsForCurrentUser() {
        try {
            User user = EntraSecurityContext.requireCurrentUser();
            if (user.getRole() == null) {
                throw new ResourceNotFoundException(RoleMessageConstants.ROLE_NOT_FOUND_FOR_AUTHENTICATED_USER);
            }
            return selfProvider.getObject().getRoleDetails(user.getRole().getId());
        } catch (AbstractApiException | EntraRequestDeniedException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error(RoleLogMessages.ROLE_DETAILS_LOAD_FAILED_FOR_USER, ex);
            throw new InternalServerException(RoleMessageConstants.UNABLE_TO_LOAD_ROLE_DETAILS);
        }
    }

    @Override
    @Transactional(readOnly = false)
    @Caching(evict = {
            @CacheEvict(cacheNames = DashboardCacheNames.ROLE_BY_ID, key = "#roleId"),
            @CacheEvict(cacheNames = DashboardCacheNames.ROLE_DETAILS, key = "#roleId"),
//            @CacheEvict(cacheNames = DashboardCacheNames.PERMISSION_MATRIX, allEntries = true),
//            @CacheEvict(cacheNames = DashboardCacheNames.ROUTE_ACCESS_COOKIE, key = "#roleId"),
//            @CacheEvict(cacheNames = PersistenceCacheNames.USER_AUTHORITIES, allEntries = true)
    })
    public MessageResponse updatePermissions(Long roleId, RolePermissionUpdateRequestDTO request) {

        if (Objects.isNull(roleId)) {
            throw new BadRequestException(RoleMessageConstants.ROLE_ID_CANNOT_BE_NULL);
        }
        if (Objects.isNull(request) || Objects.isNull(request.getModules())) {
            throw new BadRequestException(RoleMessageConstants.REQUEST_BODY_WITH_MODULES_REQUIRED);
        }

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException(RoleMessageConstants.ROLE_NOT_FOUND));

        rolePermRepository.deleteByRole_Id(roleId);

        Set<Long> requestedModuleIds = request.getModules().stream()
                .map(ModulePermissionRequestDTO::getModuleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, Module> modulesById = moduleRepository.findAllById(requestedModuleIds).stream()
                .collect(Collectors.toMap(Module::getId, module -> module));

        Map<String, Permission> permissionByModuleAndCode = new HashMap<>();
        if (!requestedModuleIds.isEmpty()) {
            for (Permission permission : permissionRepository.findAllByModuleIds(requestedModuleIds)) {
                permissionByModuleAndCode.put(permissionKey(permission.getModule().getId(), permission.getCode()), permission);
            }
        }

        List<RolePermission> rolePermissionsToSave = new ArrayList<>();

        for (ModulePermissionRequestDTO moduleReq : request.getModules()) {

            Module module = modulesById.get(moduleReq.getModuleId());
            if (module == null) {
                throw new ResourceNotFoundException(RoleMessageConstants.MODULE_NOT_FOUND);
            }

            List<String> perms = Optional.ofNullable(moduleReq.getPermissions()).orElse(List.of());
            for (String perm : perms) {
                if (perm == null || perm.isBlank()) {
                    throw new BadRequestException(RoleMessageConstants.PERMISSION_CODE_CANNOT_BE_BLANK);
                }
                Permission permission = permissionByModuleAndCode.get(permissionKey(module.getId(), perm.trim()));
                if (permission == null) {
                    throw new BadRequestException(
                            RoleMessageConstants.INVALID_PERMISSION_FOR_MODULE_PREFIX + perm);
                }
                RolePermissionId id = new RolePermissionId();
                id.setRoleId(role.getId());
                id.setPermissionId(permission.getId());
                RolePermission rp = new RolePermission();
                rp.setId(id);
                rp.setRole(role);
                rp.setPermission(permission);
                rolePermissionsToSave.add(rp);
            }
        }

        if (!rolePermissionsToSave.isEmpty()) {
            rolePermRepository.saveAll(rolePermissionsToSave);
        }

        log.info(RoleLogMessages.PERMISSIONS_UPDATED, roleId);
        return new MessageResponse(RoleConstants.SUCCESS_RESPONSE);
    }

    @Override
    @Transactional(readOnly = false)
    @Caching(evict = {
            @CacheEvict(cacheNames = DashboardCacheNames.ROLES_FIND_ALL, allEntries = true),
            @CacheEvict(cacheNames = DashboardCacheNames.ROLE_OVERVIEW, allEntries = true),
            @CacheEvict(cacheNames = DashboardCacheNames.PERMISSION_MATRIX, allEntries = true)
    })
    public MessageResponse duplicateRole(@Valid RoleDTO dto, Long roleId) {
        String name = effectiveName(dto);
        if (name == null) {
            throw new BadRequestException(RoleMessageConstants.ROLE_NAME_REQUIRED);
        }
        validateRoleNameNotExists(name);
        dto.setName(name);
        String code = ensureUniqueCode(deriveCodeFromName(name));

        Role existingRole = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException(RoleMessageConstants.ROLE_NOT_FOUND));
        Role role = new Role();
        role.setCode(code);
        applyRoleScalarsFromDto(role, dto);
        role.setSystemRole(existingRole.isSystemRole());
        try {
            roleRepository.save(role);
        } catch (DataIntegrityViolationException ex) {
            log.warn(RoleLogMessages.ROLE_DUPLICATE_INTEGRITY_FAILURE, code, ex);
            throw new ConflictException(RoleMessageConstants.ROLE_CODE_ALREADY_EXISTS);
        }
        List<RolePermission> source = rolePermRepository.findByRoleIdWithDetails(existingRole.getId());
        List<RolePermission> copies = new ArrayList<>(source.size());
        for (RolePermission rp : source) {
            RolePermissionId id = new RolePermissionId();
            id.setRoleId(role.getId());
            id.setPermissionId(rp.getPermission().getId());
            RolePermission copy = new RolePermission();
            copy.setId(id);
            copy.setRole(role);
            copy.setPermission(rp.getPermission());
            copies.add(copy);
        }
        if (!copies.isEmpty()) {
            rolePermRepository.saveAll(copies);
        }
        log.info(RoleLogMessages.ROLE_DUPLICATED, role.getId(), role.getCode());
        return new MessageResponse(RoleConstants.SUCCESS_RESPONSE);
    }

    private static String permissionKey(Long moduleId, String code) {
        return moduleId + ":" + code.trim();
    }

    private void applyRoleScalarsFromDto(Role role, RoleDTO dto) {
        role.setName(dto.getName());
        role.setDescription(dto.getDescription());
        role.setSystemRole(Boolean.TRUE.equals(dto.getSystemRole()));
        role.setArabicName(dto.getArabicName());
        role.setArabicDescription(dto.getArabicDescription());
        role.setColor(dto.getColor());
    }

    private String effectiveName(RoleDTO dto) {
        if (dto.getName() != null && !dto.getName().isBlank()) {
            return dto.getName().trim();
        }
        if (dto.getDisplayName() != null && !dto.getDisplayName().isBlank()) {
            return dto.getDisplayName().trim();
        }
        return null;
    }

    private void validateRoleNameNotExists(String name) {
        roleRepository.findByNameIgnoreCase(name).ifPresent(existing -> {
            log.warn(RoleMessageConstants.ROLE_SAVE_REJECTED_NAME_EXISTS, name);
            throw new ValidationException(RoleMessageConstants.ROLE_NAME_ALREADY_EXISTS);
        });
    }

    private void validateRoleCodeFormat(String code) {
        if (code == null || !code.matches(RoleConstants.ROLE_CODE_PATTERN)) {
            throw new ValidationException(RoleMessageConstants.ROLE_CODE_FORMAT_INVALID);
        }
    }

    private String deriveCodeFromName(String name) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException(RoleMessageConstants.ROLE_NAME_REQUIRED_TO_DERIVE_CODE);
        }
        String upper = name.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
        upper = upper.replaceAll("^_+", "").replaceAll("_+$", "");
        if (upper.isEmpty()) {
            throw new BadRequestException(RoleMessageConstants.ROLE_NAME_MUST_CONTAIN_ALPHANUMERIC);
        }
        if (!Character.isLetter(upper.charAt(0))) {
            upper = RoleConstants.DERIVED_CODE_PREFIX + upper;
        }
        if (upper.length() > RoleConstants.MAX_ROLE_CODE_LENGTH) {
            upper = upper.substring(0, RoleConstants.MAX_ROLE_CODE_LENGTH);
        }
        return upper;
    }

    private String ensureUniqueCode(String base) {
        String candidate = base;
        int i = 2;
        while (roleRepository.findByCode(candidate).isPresent()) {
            String suffix = "_" + i++;
            int max = RoleConstants.MAX_ROLE_CODE_LENGTH - suffix.length();
            String truncated = base.length() > max ? base.substring(0, max) : base;
            candidate = truncated + suffix;
        }
        return candidate;
    }
}
