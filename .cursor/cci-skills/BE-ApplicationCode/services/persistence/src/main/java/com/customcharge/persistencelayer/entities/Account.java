package com.customcharge.persistencelayer.entities;

import com.customcharge.persistencelayer.config.javers.AuditCommitProperties;
import com.customcharge.persistencelayer.config.javers.AuditCommitProperty;
import com.customcharge.persistencelayer.config.javers.AuditableEntity;
import com.customcharge.persistencelayer.config.javers.AuditValueNormalize;
import com.customcharge.persistencelayer.entity.AbstractEntity;
import com.customcharge.persistencelayer.enums.AccountNature;
import com.customcharge.persistencelayer.util.AccountVersionSupport;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.javers.core.metamodel.annotation.DiffIgnore;

import java.util.List;

/**
 * Account row: AP charge-type config via {@link APAccount} master + {@code chargeType};
 * AR charge-type config via {@link ARAccount} master + {@code chargeType}.
 */
@jakarta.persistence.Entity
@Table(
        name = "account",
        indexes = {
                @Index(name = "idx_account_entity_code", columnList = "entity_code"),
                @Index(name = "idx_account_nature", columnList = "account_nature"),
                @Index(name = "idx_account_ar_master_charge", columnList = "ar_account_id, charge_type"),
                @Index(name = "idx_account_ap_master_charge", columnList = "ap_account_id, charge_type")
        })
@Getter
@Setter
@NoArgsConstructor
@AuditableEntity(
        groupName = AuditCommitProperties.GROUP_ACCOUNT_AR_AP,
        commitProperties = {
                @AuditCommitProperty(key = AuditCommitProperties.ACCOUNT_ID, path = "id"),
                @AuditCommitProperty(
                        key = AuditCommitProperties.ENTITY_CODE,
                        path = "entityCode",
                        normalize = AuditValueNormalize.UPPER)
        })
public class Account extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "entity_code",
            referencedColumnName = "code",
            nullable = false
    )
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @DiffIgnore
    private Entity entity;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_nature", nullable = false, length = 50)
    private AccountNature accountNature;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ar_account_id")
    @DiffIgnore
    private ARAccount arAccount;

    @Column(name = "split_entry", length = 100)
    private String splitEntry;

    @Column(name = "entry_limit", length = 100)
    private String entryLimit;

    @Column(name = "charge_type", length = 50)
    private String chargeType;

    @Column(name = "sap_country_code", length = 50)
    private String sapCountryCode;

    @Column(name = "default_cash_account", length = 50)
    private String defaultCashAccount;

    @Column(name = "split_invoice", length = 50)
    private String splitInvoice;

    @Column(name = "invoice_limit", length = 50)
    private String invoiceLimit;

    @Column(name = "eligible_for_billing", length = 50)
    private String eligibleForBilling;

    @Column(name = "invoice_trigger", length = 50)
    private String invoiceTrigger;

    @Column(name = "rate_currency", length = 100)
    private String rateCurrency;

    @Column(name = "billing_currency", length = 100)
    private String billingCurrency;

    @Column(name = "account", length = 100)
    private String account;

    @Column(name = "default_account_number", length = 100)
    private String defaultAccountNumber;

    @Column(name = "default_ar_account_number", length = 100)
    private String defaultArAccountNumber;

    @Column(name = "invoice_frequency", length = 100)
    private String invoiceFrequencyEntitySettings;

    @Column(name = "ar_invoice_frequency", length = 100)
    private String arInvoiceFrequency;

    @Column(name = "ar_potential_delay", length = 100)
    private String arPotentialDelay;

    @Column(name = "ar_invoice_amount_limit", length = 100)
    private String arInvoiceAmountLimit;

    @Column(name = "ar_invoice_shipment_limit", length = 100)
    private String arInvoiceShipmentLimit;

    @Column(name = "ar_invoice_break_by", length = 255)
    private String arInvoiceBreakBy;

    @Column(name = "ar_internal_account_number", length = 100)
    private String arInternalAccountNumber;

    @Column(name = "ar_amount_code", length = 100)
    private String arAmountCode;

    @Version
    @Column(name = "version")
    private Long version;

    @PrePersist
    @PreUpdate
    void ensureVersionInitialized() {
        AccountVersionSupport.ensureInitialized(this);
    }

    @OneToMany(mappedBy = "account", fetch = FetchType.LAZY)
    @DiffIgnore
    private List<ServiceCharge> serviceCharges;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ap_account_id")
    @DiffIgnore
    private APAccount apAccount;

    public Entity getEntity() {
        return entity;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    public String getAccountName() {
        if (accountNature == AccountNature.AP && apAccount != null) {
            return apAccount.getAccountName();
        }
        if (accountNature == AccountNature.AR && arAccount != null) {
            return arAccount.getAccountName();
        }
        return null;
    }

    public void setAccountName(String accountName) {
        if (accountNature == AccountNature.AP && apAccount != null) {
            apAccount.setAccountName(accountName);
        } else if (accountNature == AccountNature.AR && arAccount != null) {
            arAccount.setAccountName(accountName);
        }
    }

    public String getAccountNumber() {
        if (accountNature == AccountNature.AP && apAccount != null) {
            return apAccount.getAccountNumber();
        }
        if (accountNature == AccountNature.AR && arAccount != null) {
            return arAccount.getAccountNumber();
        }
        return null;
    }

    public void setAccountNumber(String accountNumber) {
        if (accountNature == AccountNature.AP && apAccount != null) {
            apAccount.setAccountNumber(accountNumber);
        } else if (accountNature == AccountNature.AR && arAccount != null) {
            arAccount.setAccountNumber(accountNumber);
        }
    }

    public String getEntityCode() {
        return entity == null ? null : entity.getCode();
    }

    public String getCountryCode() {
        if (accountNature == AccountNature.AR && arAccount != null && arAccount.getCountryCode() != null) {
            return arAccount.getCountryCode();
        }
        return entity == null || entity.getCountry() == null ? null : entity.getCountry().getCode();
    }

    public String getChargeType() {
        return chargeType;
    }

    public void setChargeType(String chargeType) {
        this.chargeType = chargeType;
    }

    public String getMarketStructure() {
        return apAccount == null ? null : apAccount.getMarketStructure();
    }

    public void setMarketStructure(String marketStructure) {
        if (apAccount != null) {
            apAccount.setMarketStructure(marketStructure);
        }
    }

    public String getAccountType() {
        return apAccount == null ? null : apAccount.getAccountType();
    }

    public void setAccountType(String accountType) {
        if (accountNature == AccountNature.AP && apAccount != null) {
            apAccount.setAccountType(accountType);
        }
    }

    public String getARAccountType() {
        return arAccount == null ? null : arAccount.getAccountType();
    }

    public void setARAccountType(String accountType) {
        if (arAccount != null) {
            arAccount.setAccountType(accountType);
        }
    }

    /** AP workflow / display status on the master row. */
    public String getStatus() {
        return apAccount == null ? null : apAccount.getStatus();
    }

    public void setStatus(String status) {
        if (apAccount != null) {
            apAccount.setStatus(status);
        }
    }

    public String getAccountStatus() {
        if (accountNature == AccountNature.AR) {
            return arAccount == null ? null : arAccount.getStatus();
        }
        return apAccount == null ? null : apAccount.getStatus();
    }

    public void setAccountStatus(String accountStatus) {
        if (accountNature == AccountNature.AR) {
            if (arAccount != null) {
                arAccount.setStatus(accountStatus);
            }
            return;
        }
        if (apAccount != null) {
            apAccount.setStatus(accountStatus);
        }
    }

    public String getInvoiceFrequent() {
        if (accountNature == AccountNature.AR) {
            return arInvoiceFrequency;
        }
        return invoiceFrequencyEntitySettings;
    }

    public void setInvoiceFrequent(String invoiceFrequent) {
        if (accountNature == AccountNature.AR) {
            this.arInvoiceFrequency = invoiceFrequent;
        } else {
            this.invoiceFrequencyEntitySettings = invoiceFrequent;
        }
    }

    public String getTriggerInvoice() {
        return invoiceTrigger;
    }

    public void setTriggerInvoice(String triggerInvoice) {
        this.invoiceTrigger = triggerInvoice;
    }

    public String getEligibleForBilling() {
        return eligibleForBilling;
    }

    public void setEligibleForBilling(String eligibleForBilling) {
        this.eligibleForBilling = eligibleForBilling;
    }

    public String getInvoiceBreakBy() {
        return arInvoiceBreakBy;
    }

    public void setInvoiceBreakBy(String invoiceBreakBy) {
        this.arInvoiceBreakBy = invoiceBreakBy;
    }

    public String getInvoiceShipmentLimit() {
        return arInvoiceShipmentLimit;
    }

    public void setInvoiceShipmentLimit(String invoiceShipmentLimit) {
        this.arInvoiceShipmentLimit = invoiceShipmentLimit;
    }

    public String getInvoiceAmountLimit() {
        return arInvoiceAmountLimit;
    }

    public void setInvoiceAmountLimit(String invoiceAmountLimit) {
        this.arInvoiceAmountLimit = invoiceAmountLimit;
    }

    public String getPotentialDelay() {
        return arPotentialDelay;
    }

    public void setPotentialDelay(String potentialDelay) {
        this.arPotentialDelay = potentialDelay;
    }

    public String getAmount() {
        return arAmountCode;
    }

    public void setAmount(String amount) {
        this.arAmountCode = amount;
    }

    public String getArAccountNumber() {
        if (arAccount != null && arAccount.getArAccountNumber() != null) {
            return arAccount.getArAccountNumber();
        }
        return arInternalAccountNumber;
    }

    public void setArAccountNumber(String arAccountNumber) {
        if (arAccount != null) {
            arAccount.setArAccountNumber(arAccountNumber);
        }
        this.arInternalAccountNumber = arAccountNumber;
    }
}
