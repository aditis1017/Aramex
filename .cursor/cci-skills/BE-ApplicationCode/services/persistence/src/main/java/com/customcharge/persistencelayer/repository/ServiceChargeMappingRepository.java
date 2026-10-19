package com.customcharge.persistencelayer.repository;

import com.customcharge.persistencelayer.dto.ServiceChargeDescriptionMappingDto;
import com.customcharge.persistencelayer.entities.ServiceChargeMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ServiceChargeMappingRepository extends JpaRepository<ServiceChargeMapping, UUID> {

    @Query("""
            SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END
            FROM ServiceChargeMapping m
            WHERE m.fromEntity = :fromEntity
              AND m.toEntity = :toEntity
              AND m.chargeType = :chargeType
            """)
    boolean existsByFromEntityAndToEntityAndChargeType(
            @Param("fromEntity") String fromEntity,
            @Param("toEntity") String toEntity,
            @Param("chargeType") String chargeType);

    @Query("""
            SELECT m FROM ServiceChargeMapping m
            JOIN FETCH m.fromServiceCode fromSc
            JOIN FETCH m.toServiceCode toSc
            WHERE m.fromEntity = :fromEntity
              AND m.toEntity = :toEntity
              AND m.chargeType = :chargeType
            ORDER BY fromSc.serviceCode, toSc.serviceCode
            """)
    List<ServiceChargeMapping> findByFromEntityAndToEntityAndChargeType(
            @Param("fromEntity") String fromEntity,
            @Param("toEntity") String toEntity,
            @Param("chargeType") String chargeType);

    @Query("""
            SELECT m FROM ServiceChargeMapping m
            JOIN FETCH m.fromServiceCode fromSc
            JOIN FETCH m.toServiceCode toSc
            WHERE m.fromEntity = :fromEntity
              AND m.chargeType = :chargeType
            ORDER BY m.toEntity, fromSc.serviceCode, toSc.serviceCode
            """)
    List<ServiceChargeMapping> findByFromEntityAndChargeType(
            @Param("fromEntity") String fromEntity,
            @Param("chargeType") String chargeType);

    @Query("""
            SELECT m FROM ServiceChargeMapping m
            JOIN FETCH m.fromServiceCode fromSc
            JOIN FETCH m.toServiceCode toSc
            WHERE m.toEntity = :toEntity
              AND m.chargeType = :chargeType
            ORDER BY m.fromEntity, fromSc.serviceCode, toSc.serviceCode
            """)
    List<ServiceChargeMapping> findByToEntityAndChargeType(
            @Param("toEntity") String toEntity,
            @Param("chargeType") String chargeType);

    @Query("""
            SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END
            FROM ServiceChargeMapping m
            WHERE m.fromServiceCode.id = :fromServiceCodeId
              AND m.toServiceCode.id = :toServiceCodeId
              AND m.fromEntity = :fromEntity
              AND m.toEntity = :toEntity
              AND m.chargeType = :chargeType
            """)
    boolean existsByFromServiceCodeAndToServiceCodeAndFromEntityAndToEntityAndChargeType(
            @Param("fromServiceCodeId") UUID fromServiceCodeId,
            @Param("toServiceCodeId") UUID toServiceCodeId,
            @Param("fromEntity") String fromEntity,
            @Param("toEntity") String toEntity,
            @Param("chargeType") String chargeType);

    @Query("""
            SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END
            FROM ServiceChargeMapping m
            WHERE m.fromServiceCode.id = :fromServiceCodeId
              AND m.toServiceCode.id = :toServiceCodeId
              AND m.fromEntity = :fromEntity
              AND m.toEntity = :toEntity
              AND m.chargeType = :chargeType
              AND m.id <> :excludeMappingId
            """)
    boolean existsByPairAndEntitiesAndChargeTypeExcludingMappingId(
            @Param("fromServiceCodeId") UUID fromServiceCodeId,
            @Param("toServiceCodeId") UUID toServiceCodeId,
            @Param("fromEntity") String fromEntity,
            @Param("toEntity") String toEntity,
            @Param("chargeType") String chargeType,
            @Param("excludeMappingId") UUID excludeMappingId);

    @Query("""
            SELECT new com.customcharge.persistencelayer.dto.ServiceChargeDescriptionMappingDto(
                fs.serviceDescription,
                fs.serviceCode,
                ts.serviceDescription,
                ts.serviceCode)
            FROM ServiceChargeMapping m
            JOIN m.fromServiceCode fs
            JOIN m.toServiceCode ts
            WHERE UPPER(TRIM(m.fromEntity)) = UPPER(TRIM(:fromEntity))
              AND UPPER(TRIM(m.toEntity)) = UPPER(TRIM(:toEntity))
              AND UPPER(TRIM(m.chargeType)) = UPPER(TRIM(:chargeType))
            """)
    List<ServiceChargeDescriptionMappingDto> findDescriptionMappingsByFromEntityAndToEntityAndChargeType(
            @Param("fromEntity") String fromEntity,
            @Param("toEntity") String toEntity,
            @Param("chargeType") String chargeType);

}
