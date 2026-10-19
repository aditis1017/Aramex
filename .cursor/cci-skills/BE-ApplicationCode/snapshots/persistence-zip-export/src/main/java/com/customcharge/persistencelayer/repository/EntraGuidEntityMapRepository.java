package com.customcharge.persistencelayer.repository;

import com.customcharge.persistencelayer.entities.EntraGuidEntityMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EntraGuidEntityMapRepository extends JpaRepository<EntraGuidEntityMap, Long> {
}
