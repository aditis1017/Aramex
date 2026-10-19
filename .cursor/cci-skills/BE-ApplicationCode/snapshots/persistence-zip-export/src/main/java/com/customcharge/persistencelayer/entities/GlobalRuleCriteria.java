package com.customcharge.persistencelayer.entities;


import com.customcharge.persistencelayer.config.javers.AuditableEntity;
import com.customcharge.persistencelayer.entity.AbstractEntity;
import com.customcharge.persistencelayer.enums.Operator;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "global_rule_criteria")
@Getter
@Setter
@AuditableEntity(
        groupName = "GLOBAL_SETTING_CRITERIA"
)
public class GlobalRuleCriteria extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "global_setting_id", nullable = false)
    @JsonBackReference
    @JsonIgnore
    private GlobalSetting globalSetting;

    @Column(name = "field_name")
    private String fieldName;

    @Enumerated(EnumType.STRING)
    @Column(name = "operator")
    private Operator operator;

    @Column(name = "values")
    private String values;

    @Column(name = "exact_value")
    private String exactValue;

    @Column(name = "currency")
    private String currency;

    public GlobalRuleCriteria(){}

    public GlobalRuleCriteria(String fieldName, Operator operator, String values){
        this.fieldName=fieldName;
        this.operator=operator;
        this.values=values;
    }

}
