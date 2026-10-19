package com.customcharge.dashboard.service.impl;

import com.customcharge.dashboard.dto.PermissionMatrixDTO;
import com.customcharge.persistencelayer.dto.PermissionMatrixRowDTO;
import com.customcharge.persistencelayer.repository.RolePermissionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionServiceImplTest {

    @Mock
    private RolePermissionRepository rolePermRepository;

    @InjectMocks
    private PermissionServiceImpl permissionService;

    @Test
    void getMatrixReturnsPreferredRolesFirstAndAppendsAdditionalRoles() {
        when(rolePermRepository.findMatrixRows()).thenReturn(List.of(
                row(1L, "Customs", "VIEWER", null),
                row(1L, "Customs", "ADDITIONAL_ROLE", null),
                row(1L, "Customs", "SUPER_ADMIN", null),
                row(1L, "Customs", "CUSTOMS_MANAGER", null),
                row(1L, "Customs", "ANOTHER_ROLE", null),
                row(1L, "Customs", "FINANCE_OFFICER", null),
                row(1L, "Customs", "OPERATIONS_STAFF", null)
        ));

        List<PermissionMatrixDTO> result = permissionService.getMatrix(null, null);

        assertEquals(List.of(
                        "SUPER_ADMIN",
                        "CUSTOMS_MANAGER",
                        "FINANCE_OFFICER",
                        "OPERATIONS_STAFF",
                        "VIEWER",
                        "ADDITIONAL_ROLE",
                        "ANOTHER_ROLE"
                ),
                new ArrayList<>(result.getFirst().getRolePermissions().keySet()));
    }

    @Test
    void getMatrixGroupsGrantedPermissionsByModuleAndRole() {
        when(rolePermRepository.findMatrixRows()).thenReturn(List.of(
                row(1L, "Customs", "SUPER_ADMIN", "CUSTOMS_VIEW"),
                row(1L, "Customs", "SUPER_ADMIN", "CUSTOMS_EDIT"),
                row(1L, "Customs", "VIEWER", null),
                row(2L, "Finance", "SUPER_ADMIN", "FINANCE_VIEW"),
                row(2L, "Finance", "VIEWER", null)
        ));

        List<PermissionMatrixDTO> result = permissionService.getMatrix(null, null);

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getModuleId());
        assertEquals("Customs", result.get(0).getModuleName());
        assertEquals(Set.of("CUSTOMS_VIEW", "CUSTOMS_EDIT"), result.get(0).getRolePermissions().get("SUPER_ADMIN"));
        assertEquals(Set.of(), result.get(0).getRolePermissions().get("VIEWER"));
        assertEquals(2L, result.get(1).getModuleId());
        assertEquals(Set.of("FINANCE_VIEW"), result.get(1).getRolePermissions().get("SUPER_ADMIN"));
    }

    @Test
    void getMatrixPaginatesByModuleWhenSizeProvided() {
        when(rolePermRepository.findMatrixRows()).thenReturn(List.of(
                row(1L, "Customs", "SUPER_ADMIN", "CUSTOMS_VIEW"),
                row(2L, "Finance", "SUPER_ADMIN", "FINANCE_VIEW"),
                row(3L, "Operations", "SUPER_ADMIN", "OPS_VIEW")
        ));

        List<PermissionMatrixDTO> page0 = permissionService.getMatrix(0, 2);
        List<PermissionMatrixDTO> page1 = permissionService.getMatrix(1, 2);
        List<PermissionMatrixDTO> emptyPage = permissionService.getMatrix(5, 2);

        assertEquals(2, page0.size());
        assertEquals(1L, page0.get(0).getModuleId());
        assertEquals(2L, page0.get(1).getModuleId());
        assertEquals(1, page1.size());
        assertEquals(3L, page1.get(0).getModuleId());
        assertEquals(0, emptyPage.size());
    }

    private static PermissionMatrixRowDTO row(Long moduleId, String moduleName, String roleCode, String permissionCode) {
        return new PermissionMatrixRowDTO(moduleId, moduleName, roleCode, permissionCode);
    }
}
