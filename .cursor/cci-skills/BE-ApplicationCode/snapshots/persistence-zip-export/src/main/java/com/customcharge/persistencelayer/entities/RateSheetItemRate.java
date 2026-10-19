package com.customcharge.persistencelayer.entities;

import com.customcharge.persistencelayer.config.javers.AuditCommitProperties;
import com.customcharge.persistencelayer.config.javers.AuditCommitProperty;
import com.customcharge.persistencelayer.config.javers.AuditableEntity;
import com.customcharge.persistencelayer.config.javers.AuditValueNormalize;
import com.customcharge.persistencelayer.entity.AbstractEntity;
import com.customcharge.persistencelayer.enums.PercentageOf;
import com.customcharge.persistencelayer.enums.RateType;
import jakarta.persistence.*;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import org.javers.core.metamodel.annotation.DiffIgnore;

import java.math.BigDecimal;

@Entity
@Table(name = "rate_sheet_item_rate")
@Getter
@Setter
@AuditableEntity(
        groupName = AuditCommitProperties.GROUP_ACCOUNT_AR_AP,
        commitProperties = {
                @AuditCommitProperty(
                        key = AuditCommitProperties.ACCOUNT_ID,
                        path = "rateSheetItem.rateSheet.account.id"),
                @AuditCommitProperty(
                        key = AuditCommitProperties.ENTITY_CODE,
                        path = "rateSheetItem.rateSheet.account.entityCode",
                        normalize = AuditValueNormalize.UPPER,
                        required = false)
        })
public class RateSheetItemRate extends AbstractEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "rate_sheet_item_id", nullable = false)
    @DiffIgnore
    private RateSheetItem rateSheetItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "rate_type", nullable = false)
    private RateType rateType;

    @Enumerated(EnumType.STRING)
    @Column(name = "percentage_of")
    private PercentageOf percentageOf;

    @Column(name = "value", nullable = false)
    private BigDecimal value;
}
