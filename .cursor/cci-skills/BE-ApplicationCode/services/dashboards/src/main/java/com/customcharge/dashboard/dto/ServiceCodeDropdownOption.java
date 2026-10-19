package com.customcharge.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Service code option for rate-sheet dropdowns")
public class ServiceCodeDropdownOption {

    private String serviceCode;
    private String serviceDescription;
}
