package com.customcharge.persistencelayer.entities;

import com.customcharge.persistencelayer.entity.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@jakarta.persistence.Entity
@Table(
        name = "service_codes"
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceCodes extends AbstractEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "entity_code",
            referencedColumnName = "code"
    )
    private com.customcharge.persistencelayer.entities.Entity entity;

    @NotBlank
    @Size(max = 50)
    @Column(name = "service_code", nullable = false, length = 50)
    private String serviceCode;

    @Size(max = 255)
    @Column(name = "service_description", length = 255)
    private String serviceDescription;

    @Size(max = 50)
    @Column(name = "tax_code", length = 50)
    private String taxCode;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "fetch_tax")
    private Boolean fetchTax;

    @Column(name = "type")
    private String type;

    @Size(max = 50)
    @Column(name = "charge_type", length = 50)
    private String chargeType;

    @Column(name = "doc_type")
    private Integer docType;

    @Column(name = "info_axs_id")
    private String infoAxsId;

}
