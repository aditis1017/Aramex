package com.customcharge.persistencelayer.dto;

import java.util.List;

/**
 * Active country with its active entities for UI dropdowns.
 */
public record CountryWithEntitiesDropdownDto(
        Long countryId,
        String countryCode,
        String countryName,
        List<EntityDropdownOption> entities) {
}
