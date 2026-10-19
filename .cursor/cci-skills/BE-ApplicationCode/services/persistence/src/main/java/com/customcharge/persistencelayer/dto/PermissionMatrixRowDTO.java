package com.customcharge.persistencelayer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Flat row for the RBAC permission matrix: one module × role cell, with an optional granted permission code.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PermissionMatrixRowDTO {

    private Long moduleId;
    private String moduleName;
    private String roleCode;
    /** Null when the role has no grant for this module on this row. */
    private String permissionCode;
}
