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

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rate_sheet_item")
@Getter
@Setter
@AuditableEntity(
        groupName = AuditCommitProperties.GROUP_ACCOUNT_AR_AP,
        commitProperties = {
                @AuditCommitProperty(key = AuditCommitProperties.ACCOUNT_ID, path = "rateSheet.account.id"),
                @AuditCommitProperty(
                        key = AuditCommitProperties.ENTITY_CODE,
                        path = "rateSheet.account.entityCode",
                        normalize = AuditValueNormalize.UPPER,
                        required = false)
        })
public class RateSheetItem extends AbstractEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "rate_sheet_id")
    @DiffIgnore
    private RateSheet rateSheet;

    @Column(name = "description")
    private String description;

    @Column(name = "service_code")
    private String serviceCode;

    @Column(name = "shipment_type")
    private String shipmentType;

    @Column(name = "business_type")
    private String businessType;

    @Column(name = "category_type")
    private String categoryType;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @OneToMany(mappedBy = "rateSheetItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @DiffIgnore
    private List<RateSheetItemRate> rates = new ArrayList<>();
}
