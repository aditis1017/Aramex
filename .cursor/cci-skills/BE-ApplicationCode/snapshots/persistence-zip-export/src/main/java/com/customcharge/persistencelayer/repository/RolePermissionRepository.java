package com.customcharge.persistencelayer.repository;

import com.customcharge.persistencelayer.dto.PermissionMatrixRowDTO;
import com.customcharge.persistencelayer.entities.RolePermission;
import com.customcharge.persistencelayer.entities.RolePermissionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {

    @Modifying
    @Query("DELETE FROM RolePermission rp WHERE rp.role.id = :roleId")
    void deleteByRole_Id(@Param("roleId") Long roleId);

    @Query("""
            SELECT DISTINCT rp.permission.code
            FROM RolePermission rp
            WHERE rp.role.id = :roleId
            """)
    List<String> findPermissionCodesByRoleId(@Param("roleId") Long roleId);

    @Query("""
                SELECT rp FROM RolePermission rp
                JOIN FETCH rp.permission p
                JOIN FETCH p.module
                WHERE rp.role.id = :roleId
            """)
    List<RolePermission> findByRoleIdWithDetails(@Param("roleId") Long roleId);

    @Query("""
                SELECT rp FROM RolePermission rp
                JOIN FETCH rp.permission p
                JOIN FETCH p.module
                JOIN FETCH rp.role
            """)
    List<RolePermission> findAllWithDetails();

    /**
     * Single round-trip for the permission matrix UI: every module × role pair, with granted
     * permission codes when present ({@code permissionCode} may be null).
     */
    @Query("""
            SELECT new com.customcharge.persistencelayer.dto.PermissionMatrixRowDTO(
                m.id,
                m.displayName,
                r.code,
                CASE WHEN rp.role IS NOT NULL THEN p.code ELSE NULL END
            )
            FROM Module m
            CROSS JOIN Role r
            LEFT JOIN Permission p ON p.module = m
            LEFT JOIN RolePermission rp ON rp.role = r AND rp.permission = p
            ORDER BY m.id
            """)
    List<PermissionMatrixRowDTO> findMatrixRows();
}
