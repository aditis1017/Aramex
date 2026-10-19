package com.customcharge.persistencelayer.enums;

public enum MatchType {
    CONTAINS,
    EXACT,
    STARTWITH,
    ENDWITH;

    public String displayLabel() {
        return switch (this) {
            case STARTWITH -> "START WITH";
            case ENDWITH -> "END WITH";
            default -> name();
        };
    }
}
