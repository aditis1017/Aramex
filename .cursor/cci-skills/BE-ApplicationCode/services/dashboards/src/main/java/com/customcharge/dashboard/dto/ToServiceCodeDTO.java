package com.customcharge.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "ECS/EC3 service code on the destination entity")
public class ToServiceCodeDTO {

    private UUID uuid;
    private String serviceCode;
    private String serviceDescription;
}
