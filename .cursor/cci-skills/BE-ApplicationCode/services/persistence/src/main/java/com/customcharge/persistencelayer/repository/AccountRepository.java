package com.customcharge.persistencelayer.repository;

import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.enums.AccountNature;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID>,
        JpaSpecificationExecutor<Account>, AccountChargeConfigPersistence {

    @Query("""
            select a from Account a join a.entity e join a.apAccount ap
            where lower(ap.accountNumber) = lower(:accountNumber)
              and lower(e.code) = lower(:entityCode)
              and a.accountNature = :accountNature
              and (
                (:chargeType is null and a.chargeType is null)
                or (a.chargeType = :chargeType)
              )
            """)
    Optional<Account> findByAccountNumberAndEntityCodeAndAccountNature(
            @Param("accountNumber") String accountNumber,
            @Param("entityCode") String entityCode,
            @Param("accountNature") AccountNature accountNature,
            @Param("chargeType") String chargeType);

    @Query("""
            select a from Account a join a.entity e join a.apAccount ap
            where lower(ap.accountNumber) = lower(:accountNumber)
              and lower(e.code) = lower(:entityCode)
              and a.accountNature = com.customcharge.persistencelayer.enums.AccountNature.AP
            """)
    Optional<Account> findByAccountNumberAndEntityCodeAndAccountNature(
            @Param("accountNumber") String accountNumber,
            @Param("entityCode") String entityCode,
            @Param("accountNature") AccountNature accountNature);

    @Query("""
            select a from Account a
            join a.arAccount ar
            join a.entity e
            where lower(ar.accountNumber) = lower(:accountNumber)
              and lower(e.code) = lower(:entityCode)
              and a.accountNature = com.customcharge.persistencelayer.enums.AccountNature.AR
              and (
                (:chargeType is null and a.chargeType is null)
                or (a.chargeType = :chargeType)
              )
            """)
    Optional<Account> findByAccountNumberAndEntityCodeAndAccountNatureAndChargeType(
            @Param("accountNumber") String accountNumber,
            @Param("entityCode") String entityCode,
            @Param("accountNature") AccountNature accountNature,
            @Param("chargeType") String chargeType);

    @Query("""
            select a from Account a
            where a.arAccount.id = :arAccountId
              and (
                (:chargeType is null and a.chargeType is null)
                or (a.chargeType = :chargeType)
              )
            """)
    Optional<Account> findByArAccountIdAndChargeType(
            @Param("arAccountId") UUID arAccountId, @Param("chargeType") String chargeType);

    @Query("""
            select a from Account a
            where a.arAccount.id = :arAccountId
            order by a.chargeType asc nulls last
            """)
    List<Account> findAllByArAccountId(@Param("arAccountId") UUID arAccountId);

    @Query("""
            select a from Account a
            where a.apAccount.id = :apAccountId
              and (
                (:chargeType is null and a.chargeType is null)
                or (a.chargeType = :chargeType)
              )
            """)
    Optional<Account> findByApAccountIdAndChargeType(
            @Param("apAccountId") UUID apAccountId, @Param("chargeType") String chargeType);

    @Query("""
            select a from Account a
            where a.apAccount.id = :apAccountId
            order by a.chargeType asc nulls last
            """)
    List<Account> findAllByApAccountId(@Param("apAccountId") UUID apAccountId);

    @Query("""
            select a from Account a join a.apAccount ap
            where ap.accountNumber = :accountNumber
              and a.accountNature = :accountNature
            """)
    List<Account> findByAccountNumberAndAccountNature(
            @Param("accountNumber") String accountNumber,
            @Param("accountNature") AccountNature accountNature);

    @Query("""
            select a from Account a
            join a.arAccount ar
            where lower(ar.accountNumber) = lower(:accountNumber)
              and a.accountNature = com.customcharge.persistencelayer.enums.AccountNature.AR
              and (
                (:chargeType is null and a.chargeType is null)
                or (a.chargeType = :chargeType)
              )
            """)
    List<Account> findByAccountNumberAndAccountNatureAndChargeType(
            @Param("accountNumber") String accountNumber,
            @Param("accountNature") AccountNature accountNature,
            @Param("chargeType") String chargeType);

    @Query("""
            select a from Account a join a.entity e
            where a.accountNature = com.customcharge.persistencelayer.enums.AccountNature.ENTITY_SETTINGS
              and lower(e.code) = lower(:entityCode)
              and a.accountNature = :accountNature
            """)
    Optional<Account> findByAccountNumberIsNullAndEntityCodeAndAccountNature(
            @Param("entityCode") String entityCode,
            @Param("accountNature") AccountNature accountNature);

    @Query("""
            select a from Account a join a.entity e
            where a.accountNature = com.customcharge.persistencelayer.enums.AccountNature.ENTITY_SETTINGS
              and lower(e.code) = lower(:entityCode)
              and a.accountNature = :accountNature
              and (
                (:chargeType is null and a.chargeType is null)
                or (a.chargeType = :chargeType)
              )
            """)
    Optional<Account> findByAccountNumberIsNullAndEntityCodeAndAccountNatureAndChargeType(
            @Param("entityCode") String entityCode,
            @Param("accountNature") AccountNature accountNature,
            @Param("chargeType") String chargeType);

    @Query("""
            select a from Account a join a.entity e
            where a.accountNature = com.customcharge.persistencelayer.enums.AccountNature.ENTITY_SETTINGS
              and lower(e.code) = lower(:entityCode)
              and a.accountNature = :accountNature
            order by a.chargeType asc nulls last
            """)
    List<Account> findByAccountNumberIsNullAndEntityCodeAndAccountNatureOrderByChargeTypeAsc(
            @Param("entityCode") String entityCode,
            @Param("accountNature") AccountNature accountNature);

    @Query("""
            select a from Account a join a.entity e join a.apAccount ap
            where lower(e.code) = lower(:entityCode)
              and a.accountNature = com.customcharge.persistencelayer.enums.AccountNature.AP
            order by ap.accountName asc
            """)
    List<Account> findApAccountsByEntityCode(@Param("entityCode") String entityCode);

    @Query("""
            select a from Account a join a.entity e
            where a.accountNature = com.customcharge.persistencelayer.enums.AccountNature.ENTITY_SETTINGS
              and lower(e.code) = lower(:entityCode)
              and a.accountNature = :accountNature
              and (
                (:chargeType is null and a.chargeType is null)
                or (a.chargeType = :chargeType)
              )
            order by a.chargeType asc nulls last
            """)
    List<Account> findByAccountNumberIsNullAndEntityCodeAndAccountNatureAndChargeTypeOrderByChargeTypeAsc(
            @Param("entityCode") String entityCode,
            @Param("accountNature") AccountNature accountNature,
            @Param("chargeType") String chargeType);

    @Query("""
            SELECT a FROM Account a
            JOIN FETCH a.entity e
            WHERE a.accountNature = com.customcharge.persistencelayer.enums.AccountNature.ENTITY_SETTINGS
              AND LOWER(e.code) IN :entityCodes
              AND a.chargeType IN :chargeTypes
            """)
    List<Account> findEntitySettingsAccountsByEntityCodesAndChargeTypes(
            @Param("entityCodes") Collection<String> entityCodes,
            @Param("chargeTypes") Collection<String> chargeTypes);

    @Query("""
            select a from Account a
            join a.arAccount ar
            where lower(ar.arAccountNumber) = lower(:arAccountNumber)
              and a.accountNature = com.customcharge.persistencelayer.enums.AccountNature.AR
            """)
    Optional<Account> findArByArAccountNumber(@Param("arAccountNumber") String arAccountNumber);

    @Query("""
            select a from Account a
            join a.arAccount ar
            where lower(ar.accountNumber) = lower(:accountNumber)
              and a.accountNature = com.customcharge.persistencelayer.enums.AccountNature.AR
            """)
    List<Account> findArConfigsByMasterAccountNumber(@Param("accountNumber") String accountNumber);

    @Query("""
            select a from Account a join a.entity e
            where lower(e.code) = lower(:entityCode)
              and a.accountNature = :accountNature
            order by a.chargeType asc nulls last
            """)
    @EntityGraph(attributePaths = {"entity", "apAccount", "arAccount"})
    List<Account> findByEntityCodeAndAccountNature(
            @Param("entityCode") String entityCode, @Param("accountNature") AccountNature accountNature);

    @EntityGraph(attributePaths = {"entity", "apAccount", "arAccount"})
    List<Account> findByAccountNature(AccountNature accountNature);

    @Override
    @EntityGraph(attributePaths = {"entity", "apAccount", "arAccount"})
    @NonNull
    Optional<Account> findById(@NonNull UUID accountId);
}
