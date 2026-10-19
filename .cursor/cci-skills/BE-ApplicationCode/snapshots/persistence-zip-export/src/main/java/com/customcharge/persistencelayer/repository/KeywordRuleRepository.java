package com.customcharge.persistencelayer.repository;

import com.customcharge.persistencelayer.entities.KeywordRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface KeywordRuleRepository extends JpaRepository<KeywordRule, UUID> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update KeywordRule k set k.isActive = false where k.isActive = true")
    int deactivateAllActiveRules();

    List<KeywordRule> findByIsActiveTrueOrderByKeywordAsc();

    @Query(value = "SELECT fn_is_b2b(:input)", nativeQuery = true)
    Integer evaluateB2B(@Param("input") String input);
}
