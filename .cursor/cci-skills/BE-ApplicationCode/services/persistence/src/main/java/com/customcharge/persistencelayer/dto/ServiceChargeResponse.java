package com.customcharge.persistencelayer.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class ServiceChargeResponse {
    private UUID id;
    private String serviceCode;
    private String description;
    private String serviceDescription;
    private String type;
    private Boolean isActive;
    private Double taxValue;
    private String taxCode;
    private Boolean active;
    private String defaultAccountNumber;
    private Boolean isBillable;
    private String split;
    private String mappedServiceCode;
    private String mappedServiceDescription;
    private String infoAxsId;
    private Boolean fetchTax;
}