package com.customcharge.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Service charge mapping screen data")
public class ServiceChargeMappingScreenDTO {

    private List<FromServiceCodeDTO> fromServiceCodes;
    private List<ToServiceCodeDTO> toServiceCodes;
    private List<ExistingMappingDTO> mappings;
}
