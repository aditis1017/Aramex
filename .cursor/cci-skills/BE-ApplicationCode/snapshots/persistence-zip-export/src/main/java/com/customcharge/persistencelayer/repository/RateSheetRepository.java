package com.customcharge.persistencelayer.repository;

import com.customcharge.persistencelayer.entities.RateSheet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RateSheetRepository extends JpaRepository<RateSheet, UUID> {
//    Optional<RateSheet> findByAccount_IdAndChargeCode_Code(UUID accountId, String code);
      Optional<RateSheet> findByAccount_Id(UUID accountId);
}
