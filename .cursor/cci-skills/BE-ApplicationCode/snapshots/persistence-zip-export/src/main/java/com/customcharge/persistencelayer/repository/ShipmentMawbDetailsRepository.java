package com.customcharge.persistencelayer.repository;

import com.customcharge.persistencelayer.entities.ShipmentMawbDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ShipmentMawbDetailsRepository extends JpaRepository<ShipmentMawbDetails, UUID> {

    Optional<ShipmentMawbDetails> findByHawbNumberAndMawbNumberAndSourceEntityAndTargetEntity(
            String hawbNumber,
            String mawbNumber,
            String sourceEntity,
            String targetEntity
    );

    Optional<ShipmentMawbDetails> findFirstByHawbNumberAndSourceEntityAndTargetEntityOrderByCreatedTimeAsc(
            String hawbNumber,
            String sourceEntity,
            String targetEntity
    );
}
