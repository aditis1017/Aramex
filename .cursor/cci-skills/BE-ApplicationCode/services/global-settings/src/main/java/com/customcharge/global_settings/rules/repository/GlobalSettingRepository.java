package com.customcharge.global_settings.rules.repository;


import com.customcharge.persistencelayer.entities.GlobalSetting;
import jakarta.annotation.Nonnull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GlobalSettingRepository extends JpaRepository<GlobalSetting, UUID>, JpaSpecificationExecutor<GlobalSetting> {

    List<GlobalSetting> findByDestinationAndRuleConditionAndTargetValueIn(
            String destinationCode,
            String ruleCondition,
            Collection<String> targetValues
    );

    List<GlobalSetting> findByDestinationAndRuleConditionAndTargetValueInAndIdNot(
            String destinationCode,
            String ruleCondition,
            Collection<String> targetValues,
            UUID id
    );

    @EntityGraph(attributePaths = {"rules"})
    @Override
    Optional<GlobalSetting> findById(@Nonnull UUID id);

    @EntityGraph(attributePaths = {"rules"})
    @Override
    List<GlobalSetting> findAll(Sort sort);

    @EntityGraph(attributePaths = {"rules"})
    @Override
    Page<GlobalSetting> findAll(Specification<GlobalSetting> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"rules"})
    List<GlobalSetting> findByStatusTrue();

    int countByTargetValueIn(List<String> targetValue);

    List<GlobalSetting> findAllByTargetValueIn(List<String> targetValue,Sort sort);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE global_setting gs
            SET sequence_number = mapping.seq
            FROM unnest(CAST(:ids AS uuid[]), CAST(:sequences AS int[])) AS mapping(id, seq)
            WHERE gs.id = mapping.id
              AND gs.target_value IN (:targetValues)
            """, nativeQuery = true)
    int bulkUpdateSequenceNumbers(
            @Param("ids") UUID[] ids,
            @Param("sequences") Integer[] sequences,
            @Param("targetValues") Collection<String> targetValues);
}

