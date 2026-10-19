package com.customcharge.dashboard.service.impl;

import com.customcharge.dashboard.cache.DashboardCacheNames;
import com.customcharge.dashboard.constants.PermissionLogMessages;
import com.customcharge.dashboard.constants.PermissionMessageConstants;
import com.customcharge.dashboard.dto.PermissionMatrixDTO;
import com.customcharge.dashboard.service.PermissionService;
import com.customcharge.persistencelayer.dto.PermissionMatrixRowDTO;
import com.customcharge.persistencelayer.exception.AbstractApiException;
import com.customcharge.persistencelayer.exception.InternalServerException;
import com.customcharge.persistencelayer.repository.RolePermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PermissionServiceImpl implements PermissionService {

    private static final List<String> ROLE_ORDER = List.of(
            "SUPER_ADMIN",
            "CUSTOMS_MANAGER",
            "FINANCE_OFFICER",
            "OPERATIONS_STAFF",
            "VIEWER"
    );

    private final RolePermissionRepository rolePermRepository;

    @Override
    @Cacheable(
            cacheNames = DashboardCacheNames.PERMISSION_MATRIX,
            key = "{#page ?: 0, #size ?: 'ALL'}")
    public List<PermissionMatrixDTO> getMatrix(Integer page, Integer size) {
        List<PermissionMatrixDTO> matrix = buildMatrix();
        if (size == null) {
            return matrix;
        }
        int pageNumber = page == null ? 0 : page;
        int fromIndex = pageNumber * size;
        if (fromIndex >= matrix.size()) {
            return List.of();
        }
        return matrix.subList(fromIndex, Math.min(fromIndex + size, matrix.size()));
    }

    private List<PermissionMatrixDTO> buildMatrix() {
        try {
            List<PermissionMatrixRowDTO> rows = Objects.requireNonNullElse(
                    rolePermRepository.findMatrixRows(),
                    Collections.emptyList()
            );

            Map<Long, String> moduleNames = new LinkedHashMap<>();
            Set<String> roleCodes = new LinkedHashSet<>();
            Map<Long, Map<String, Set<String>>> matrix = new LinkedHashMap<>();

            for (PermissionMatrixRowDTO row : rows) {
                if (row == null || row.getModuleId() == null) {
                    continue;
                }

                moduleNames.putIfAbsent(row.getModuleId(), row.getModuleName());
                matrix.computeIfAbsent(row.getModuleId(), id -> new LinkedHashMap<>());

                String roleCode = row.getRoleCode();
                if (roleCode == null) {
                    log.warn(PermissionLogMessages.SKIPPING_NULL_ROLE_CODE, row.getModuleId());
                    continue;
                }

                roleCodes.add(roleCode);
            }

            List<String> orderedRoles = roleCodes.stream()
                    .sorted((left, right) -> Integer.compare(roleOrder(left), roleOrder(right)))
                    .toList();

            for (Long moduleId : moduleNames.keySet()) {
                Map<String, Set<String>> roleMap = matrix.get(moduleId);
                for (String roleCode : orderedRoles) {
                    roleMap.putIfAbsent(roleCode, new HashSet<>());
                }
            }

            for (PermissionMatrixRowDTO row : rows) {
                if (row == null
                        || row.getModuleId() == null
                        || row.getRoleCode() == null
                        || row.getPermissionCode() == null) {
                    continue;
                }

                Map<String, Set<String>> moduleRow = matrix.get(row.getModuleId());
                if (moduleRow == null) {
                    log.warn(PermissionLogMessages.SKIPPING_UNKNOWN_MODULE, row.getModuleId());
                    continue;
                }
                Set<String> cell = moduleRow.get(row.getRoleCode());
                if (cell == null) {
                    log.warn(PermissionLogMessages.SKIPPING_UNKNOWN_ROLE_CODE, row.getRoleCode(), row.getModuleId());
                    continue;
                }
                cell.add(row.getPermissionCode());
            }

            return moduleNames.entrySet().stream()
                    .map(entry -> new PermissionMatrixDTO(
                            entry.getKey(),
                            entry.getValue(),
                            matrix.getOrDefault(entry.getKey(), Collections.emptyMap())
                    ))
                    .toList();

        } catch (AbstractApiException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error(PermissionLogMessages.MATRIX_BUILD_FAILED, ex);
            throw new InternalServerException(PermissionMessageConstants.UNABLE_TO_LOAD_MATRIX);
        }
    }

    private static int roleOrder(String roleCode) {
        int index = ROLE_ORDER.indexOf(roleCode);
        return index >= 0 ? index : Integer.MAX_VALUE;
    }
}
