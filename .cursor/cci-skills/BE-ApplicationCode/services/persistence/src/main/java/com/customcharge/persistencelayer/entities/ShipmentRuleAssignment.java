package com.customcharge.persistencelayer.entities;

import com.customcharge.persistencelayer.dto.RuleSnapshotEntry;
import com.customcharge.persistencelayer.entity.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.UUID;

/**
 * Records which rule assigned shipment type or category type for a HAWB, including evaluation context.
 */
@Entity
@Table(
        name = "shipment_rule_assignment",
        indexes = @Index(name = "idx_shipment_rule_assignment_hawb", columnList = "hawb_number")
)
@Getter
@Setter
@NoArgsConstructor
public class ShipmentRuleAssignment extends AbstractEntity {

    @Column(name = "hawb_number", nullable = false, length = 50)
    private String hawbNumber;

    /**
     * Assigned value from the rule (shipment type code or category type code).
     */
    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Column(name = "rule_id", nullable = false)
    private UUID ruleId;

    /**
     * All candidate rules collected and sorted for the evaluation domain.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "domain_snapshot", nullable = false, columnDefinition = "jsonb")
    private List<RuleSnapshotEntry> domainSnapshot;

    /**
     * The rule that was finally triggered and produced the assigned {@link #type}.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rule_snapshot", nullable = false, columnDefinition = "jsonb")
    private RuleSnapshotEntry ruleSnapshot;
}
