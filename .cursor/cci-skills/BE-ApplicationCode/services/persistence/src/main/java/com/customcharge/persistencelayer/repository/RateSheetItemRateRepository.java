package com.customcharge.persistencelayer.repository;

import com.customcharge.persistencelayer.entities.RateSheetItemRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RateSheetItemRateRepository extends JpaRepository<RateSheetItemRate, UUID> {
}
