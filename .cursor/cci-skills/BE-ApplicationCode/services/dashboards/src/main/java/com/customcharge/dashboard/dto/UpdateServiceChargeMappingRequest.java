package com.customcharge.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(description = "Partial update: only changed or new mapping rows")
public class UpdateServiceChargeMappingRequest {

    @NotBlank(message = "From entity code is required")
    private String fromEntityCode;

    @NotBlank(message = "To entity code is required")
    private String toEntityCode;

    @NotBlank(message = "Charge type is required")
    private String chargeType;

    @NotNull(message = "Mappings list is required")
    @Valid
    @Schema(description = "Changed rows (with mappingId) and/or new rows (without mappingId); may be empty")
    private List<MappingRequestDTO> mappings;
}
