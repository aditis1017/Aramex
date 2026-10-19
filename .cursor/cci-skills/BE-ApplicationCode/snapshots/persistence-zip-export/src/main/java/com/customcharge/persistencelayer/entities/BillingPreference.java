package com.customcharge.persistencelayer.entities;

import com.customcharge.persistencelayer.entity.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "billing_preferences")
@Getter
@Setter
public class BillingPreference extends AbstractEntity {

    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;

    @Column(name = "charge_code")
    private String chargeCode;

    @Column(name = "eligible_for_billing")
    private String eligibleForBilling;

    @Column(name = "invoice_trigger")
    private String invoiceTrigger;

    @Column(name = "split_invoice")
    private String splitInvoice;

    @Column(name = "invoice_limit")
    private Integer invoiceLimit;

    @Column(name = "invoicing_frequency")
    private String invoicingFrequency;

    @Column(name = "invoice_group_by")
    private String invoiceGroupBy;

    @Column(name = "potential_delay_days")
    private Integer potentialDelayDays;
}
