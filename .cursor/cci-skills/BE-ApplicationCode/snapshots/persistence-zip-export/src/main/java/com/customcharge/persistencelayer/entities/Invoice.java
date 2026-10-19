package com.customcharge.persistencelayer.entities;

import com.customcharge.persistencelayer.entity.AbstractEntity;
import jakarta.persistence.*;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "invoice")
public class Invoice extends AbstractEntity {

    @Column(name = "invoice_type")
    private String invoiceType;

    @Column(name = "invoice_number", unique = true)
    private String invoiceNumber;

    @Column(name = "internal_reference")
    private String internalReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    @Column(name = "entity_code")
    private String entityCode;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Column(name = "currency")
    private String currency;

    @Column(name = "document_date")
    private LocalDate documentDate;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "status")
    private String status;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL)
    private List<InvoiceLineItem> lineItems;
}
