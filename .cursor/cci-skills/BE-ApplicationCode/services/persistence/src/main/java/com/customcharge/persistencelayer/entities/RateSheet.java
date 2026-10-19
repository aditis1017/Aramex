package com.customcharge.persistencelayer.entities;

import com.customcharge.persistencelayer.config.javers.AuditCommitProperties;
import com.customcharge.persistencelayer.config.javers.AuditCommitProperty;
import com.customcharge.persistencelayer.config.javers.AuditableEntity;
import com.customcharge.persistencelayer.config.javers.AuditValueNormalize;
import com.customcharge.persistencelayer.entity.AbstractEntity;
import jakarta.persistence.*;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import org.javers.core.metamodel.annotation.DiffIgnore;

import java.util.List;

@Entity
@Table(name = "rate_sheet")
@Getter
@Setter
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
public class RateSheet extends AbstractEntity {

    @Column(name = "is_active")
    private Boolean active = true;

    @ManyToOne(optional = false)
    @JoinColumn(name = "account_id")
    @DiffIgnore
    private Account account;

    @OneToMany(mappedBy = "rateSheet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RateSheetItem> items;
}
