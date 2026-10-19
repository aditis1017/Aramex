package com.customcharge.persistencelayer.entities;

import com.customcharge.persistencelayer.entity.AbstractEntity;
import jakarta.persistence.*;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "invoice_line_items")
public class InvoiceLineItem extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "charge_id")
//    private ChargeLine chargeLine;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "service_code_id")
//    private ServiceCode serviceCode;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "gl_account")
    private String glAccount;

    @Column(name = "company_code")
    private String companyCode;
}
