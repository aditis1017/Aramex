package com.customcharge.persistencelayer.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class RuleDefinitionDto extends AbstractDto {
    private UUID packageId;
    private String ruleKey;
    private String drlContent;
    private Integer versionNumber;
    private UUID statusId;
}
