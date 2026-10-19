package com.customcharge.persistencelayer.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Read-only mapping of {@code v_entity_charge_dashboard}.
 * {@code declarationnumber} is not populated in the list view (see migration 023); BOE search uses line items.
 * {@code accountNumber}/{@code accountName} are resolved in SQL by charge type to match legacy search:
 * ECD/ECS/EC3 → AP, DTA → AR, DTC/DTS/DT3 → shipment parties.
 */
@Entity
@Immutable
@Table(name = "v_entity_charge_dashboard")
@Getter
@Setter
public class EntityChargeDashboardView {

    @Id
    @Column(name = "charge_id")
    private UUID chargeId;

    @Column(name = "created_time")
    private Instant createdTime;

    @Column(name = "chargetype")
    private String chargetype;

    @Column(name = "chargestatus")
    private String chargestatus;

    @Column(name = "currency")
    private String currency;

    @Column(name = "outstanding_amount")
    private BigDecimal outstandingAmount;

    @Column(name = "actual_amount")
    private BigDecimal actualAmount;

    @Column(name = "entity")
    private String entity;

    @Column(name = "trackingnumber")
    private String trackingnumber;

    @Column(name = "originentity")
    private String originentity;

    @Column(name = "destinationentity")
    private String destinationentity;

    @Column(name = "thirdpartentity")
    private String thirdpartentity;

    @Column(name = "thirdpartcountry")
    private String thirdpartcountry;

    @Column(name = "businesstype")
    private String businesstype;

    @Column(name = "category")
    private String category;

    @Column(name = "origincountry")
    private String origincountry;

    @Column(name = "destinationcountry")
    private String destinationcountry;

    @Column(name = "console")
    private String console;

    @Column(name = "pickupdate")
    private LocalDateTime pickupdate;

    @Column(name = "alerteddate")
    private LocalDateTime alerteddate;

    @Column(name = "account_type", length = 50)
    private String accountType;

    @Column(name = "consignee_number")
    private String consigneeNumber;

    @Column(name = "shipper_number")
    private String shipperNumber;

    @Column(name = "third_party_number")
    private String thirdPartyNumber;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "account_name")
    private String accountName;

    @Column(name = "declarationnumber", length = 100)
    private String declarationnumber;
}
