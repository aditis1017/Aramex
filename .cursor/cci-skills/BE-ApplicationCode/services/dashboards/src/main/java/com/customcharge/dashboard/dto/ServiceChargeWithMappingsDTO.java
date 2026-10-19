package com.customcharge.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceChargeWithMappingsDTO {
    private UUID id;
    private String serviceCode;
    private String description;
    private String serviceDescription;
    private Boolean isActive;
    private Double taxValue;
    private Boolean active;
    private String taxCode;
    private String defaultAccountNo;
    private Boolean isBillable;
    private String split;
    private Boolean isDefault;
    private Boolean isDeleted;
    private String mappedServiceCode;
    private List<ServiceChargeMappingItemDTO> mappings;
}
