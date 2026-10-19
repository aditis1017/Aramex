package com.customcharge.persistencelayer.dto;

import com.customcharge.persistencelayer.enums.Operator;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GlobalRuleCriteriaDto extends AbstractDto{
    private String globalSettingId;
    private String fieldName;
    @Enumerated(EnumType.STRING)
    private Operator operator;
    private String operatorValue;
    private String values;
    private String exactValue;
    private String currency;
}
