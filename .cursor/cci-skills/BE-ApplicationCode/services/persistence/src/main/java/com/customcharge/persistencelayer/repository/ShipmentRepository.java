package com.customcharge.persistencelayer.repository;

import com.customcharge.persistencelayer.entities.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {
    boolean existsByHawbNumber(String hawbNumber);
    Optional<Shipment> findByHawbNumber(String hawbNumber);

    List<Shipment> findByHawbNumberIn(Collection<String> hawbNumbers);

    List<Shipment> findByPaymentTypeAndShipmentType(String paymentType, String shipmentType);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Shipment s
            SET s.createdTime = :createdTime, s.updatedTime = :updatedTime
            WHERE s.hawbNumber = :hawbNumber
            """)
    int updateAuditTimesByHawbNumber(
            @Param("hawbNumber") String hawbNumber,
            @Param("createdTime") Instant createdTime,
            @Param("updatedTime") Instant updatedTime);
}