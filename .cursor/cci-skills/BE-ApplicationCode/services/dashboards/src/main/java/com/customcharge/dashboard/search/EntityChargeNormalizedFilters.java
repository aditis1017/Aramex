package com.customcharge.dashboard.search;

import com.customcharge.persistencelayer.search.SearchCriteria;

import java.util.List;

public record EntityChargeNormalizedFilters(
        List<SearchCriteria> criteria,
        List<String> accountNumbers,
        List<String> accountTypes,
        List<String> boeNumbers) {

    public static EntityChargeNormalizedFilters empty() {
        return new EntityChargeNormalizedFilters(List.of(), List.of(), List.of(), List.of());
    }
}
