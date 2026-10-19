package com.customcharge.persistencelayer.repository;

import com.customcharge.persistencelayer.entities.Charge;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChargeRepository extends JpaRepository<Charge, UUID>, JpaSpecificationExecutor<Charge> {

    Optional<Charge> findByShipment_IdAndChargeCode(UUID shipmentId, String chargeCode);

    @Query("""
            SELECT DISTINCT c FROM Charge c
            LEFT JOIN FETCH c.entity e
            LEFT JOIN FETCH e.country
            WHERE c.shipment.id = :shipmentId
            ORDER BY c.chargeCode ASC
            """)
    List<Charge> findByShipment_IdWithEntity(@Param("shipmentId") UUID shipmentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Charge c WHERE c.id = :id")
    Optional<Charge> findByIdForUpdate(@Param("id") UUID id);

    boolean existsByShipment_IdAndStatus(UUID shipmentId, String status);

    boolean existsByShipment_Id(UUID shipmentId);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE Charge c
            SET c.status = :status
            WHERE c.shipment.id = :shipmentId
            """)
    int updateStatusByShipmentId(@Param("shipmentId") UUID shipmentId, @Param("status") String status);

    List<Charge> findByShipment_Id(UUID shipmentId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Charge c WHERE c.shipment.id = :shipmentId")
    void deleteByShipment_Id(@Param("shipmentId") UUID shipmentId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Charge c
            SET c.createdTime = :createdTime, c.updatedTime = :updatedTime
            WHERE c.shipment.hawbNumber = :hawbNumber
            """)
    int updateAuditTimesByShipmentHawbNumber(
            @Param("hawbNumber") String hawbNumber,
            @Param("createdTime") Instant createdTime,
            @Param("updatedTime") Instant updatedTime);
}
