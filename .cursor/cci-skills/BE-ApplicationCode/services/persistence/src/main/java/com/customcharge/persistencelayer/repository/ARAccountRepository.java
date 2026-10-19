package com.customcharge.persistencelayer.repository;

import com.customcharge.persistencelayer.entities.ARAccount;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ARAccountRepository extends JpaRepository<ARAccount, UUID>, JpaSpecificationExecutor<ARAccount> {

    @Override
    @EntityGraph(attributePaths = {"chargeConfigs"})
    List<ARAccount> findAll();

    @Query("""
            select ar from ARAccount ar
            where lower(ar.accountNumber) = lower(:accountNumber)
              and lower(ar.entityCode) = lower(:entityCode)
            """)
    Optional<ARAccount> findByAccountNumberAndEntityCode(
            @Param("accountNumber") String accountNumber, @Param("entityCode") String entityCode);

    @EntityGraph(attributePaths = {"chargeConfigs"})
    @Query("""
            select ar from ARAccount ar
            where lower(ar.accountNumber) = lower(:accountNumber)
              and lower(ar.entityCode) = lower(:entityCode)
            """)
    Optional<ARAccount> findByAccountNumberAndEntityCodeWithConfigs(
            @Param("accountNumber") String accountNumber, @Param("entityCode") String entityCode);

    @Query("""
            select ar from ARAccount ar
            where lower(ar.arAccountNumber) = lower(:arAccountNumber)
              and lower(ar.entityCode) = lower(:entityCode)
            """)
    Optional<ARAccount> findByArAccountNumberAndEntityCode(
            @Param("arAccountNumber") String arAccountNumber, @Param("entityCode") String entityCode);
}
