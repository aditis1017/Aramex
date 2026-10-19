package com.customcharge.persistencelayer.config.javers;

import java.util.Locale;

public enum AuditValueNormalize {
    NONE,
    UPPER,
    TRIM_UPPER;

    public String apply(String value) {
        if (value == null) {
            return null;
        }
        return switch (this) {
            case NONE -> value;
            case UPPER -> value.toUpperCase(Locale.ROOT);
            case TRIM_UPPER -> value.trim().toUpperCase(Locale.ROOT);
        };
    }
}
