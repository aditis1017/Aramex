package com.customcharge.persistencelayer.enums;

import java.util.Arrays;

public enum RateType {
    FIXED("Fixed"),
    PERCENTAGE("Percentage");

    private final String displayName;

    RateType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static RateType fromDisplayName(String name) {
        return Arrays.stream(values())
                .filter(e -> e.displayName.equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid PercentageOf: " + name));
    }
}