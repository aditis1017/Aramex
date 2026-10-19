package com.customcharge.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Schema(description = "DTA to ECS/EC3 service code pair")
public class ServiceChargeMappingPairDTO {

    @NotNull(message = "From service code uuid is required")
    @Schema(description = "Source (DTA) service_codes.id")
    private UUID fromUuid;

    @NotNull(message = "To service code uuid is required")
    @Schema(description = "Destination (ECS/EC3) service_codes.id")
    private UUID toUuid;
}
