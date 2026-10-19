package com.customcharge.persistencelayer.repository;

import com.customcharge.persistencelayer.dto.UserSummaryDTO;
import com.customcharge.persistencelayer.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("""
            SELECT u FROM User u
            LEFT JOIN FETCH u.role
            WHERE LOWER(TRIM(u.email)) = LOWER(TRIM(:email))
            """)
    Optional<User> findByEmailIgnoreCaseWithRole(@Param("email") String email);

    @Query("""
            SELECT u FROM User u
            LEFT JOIN FETCH u.role
            WHERE LOWER(TRIM(u.username)) = LOWER(TRIM(:username))
            """)
    Optional<User> findByUsernameIgnoreCaseWithRole(@Param("username") String username);

    @Query("""
    SELECT u FROM User u
    JOIN FETCH u.role r
    WHERE r.code = :roleCode
    AND u.active = true
""")
    List<User> findByRoleCodeAndActiveTrue(@Param("roleCode") String roleCode);

    Optional<User> findByEmail(String email);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCaseAndIdNot(String username, Long id);

    long countByRole_Id(Long roleId);

    @Query("""
    SELECT new com.customcharge.persistencelayer.dto.UserSummaryDTO(
        u.id,
        u.username,
        u.email,
        r.id,
        r.code,
        r.displayName
    )
    FROM User u
    LEFT JOIN u.role r
""")
    List<UserSummaryDTO> fetchUserSummary();
}
