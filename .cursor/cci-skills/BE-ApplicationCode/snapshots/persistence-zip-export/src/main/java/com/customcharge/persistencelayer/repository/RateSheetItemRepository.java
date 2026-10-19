package com.customcharge.persistencelayer.repository;

import com.customcharge.persistencelayer.entities.RateSheetItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RateSheetItemRepository extends JpaRepository<RateSheetItem, UUID> {

    @EntityGraph(attributePaths = "rates")
    List<RateSheetItem> findByRateSheetId(UUID rateSheetId);

    @EntityGraph(attributePaths = "rates")
    List<RateSheetItem> findByRateSheet_IdAndIsDeletedFalse(UUID rateSheetId);

    @EntityGraph(attributePaths = "rates")
    Optional<RateSheetItem> findWithRatesById(UUID id);
}
