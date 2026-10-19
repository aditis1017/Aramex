package com.customcharge.persistencelayer.entities;

import com.customcharge.persistencelayer.config.javers.AuditCommitProperties;
import com.customcharge.persistencelayer.config.javers.AuditCommitProperty;
import com.customcharge.persistencelayer.config.javers.AuditableEntity;
import com.customcharge.persistencelayer.entity.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.javers.core.metamodel.annotation.DiffIgnore;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(
        name = "charge",
        indexes = {
                @Index(name = "idx_charge_charge_code", columnList = "charge_code"),
                @Index(name = "idx_charge_charge_code_created_time", columnList = "charge_code, created_time"),
                @Index(name = "idx_charge_shipment_id", columnList = "shipment_id"),
                @Index(name = "idx_charge_entity_id", columnList = "entity_id"),
                @Index(name = "idx_charge_entity_created_time", columnList = "entity_id, created_time"),
                @Index(name = "idx_charge_entity_code_created_time",
                        columnList = "entity_id, charge_code, created_time")
        })
@Getter
@Setter
@AuditableEntity(
        groupName = AuditCommitProperties.GROUP_SHIPMENT_CHARGE,
        commitProperties = {
                @AuditCommitProperty(
                        key = AuditCommitProperties.HAWB_NUMBER,
                        path = "shipment.hawbNumber")
        })
public class Charge extends AbstractEntity {

    @ManyToOne
    @JoinColumn(name = "shipment_id")
    @DiffIgnore
    private Shipment shipment;

    @Column(name = "charge_code")
    private String chargeCode;

    @ManyToOne
    @JoinColumn(name = "entity_id")
    @DiffIgnore
    private com.customcharge.persistencelayer.entities.Entity entity;

    @Column(name = "status")
    private String status;

    @Column(name = "estimate_amount")
    private BigDecimal estimateAmount;

    @Column(name = "actual_amount")
    private BigDecimal actualAmount;

    @Column(name = "outstanding_amount")
    private BigDecimal outstandingAmount;

    @Column(name = "currency")
    private String currency;

    @Column(name = "cif_value")
    private Double cifValue;

    @Column(name = "cif_currency", length = 50)
    private String cifCurrency;

    @Column(name = "cif_currency_rate")
    private Double cifCurrencyRate;

    @Column(name = "shipping_charges")
    private Double shippingCharges;

    /** Billing classification for dashboard/search (ACCOUNT or CASH). */
    @Column(name = "account_type", length = 50)
    private String accountType;

    @OneToMany(mappedBy = "charge", fetch = FetchType.LAZY)
    @DiffIgnore
    private List<ChargeLineItem> lineItems;

}
