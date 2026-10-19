package com.customcharge.persistencelayer.entities;

import com.customcharge.persistencelayer.config.javers.AuditCommitProperties;
import com.customcharge.persistencelayer.config.javers.AuditCommitProperty;
import com.customcharge.persistencelayer.config.javers.AuditableEntity;
import com.customcharge.persistencelayer.config.javers.AuditValueNormalize;
import com.customcharge.persistencelayer.entity.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "service_charge_mapping")
@Getter
@Setter
@NoArgsConstructor
@AuditableEntity(
        groupName = AuditCommitProperties.GROUP_ACCOUNT_AR_AP,
        commitProperties = {
                @AuditCommitProperty(
                        key = AuditCommitProperties.ENTITY_CODE,
                        path = "fromEntity",
                        normalize = AuditValueNormalize.UPPER)
        })
public class ServiceChargeMapping extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_service_code", nullable = false)
    private ServiceCodes fromServiceCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_service_code", nullable = false)
    private ServiceCodes toServiceCode;

    @Column(name = "from_entity", nullable = false)
    private String fromEntity;

    @Column(name = "to_entity", nullable = false)
    private String toEntity;

    @Column(name = "charge_type", nullable = false, length = 50)
    private String chargeType;
}
