package com.customcharge.persistencelayer.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ServiceCodeDto extends AbstractDto {
    private UUID entityId;
    private UUID accountId;
    private String chargeCode;
    private Integer serviceCodeNumber;
    private String category;
    private String description;
    private String tax;
    private Boolean active;
}
