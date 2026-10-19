package com.customcharge.dashboard.constants;

/**
 * Role validation patterns and messages.
 */
public final class RoleConstants {

    public static final String ROLE_CODE_PATTERN = "^[A-Z][A-Z0-9_]*$";

    public static final String ROLE_CODE_VALIDATION_MESSAGE =
            "Role code must start with a letter and contain only uppercase letters, digits, and underscores";

    public static final String SUCCESS_RESPONSE = "Success";
    public static final String DERIVED_CODE_PREFIX = "R_";
    public static final int MAX_ROLE_CODE_LENGTH = 100;

    private RoleConstants() {
    }
}
