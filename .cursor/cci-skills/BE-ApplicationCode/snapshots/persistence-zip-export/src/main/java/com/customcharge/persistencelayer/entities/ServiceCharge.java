package com.customcharge.persistencelayer.entities;

import com.customcharge.persistencelayer.config.javers.AuditCommitProperties;
import com.customcharge.persistencelayer.config.javers.AuditCommitProperty;
import com.customcharge.persistencelayer.config.javers.AuditableEntity;
import com.customcharge.persistencelayer.config.javers.AuditValueNormalize;
import com.customcharge.persistencelayer.entity.AbstractEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.javers.core.metamodel.annotation.DiffIgnore;

@jakarta.persistence.Entity
@Table(
        name = "service_charge"
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AuditableEntity(
        groupName = AuditCommitProperties.GROUP_ACCOUNT_AR_AP,
        commitProperties = {
                @AuditCommitProperty(key = AuditCommitProperties.ACCOUNT_ID, path = "account.id"),
                @AuditCommitProperty(
                        key = AuditCommitProperties.ENTITY_CODE,
                        path = "account.entityCode",
                        normalize = AuditValueNormalize.UPPER,
                        required = false)
        })
public class ServiceCharge extends AbstractEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_code_id", nullable = false)
    @DiffIgnore
    private ServiceCodes serviceCodes;

    @Column(name = "description")
    private String description;

    @Column(name = "is_active")
    private Boolean active = true;

    @Column(name = "is_billable")
    private Boolean isBillable;

    @Column(name = "split")
    private String split;

    @Column(name = "default_account_number")
    private String defaultAccountNumber;

    @Column(name = "tax_value", columnDefinition = "DECIMAL(19,4)")
    private Double taxValue;
}
