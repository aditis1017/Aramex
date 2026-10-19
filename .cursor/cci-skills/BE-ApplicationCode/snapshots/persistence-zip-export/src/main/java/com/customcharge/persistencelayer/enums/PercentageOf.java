package com.customcharge.persistencelayer.enums;

import java.util.Arrays;

public enum PercentageOf {

    CIF_VALUE("CIF Value"),
    COST_OF_GOODS("Cost of Goods");

    private final String displayName;

    PercentageOf(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static PercentageOf fromDisplayName(String name) {
        return Arrays.stream(values())
                .filter(e -> e.displayName.equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid PercentageOf: " + name));
    }
}