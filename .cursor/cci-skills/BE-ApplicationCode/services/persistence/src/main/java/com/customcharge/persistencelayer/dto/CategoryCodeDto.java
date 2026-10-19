package com.customcharge.persistencelayer.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class CategoryCodeDto extends AbstractDto {
    private UUID categoryId;
    private String codeKey;
    private String displayName;
    private String entityCode;
    private Boolean isActive;
}
