package com.customcharge.persistencelayer.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class GlobalSettingDto extends AbstractDto{
    private String destination;
    private List<GlobalRuleCriteriaDto> rules;
    private String drlRule;
    private String targetField;
    private String targetValue;
    private Boolean status;
    private Integer sequenceNumber;
    private String ruleSignature;
    private Boolean editable;
}
