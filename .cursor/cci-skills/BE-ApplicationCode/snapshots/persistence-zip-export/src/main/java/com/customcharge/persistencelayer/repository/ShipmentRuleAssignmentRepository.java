package com.customcharge.persistencelayer.repository;

import com.customcharge.persistencelayer.entities.ShipmentRuleAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ShipmentRuleAssignmentRepository extends JpaRepository<ShipmentRuleAssignment, UUID> {

    @Modifying(clearAutomatically = true)
    @Query("delete from ShipmentRuleAssignment a where a.hawbNumber = :hawbNumber")
    void deleteByHawbNumber(@Param("hawbNumber") String hawbNumber);

}
