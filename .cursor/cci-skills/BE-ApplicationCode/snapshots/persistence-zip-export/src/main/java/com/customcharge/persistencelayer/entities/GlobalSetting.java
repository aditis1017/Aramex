package com.customcharge.persistencelayer.entities;

import com.customcharge.persistencelayer.config.javers.AuditableEntity;
import com.customcharge.persistencelayer.entity.AbstractEntity;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.javers.core.metamodel.annotation.DiffIgnore;
import jakarta.persistence.Entity;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "global_setting")
@NoArgsConstructor
@AllArgsConstructor
@AuditableEntity(
        groupName = "GLOBAL_SETTING"
)
public class GlobalSetting extends AbstractEntity {

    @Column(name = "destination_code")
    private String destination;

    @Column(name = "target_field")
    private String targetField;

    @Column(name = "target_value")
    private String targetValue;

    @Column(name = "status")
    private Boolean status;

    @Column(name = "sequence_number")
    private Integer sequenceNumber;

    @DiffIgnore
    @Column(name = "rule_signature")
    private String ruleSignature;

    @DiffIgnore
    @OneToMany(
            mappedBy = "globalSetting",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @JsonManagedReference
    private List<GlobalRuleCriteria> rules = new ArrayList<>();

    @DiffIgnore
    @Column(columnDefinition = "TEXT",name = "drl_rule")
    private String drlRule;

    @Column(columnDefinition = "TEXT", name = "rule_condition")
    private String ruleCondition;
}