package com.customcharge.persistencelayer.entities;

import com.customcharge.persistencelayer.config.javers.AuditCommitProperties;
import com.customcharge.persistencelayer.config.javers.AuditCommitProperty;
import com.customcharge.persistencelayer.config.javers.AuditableEntity;
import com.customcharge.persistencelayer.config.javers.AuditValueNormalize;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.javers.core.metamodel.annotation.DiffIgnore;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * AP master identity: one row per business account number and entity.
 * Charge-type configuration lives on linked {@link Account} rows ({@code accountNature = AP}).
 */
@jakarta.persistence.Entity
@Table(
        name = "ap_account",
        indexes = {
                @Index(name = "idx_ap_account_entity_code", columnList = "entity_code"),
                @Index(name = "uq_ap_account_number_entity", columnList = "account_number, entity_code", unique = true),
                @Index(name = "idx_ap_account_updated_time", columnList = "updated_time")
        })
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@AuditableEntity(
        groupName = AuditCommitProperties.GROUP_ACCOUNT_AR_AP,
        commitProperties = {
                @AuditCommitProperty(key = AuditCommitProperties.ACCOUNT_ID, path = "id"),
                @AuditCommitProperty(
                        key = AuditCommitProperties.ENTITY_CODE,
                        path = "entityCode",
                        normalize = AuditValueNormalize.UPPER)
        })
public class APAccount implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "entity_code", nullable = false, length = 100)
    private String entityCode;

    @Column(name = "account_name", nullable = false, length = 150)
    private String accountName;

    @Column(name = "account_number", nullable = false, length = 150)
    private String accountNumber;

    @Column(name = "market_structure", length = 100)
    private String marketStructure;

    @Column(name = "ap_account_status", length = 50)
    private String status;

    @Column(name = "account_type", length = 50)
    private String accountType;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;

    @CreatedDate
    @Column(name = "created_time", nullable = false, updatable = false)
    private Instant createdTime;

    @LastModifiedDate
    @Column(name = "updated_time", nullable = false)
    private Instant updatedTime;

    @OneToMany(mappedBy = "apAccount", fetch = FetchType.LAZY)
    @DiffIgnore
    private List<Account> chargeConfigs = new ArrayList<>();
}
