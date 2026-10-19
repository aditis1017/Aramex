package com.customcharge.persistencelayer.enums;

public enum Operator {
    EQUALS("Equals"),
    NOT_EQUALS("NotEquals"),
    CONTAINS("Contains"),
    IN("In"),
    NOT_IN("Not In"),
    GREATERTHAN("Greater than"),
    LESSTHAN("Less than"),
    GREATERTHANEQUALS("Greater then Equal to"),
    LESSTHANEQUALS("Less than Equal to");

    private final String value;

    Operator(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}