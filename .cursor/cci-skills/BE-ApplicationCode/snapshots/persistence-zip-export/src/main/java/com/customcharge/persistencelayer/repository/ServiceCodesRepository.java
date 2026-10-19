package com.customcharge.persistencelayer.repository;

import com.customcharge.persistencelayer.entities.ServiceCodes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceCodesRepository extends JpaRepository<ServiceCodes, UUID> {

    Optional<ServiceCodes> findByEntity_CodeAndServiceCodeAndChargeType(
            String entityCode, String serviceCode, String chargeType);

    Optional<ServiceCodes> findByEntity_CodeAndServiceCodeAndChargeTypeAndIsDeletedFalse(
            String entityCode, String serviceCode, String chargeType);

    boolean existsByEntity_CodeAndServiceCodeAndChargeTypeAndIsDeletedFalse(
            String entityCode, String serviceCode, String chargeType);

    Optional<ServiceCodes> findByEntity_CodeAndServiceCode(String entityCode, String serviceCode);

    boolean existsByEntity_CodeAndServiceCode(String entityCode, String serviceCode);

    @Query("""
            SELECT sc FROM ServiceCodes sc
            WHERE sc.entity.code = :entityCode
              AND sc.chargeType = :chargeType
              AND sc.isDeleted = false
            ORDER BY sc.serviceCode ASC
            """)
    List<ServiceCodes> findByEntityCodeAndChargeTypeAndIsDeletedFalse(
            @Param("entityCode") String entityCode,
            @Param("chargeType") String chargeType);

    @Query("""
            SELECT scs FROM ServiceCodes scs
            JOIN FETCH scs.entity e
            WHERE e.code = :entityCode
              AND scs.isDeleted = false
              AND scs.isActive=true
              AND (:chargeTypes IS NULL OR scs.chargeType IN :chargeTypes)
            ORDER BY scs.serviceCode
            """)
    List<ServiceCodes> findActiveMastersByEntityCodeAndChargeTypes(
            @Param("entityCode") String entityCode,
            @Param("chargeTypes") List<String> chargeTypes);


    @Query("""
                SELECT scs FROM ServiceCodes scs
                JOIN FETCH scs.entity e
                WHERE e.code = :entityCode
                  AND scs.isDeleted = false
                  AND (:chargeTypes IS NULL OR scs.chargeType IN :chargeTypes)
                ORDER BY scs.serviceCode
                """)
        List<ServiceCodes> findMastersByEntityCodeAndChargeTypes(
                @Param("entityCode") String entityCode,
                @Param("chargeTypes") List<String> chargeTypes);

    @Query("""
            SELECT DISTINCT sc FROM ServiceCodes sc
            JOIN FETCH sc.entity
            WHERE sc.id IN :ids
              AND sc.isDeleted = false
            """)
    List<ServiceCodes> findAllByIdInWithEntity(@Param("ids") Collection<UUID> ids);
}
