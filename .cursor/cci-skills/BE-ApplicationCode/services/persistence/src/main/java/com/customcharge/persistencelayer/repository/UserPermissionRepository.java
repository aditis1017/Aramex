package com.customcharge.persistencelayer.repository;

import com.customcharge.persistencelayer.entities.UserPermission;
import com.customcharge.persistencelayer.entities.UserPermissionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserPermissionRepository extends JpaRepository<UserPermission, UserPermissionId> {

    @Query("""
            SELECT up FROM UserPermission up
            JOIN FETCH up.permission
            WHERE up.user.id = :userId
            """)
    List<UserPermission> findAllByUserIdWithPermission(@Param("userId") Long userId);
}
