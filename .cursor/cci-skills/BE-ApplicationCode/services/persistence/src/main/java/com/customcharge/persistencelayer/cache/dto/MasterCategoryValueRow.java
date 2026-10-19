package com.customcharge.persistencelayer.cache.dto;

public record MasterCategoryValueRow(
        Long id,
        Long categoryId,
        String code,
        String name,
        String label,
        Long parentId,
        String entityCode,
        Boolean isActive,
        String loadDataFrom
) {
}
