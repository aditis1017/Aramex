package com.customcharge.persistencelayer.repository;

import com.customcharge.persistencelayer.entities.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByCode(String code);

    Optional<Permission> findByCodeAndModule_Id(String code, Long moduleId);

    @Query("""
            SELECT p FROM Permission p
            JOIN FETCH p.module m
            WHERE m.id IN :moduleIds
            """)
    List<Permission> findAllByModuleIds(@Param("moduleIds") Collection<Long> moduleIds);
}
