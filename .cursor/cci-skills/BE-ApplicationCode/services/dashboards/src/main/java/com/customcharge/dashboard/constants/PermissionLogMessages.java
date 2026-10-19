package com.customcharge.dashboard.constants;

/**
 * Structured log message templates for permission matrix operations.
 */
public final class PermissionLogMessages {

    public static final String SKIPPING_NULL_ROLE_CODE =
            "Skipping role permission with null role code for moduleId={}";
    public static final String SKIPPING_UNKNOWN_MODULE =
            "Skipping role permission for unknown moduleId={}";
    public static final String SKIPPING_UNKNOWN_ROLE_CODE =
            "Skipping role permission for unknown roleCode={} moduleId={}";
    public static final String MATRIX_BUILD_FAILED = "Failed to build permission matrix";

    private PermissionLogMessages() {
    }
}
