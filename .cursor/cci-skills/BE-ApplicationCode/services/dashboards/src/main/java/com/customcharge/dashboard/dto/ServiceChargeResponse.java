package com.customcharge.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceChargeResponse {
    private UUID id;
    private String serviceCode;
    private String description;
    private String serviceDescription;
    private Double taxValue;
    private String taxCode;
    private Boolean active;
    private String defaultAccountNumber;
    private Boolean isBillable;
    private String split;
    private String mappedServiceCode;
    private String MappedServiceDescription;
    private Boolean editable;
}