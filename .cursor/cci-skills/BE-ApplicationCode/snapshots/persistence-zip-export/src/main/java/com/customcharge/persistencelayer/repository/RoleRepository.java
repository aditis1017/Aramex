package com.customcharge.persistencelayer.repository;

import com.customcharge.persistencelayer.dto.RoleSummaryDTO;
import com.customcharge.persistencelayer.entities.Role;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByCode(String code);

    @Query("""
            SELECT r FROM Role r
            WHERE LOWER(TRIM(r.code)) = LOWER(TRIM(:code))
            """)
    Optional<Role> findByCodeIgnoreCase(@Param("code") String code);

    @Query("""
            SELECT r FROM Role r
            WHERE LOWER(TRIM(r.name)) = LOWER(TRIM(:name))
            """)
    Optional<Role> findByNameIgnoreCase(@Param("name") String name);

    /**
     * Join {@code User} explicitly: {@link com.customcharge.persistencelayer.entities.Role} has no inverse
     * {@code users} collection, so {@code LEFT JOIN r.users} is invalid JPQL.
     * Constructor arg order must match {@link com.customcharge.persistencelayer.dto.RoleSummaryDTO} field order
     * (Lombok {@code @AllArgsConstructor}).
     */
    @Query("""
            SELECT new com.customcharge.persistencelayer.dto.RoleSummaryDTO(
                r.id,
                r.code,
                r.arabicName,
                r.displayName,
                r.name,
                COALESCE(r.description, ''),
                r.arabicDescription,
                r.systemRole,
                r.color,
                COUNT(u.id)
            )
            FROM Role r
            LEFT JOIN r.users u
            GROUP BY r.id, r.code, r.arabicName, r.displayName, r.name, r.description,
                     r.arabicDescription, r.systemRole, r.color
            ORDER BY r.displayName
            """)
    List<RoleSummaryDTO> fetchRoleSummary(Pageable pageable);
}
