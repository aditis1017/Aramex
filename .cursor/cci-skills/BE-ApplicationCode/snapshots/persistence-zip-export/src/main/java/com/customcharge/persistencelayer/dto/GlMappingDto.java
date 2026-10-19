package com.customcharge.persistencelayer.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class GlMappingDto extends AbstractDto {
    private UUID accountId;
    private UUID entityId;
    private String chargeCode;
    private UUID serviceCodeId;
    private String side;
    private String companyCode;
    private String glAccount;
    private String costCenter;
    private String taxCode;
}
