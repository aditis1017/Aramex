package com.customcharge.persistencelayer.entities;

import com.customcharge.persistencelayer.entity.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.javers.core.metamodel.annotation.DiffIgnore;

import java.math.BigDecimal;
import java.util.UUID;


@Entity
@Table(
        name = "charge_line_item",
        indexes = {
                @Index(name = "idx_charge_line_item_charge_id", columnList = "charge_id")
        })
@Getter
@Setter
public class ChargeLineItem extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "charge_id", nullable = false)
    @DiffIgnore
    private Charge charge;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    @DiffIgnore
    private Account account;

    @Column(name = "file_reference_id")
    private UUID fileReferenceId;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Column(name = "declaration_number", length = 100)
    private String declarationNumber;

    @Column(name = "consol_group", length = 100)
    private String consolGroup;

    @Column(name = "paying_entity", length = 100)
    private String payingEntity;

    @Column(name = "service_code")
    private String serviceCode;

    @Column(name = "exchange_rate")
    private Double exchangeRate;

    @Column(name = "info_axs_id")
    private String infoAxsId;

    @Column(name = "pre_taxable_amount")
    private BigDecimal preTaxableAmount;

    @Column(name = "pre_converted_amount")
    private BigDecimal preConvertedAmount;

    @Column(name = "ap_account_number", length = 100)
    private String apAccountNumber;

    @Column(name = "service_code_description")
    private String serviceCodeDescription;

    @Column(name = "contracted_rate")
    private BigDecimal contractedRate;
}
