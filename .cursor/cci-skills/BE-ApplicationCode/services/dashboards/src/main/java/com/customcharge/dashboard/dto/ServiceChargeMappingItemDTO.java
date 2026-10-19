package com.customcharge.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceChargeMappingItemDTO {
    private String serviceCode;
    private String serviceDescription;
}
