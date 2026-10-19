package com.customcharge.persistencelayer.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class EntityMasterDto extends AbstractDto {
    private String code;
    private String type;
    private String countryCode;
    private UUID defaultCashAccountId;
}
