package com.customcharge.persistencelayer.dto;

public record ServiceChargeDescriptionMappingDto(
        String fromDescription,
        String fromCode,
        String toDescription,
        String toCode) {
}
