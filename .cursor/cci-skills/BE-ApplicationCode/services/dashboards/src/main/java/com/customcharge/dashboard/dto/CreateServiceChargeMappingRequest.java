package com.customcharge.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(description = "Create service charge mappings from uuid pairs and entity codes")
public class CreateServiceChargeMappingRequest {

    @NotBlank(message = "From entity code is required")
    @Schema(description = "Source entity code", example = "RUH")
    private String fromEntityCode;

    @NotBlank(message = "To entity code is required")
    @Schema(description = "Destination entity code", example = "JED")
    private String toEntityCode;

    @NotBlank(message = "Charge type is required")
    @Schema(description = "Destination charge type (ECS or EC3)", example = "ECS")
    private String chargeType;

    @NotNull(message = "Mappings list is required")
    @NotEmpty(message = "At least one mapping pair is required")
    @Valid
    @Schema(description = "From/to service code uuid pairs to persist")
    private List<ServiceChargeMappingPairDTO> mappings;
}
