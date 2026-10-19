package com.customcharge.persistencelayer.entities;

import com.customcharge.persistencelayer.entity.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "shipment_mawb_details",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_shipment_mawb_details_hawb_mawb_source_target",
                columnNames = {"hawb_number", "mawb_number", "source_entity", "target_entity"}
        )
)
@Getter
@Setter
public class ShipmentMawbDetails extends AbstractEntity {

    @Column(name = "hawb_number", nullable = false, length = 100)
    private String hawbNumber;

    @Column(name = "mawb_number", nullable = false, length = 100)
    private String mawbNumber;

    @Column(name = "source_entity", nullable = false, length = 100)
    private String sourceEntity;

    @Column(name = "target_entity", nullable = false, length = 100)
    private String targetEntity;
}
