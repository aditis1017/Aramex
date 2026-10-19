package com.customcharge.persistencelayer.cache.dto;

public record MasterCategorySummary(
        Long id,
        String code,
        String name,
        Boolean isActive,
        Boolean entityScoped
) {
}
