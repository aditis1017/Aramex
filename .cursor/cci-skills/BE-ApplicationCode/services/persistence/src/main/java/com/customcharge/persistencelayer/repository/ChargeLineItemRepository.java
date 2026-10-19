package com.customcharge.persistencelayer.repository;

import com.customcharge.persistencelayer.entities.ChargeLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface ChargeLineItemRepository extends JpaRepository<ChargeLineItem, UUID> {

    void deleteAllByCharge_Id(UUID chargeId);

    @Query("SELECT DISTINCT cli FROM ChargeLineItem cli LEFT JOIN FETCH cli.account WHERE cli.charge.id = :chargeId")
    List<ChargeLineItem> findByCharge_Id(@Param("chargeId") UUID chargeId);

    @Query("""
            SELECT DISTINCT cli FROM ChargeLineItem cli
            LEFT JOIN FETCH cli.account
            JOIN FETCH cli.charge
            WHERE cli.charge.id IN :chargeIds
            ORDER BY cli.serviceCode ASC
            """)
    List<ChargeLineItem> findByCharge_IdIn(@Param("chargeIds") Collection<UUID> chargeIds);

    boolean existsByCharge_IdAndPaymentReferenceIgnoreCase(UUID chargeId, String paymentReference);

    @Query("""
            SELECT cli FROM ChargeLineItem cli
            WHERE cli.charge.id = :chargeId
              AND LOWER(TRIM(COALESCE(cli.paymentReference, ''))) = LOWER(TRIM(:paymentReference))
            """)
    List<ChargeLineItem> findByChargeIdAndPaymentReferenceNormalized(
            @Param("chargeId") UUID chargeId,
            @Param("paymentReference") String paymentReference);

    @Query("""
            SELECT CASE WHEN COUNT(cli) > 0 THEN true ELSE false END
            FROM ChargeLineItem cli
            WHERE LOWER(TRIM(COALESCE(cli.declarationNumber, ''))) = LOWER(TRIM(:declarationNumber))
            """)
    boolean existsByDeclarationNumberNormalized(@Param("declarationNumber") String declarationNumber);

    @Query("""
            SELECT COALESCE(SUM(cli.amount), 0) FROM ChargeLineItem cli
            WHERE cli.charge.id = :chargeId
              AND cli.paymentReference IS NOT NULL
              AND LENGTH(TRIM(cli.paymentReference)) > 0
            """)
    java.math.BigDecimal sumAmountByChargeId(@Param("chargeId") UUID chargeId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ChargeLineItem cli WHERE cli.charge.id IN :chargeIds")
    void deleteByCharge_IdIn(@Param("chargeIds") Collection<UUID> chargeIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ChargeLineItem cli
            SET cli.createdTime = :createdTime, cli.updatedTime = :updatedTime
            WHERE cli.charge.shipment.hawbNumber = :hawbNumber
            """)
    int updateAuditTimesByShipmentHawbNumber(
            @Param("hawbNumber") String hawbNumber,
            @Param("createdTime") Instant createdTime,
            @Param("updatedTime") Instant updatedTime);
}
