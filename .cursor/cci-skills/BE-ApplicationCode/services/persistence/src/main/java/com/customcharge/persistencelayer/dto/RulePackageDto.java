package com.customcharge.persistencelayer.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RulePackageDto extends AbstractDto {
    private String packageName;
    private String description;
    private Boolean isActive;
}
