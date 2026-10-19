package com.customcharge.persistencelayer.dto;

/**
 * Active currency mapped to a country for UI dropdowns and lookups.
 */
public record ActiveCurrencyDto(
        Long id,
        String currencyCode,
        String currencyName,
        String countryCode,
        String countryName) {
}
