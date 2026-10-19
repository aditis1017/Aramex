package com.customcharge.persistencelayer.repository;

import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.ServiceCharge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceChargeRepository extends JpaRepository<ServiceCharge, UUID> {

    @Query("""
            SELECT sc FROM ServiceCharge sc
            JOIN FETCH sc.serviceCodes scs
            JOIN FETCH scs.entity
            WHERE sc.account = :account
              AND scs.entity.code = :entityCode
              AND scs.chargeType = :chargeType
              AND scs.isDeleted = false
            """)
    List<ServiceCharge> findByAccountWithActiveServiceCodes(
            @Param("account") Account account,
            @Param("entityCode") String entityCode,
            @Param("chargeType") String chargeType);

    @Query("""
            SELECT sc FROM ServiceCharge sc
            JOIN FETCH sc.serviceCodes scs
            JOIN FETCH scs.entity
            WHERE sc.account = :account
              AND scs.entity.code = :entityCode
              AND scs.chargeType = :chargeType
              AND scs.isDeleted = false
              AND sc.active = true
            """)
    List<ServiceCharge> findActiveServiceChargesByAccountAndEntityCodeAndChargeType(
            @Param("account") Account account,
            @Param("entityCode") String entityCode,
            @Param("chargeType") String chargeType);

    @Query("""
            SELECT sc FROM ServiceCharge sc
            JOIN FETCH sc.serviceCodes scs
            JOIN FETCH scs.entity
            WHERE sc.account = :account
              AND scs.entity.code = :entityCode
              AND scs.chargeType IN :chargeTypes
              AND scs.isDeleted = false
            """)
    List<ServiceCharge> findByAccountAndEntityCodeAndChargeTypesIn(
            @Param("account") Account account,
            @Param("entityCode") String entityCode,
            @Param("chargeTypes") List<String> chargeTypes);

    @Query("""
            SELECT sc FROM ServiceCharge sc
            JOIN FETCH sc.serviceCodes scs
            JOIN FETCH scs.entity
            WHERE sc.account = :account
              AND scs.entity.code = :entityCode
              AND scs.chargeType IN :chargeTypes
              AND scs.isDeleted = false
              AND sc.active = true
            """)
    List<ServiceCharge> findActiveServiceChargesByAccountAndEntityCodeAndChargeTypes(
            @Param("account") Account account,
            @Param("entityCode") String entityCode,
            @Param("chargeTypes") List<String> chargeTypes);

    @Query("""
            SELECT sc FROM ServiceCharge sc
            JOIN FETCH sc.serviceCodes scs
            JOIN FETCH scs.entity
            WHERE sc.account.id = :accountId
              AND sc.active = true
              AND scs.isDeleted = false
            """)
    List<ServiceCharge> findByAccountIdAndActiveTrueWithActiveServiceCodes(@Param("accountId") UUID accountId);

    @Query("""
            SELECT sc FROM ServiceCharge sc
            JOIN FETCH sc.serviceCodes scs
            JOIN FETCH scs.entity
            WHERE sc.id = :id
            """)
    Optional<ServiceCharge> findByIdAndIsDeletedFalse(@Param("id") UUID id);


    @Query("""
            SELECT sc FROM ServiceCharge sc
            JOIN FETCH sc.serviceCodes scs
            JOIN FETCH scs.entity
            WHERE sc.id = :id
              AND scs.isDeleted = false
            """)
    Optional<ServiceCharge> findByIdWithActiveServiceCodes(@Param("id") UUID id);

    @Query("""
            SELECT sc FROM ServiceCharge sc
            JOIN FETCH sc.serviceCodes scs
            WHERE sc.account = :account
              AND scs.id = :serviceCodesId
              AND scs.isDeleted = false
            """)
    Optional<ServiceCharge> findByAccountAndServiceCodes_IdWithActiveServiceCodes(
            @Param("account") Account account,
            @Param("serviceCodesId") UUID serviceCodesId);

    @Query("""
            SELECT sc FROM ServiceCharge sc
            JOIN FETCH sc.serviceCodes scs
            JOIN FETCH scs.entity
            WHERE sc.account = :account
              AND scs.id IN :serviceCodeIds
              AND scs.isDeleted = false
              AND scs.isActive = true
            """)
    List<ServiceCharge> findByAccountAndServiceCodesIn(
            @Param("account") Account account,
            @Param("serviceCodeIds") List<UUID> serviceCodeIds);
}
