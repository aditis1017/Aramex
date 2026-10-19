package com.customcharge.dashboard.constants;

import java.util.List;

/**
 * Default user, role, and module seed data for user-role bootstrap.
 */
public final class UserRoleConstants {

    public static final String DEFAULT_ROLE_CODE = "OPERATIONS_STAFF";
    public static final String DEFAULT_USER_PASSWORD = "password";
    public static final String SUCCESS_RESPONSE = "Success";

    public static final String MODULE_CUSTOMS = "CUSTOMS";
    public static final String MODULE_DOCUMENTS_LIST = "DOCUMENTS_LIST";
    public static final String MODULE_REPORTS = "REPORTS";
    public static final String MODULE_AR_MANAGEMENT = "AR_MANAGEMENT";
    public static final String MODULE_AP_MANAGEMENT = "AP_MANAGEMENT";
    public static final String MODULE_GLOBAL_SETTINGS = "GLOBAL_SETTINGS";
    public static final String MODULE_ENTITY_SETTINGS = "ENTITY_SETTINGS";
    public static final String MODULE_B2B_KEYWORDS_MANAGER = "B2B_KEYWORDS_MANAGER";

    public static final List<String[]> DEFAULT_MODULES = List.of(
            new String[]{MODULE_CUSTOMS, "Customs"},
            new String[]{MODULE_DOCUMENTS_LIST, "Documents List"},
            new String[]{MODULE_REPORTS, "Reports"},
            new String[]{MODULE_AR_MANAGEMENT, "AR Management"},
            new String[]{MODULE_AP_MANAGEMENT, "AP Management"},
            new String[]{MODULE_GLOBAL_SETTINGS, "Global Settings"},
            new String[]{MODULE_ENTITY_SETTINGS, "Entity Settings"},
            new String[]{MODULE_B2B_KEYWORDS_MANAGER, "B2B Keywords Manager"}
    );

    public static final List<String[]> DEFAULT_USERS = List.of(
            new String[]{"malek.hijazi", "malek.hijazi@example.com"},
            new String[]{"zaid.allawi", "zaid.allawi@example.com"},
            new String[]{"rama.alsaid", "rama.alsaid@example.com"},
            new String[]{"omar.khalil", "omar.khalil@example.com"},
            new String[]{"layla.mansour", "layla.mansour@example.com"}
    );

    public static final String NO_ROLES_FOR_DEFAULT_USERS =
            "No roles found: seed roles before creating default users";
    public static final String USER_NOT_FOUND = "User not found";
    public static final String ROLE_NOT_FOUND = "Role not found";
    public static final String USER_ID_CANNOT_BE_NULL = "User ID cannot be null";
    public static final String ROLE_ID_REQUIRED = "roleId is required";

    private UserRoleConstants() {
    }
}
