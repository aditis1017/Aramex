package com.customcharge.dashboard.constants;

/**
 * Log message templates for user role operations.
 */
public final class UserRoleLogMessages {

    public static final String SKIP_DEFAULT_USER_CREATION =
            "Skipping default user creation: users already exist";
    public static final String CREATED_DEFAULT_USERS = "Created {} default users";
    public static final String USER_ROLE_UPDATED = "User role updated userId={} roleId={}";

    private UserRoleLogMessages() {
    }
}
