package com.customcharge.persistencelayer.repository;

import com.customcharge.persistencelayer.entities.APAccount;
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
public interface APAccountRepository extends JpaRepository<APAccount, UUID>, JpaSpecificationExecutor<APAccount> {

    @Override
    @EntityGraph(attributePaths = {"chargeConfigs"})
    List<APAccount> findAll();

    @Query("""
            select ap from APAccount ap
            where lower(ap.accountNumber) = lower(:accountNumber)
              and lower(ap.entityCode) = lower(:entityCode)
            """)
    Optional<APAccount> findByAccountNumberAndEntityCode(
            @Param("accountNumber") String accountNumber, @Param("entityCode") String entityCode);

    @EntityGraph(attributePaths = {"chargeConfigs"})
    @Query("""
            select ap from APAccount ap
            where lower(ap.accountNumber) = lower(:accountNumber)
              and lower(ap.entityCode) = lower(:entityCode)
            """)
    Optional<APAccount> findByAccountNumberAndEntityCodeWithConfigs(
            @Param("accountNumber") String accountNumber, @Param("entityCode") String entityCode);

    @Query("""
            select ap from APAccount ap
            where lower(ap.entityCode) = lower(:entityCode)
            order by ap.accountNumber asc
            """)
    List<APAccount> findByEntityCode(@Param("entityCode") String entityCode);
}
