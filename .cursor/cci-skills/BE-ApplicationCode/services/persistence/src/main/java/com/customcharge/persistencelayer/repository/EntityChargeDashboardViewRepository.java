package com.customcharge.persistencelayer.repository;

import com.customcharge.persistencelayer.entities.EntityChargeDashboardView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EntityChargeDashboardViewRepository
        extends JpaRepository<EntityChargeDashboardView, UUID>,
        JpaSpecificationExecutor<EntityChargeDashboardView> {
}
