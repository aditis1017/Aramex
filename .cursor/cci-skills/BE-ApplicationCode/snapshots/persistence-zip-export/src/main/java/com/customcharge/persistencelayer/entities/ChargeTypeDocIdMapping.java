package com.customcharge.persistencelayer.entities;

import com.customcharge.persistencelayer.entity.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "charge_type_doc_id_mapping")
@Getter
@Setter
@NoArgsConstructor
public class ChargeTypeDocIdMapping extends AbstractEntity {

    @Column(name = "charge_type", nullable = false, unique = true, length = 10)
    private String chargeType;

    @Column(name = "doc_id", nullable = false, unique = true, length = 10)
    private String docId;
}
