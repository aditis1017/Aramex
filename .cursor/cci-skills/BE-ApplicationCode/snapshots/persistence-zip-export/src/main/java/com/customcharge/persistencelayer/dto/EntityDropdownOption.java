package com.customcharge.persistencelayer.dto;

/**
 * Minimal id/code/name shape for entity options nested under a country in master-data dropdowns.
 */
public record EntityDropdownOption(Long id, String code, String name) {
}
