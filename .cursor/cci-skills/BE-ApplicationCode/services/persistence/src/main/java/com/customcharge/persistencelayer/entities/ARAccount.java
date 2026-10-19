package com.customcharge.persistencelayer.entities;

import com.customcharge.persistencelayer.config.javers.AuditCommitProperties;
import com.customcharge.persistencelayer.config.javers.AuditCommitProperty;
import com.customcharge.persistencelayer.config.javers.AuditableEntity;
import com.customcharge.persistencelayer.config.javers.AuditValueNormalize;
import jakarta.persistence.Column;
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

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@jakarta.persistence.Entity
@Table(
        name = "ar_account",
        indexes = {
                @Index(name = "idx_ar_account_entity_code", columnList = "entity_code"),
                @Index(name = "uq_ar_account_number_entity", columnList = "account_number, entity_code", unique = true)
        })
@Getter
@Setter
@NoArgsConstructor
@AuditableEntity(
        groupName = AuditCommitProperties.GROUP_ACCOUNT_AR,
        commitProperties = {
                @AuditCommitProperty(key = AuditCommitProperties.ACCOUNT_ID, path = "id"),
                @AuditCommitProperty(
                        key = AuditCommitProperties.ENTITY_CODE,
                        path = "entityCode",
                        normalize = AuditValueNormalize.UPPER)
        })
public class ARAccount implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "entity_code", nullable = false, length = 200)
    private String entityCode;

    @Column(name = "account_name", nullable = false, length = 250)
    private String accountName;

    @Column(name = "account_number", nullable = false, length = 250)
    private String accountNumber;

    @Column(name = "country_code", length = 50)
    private String countryCode;

    @Column(name = "ar_account_type", length = 100)
    private String accountType;

    @Column(name = "ar_account_number", length = 100)
    private String arAccountNumber;

    @Column(name = "status", length = 100)
    private String status;

    @Column(name = "team_id", length = 100)
    private String teamId;

    @Column(name = "team_name", length = 255)
    private String teamName;

    @Column(name = "reach_account_id", length = 100)
    private String reachAccountId;

    @Column(name = "business_type", length = 100)
    private String businessType;

    @Column(name = "business_segment", length = 100)
    private String businessSegment;

    @Column(name = "rate_currency", length = 100)
    private String rateCurrency;

    @Column(name = "billing_currency", length = 100)
    private String billingCurrency;

    @Column(name = "ar_custom_type", length = 50)
    private String arCustomType;

    @Column(name = "created_date")
    private Instant createdDate;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @OneToMany(mappedBy = "arAccount", fetch = FetchType.LAZY)
    @DiffIgnore
    private List<Account> chargeConfigs = new ArrayList<>();

    public String getCustomType() {
        return arCustomType;
    }

    public void setCustomType(String customType) {
        this.arCustomType = customType;
    }
}
