package com.customcharge.dashboard.constants;

/**
 * Structured log message templates for role operations.
 */
public final class RoleLogMessages {

    public static final String ROLE_CREATE_REJECTED_CODE_EXISTS =
            "Role create rejected: code already exists code={}";
    public static final String ROLE_CREATE_INTEGRITY_FAILURE =
            "Role create failed due to data integrity (possible concurrent duplicate) code={}";
    public static final String ROLE_CREATED = "Role created id={} code={}";
    public static final String ROLE_UPDATE_INTEGRITY_FAILURE =
            "Role update failed due to data integrity id={} code={}";
    public static final String ROLE_UPDATED = "Role updated id={}";
    public static final String ROLE_DETAILS_LOAD_FAILED = "Failed to load role details for roleId={}";
    public static final String ROLE_DETAILS_LOAD_FAILED_FOR_USER =
            "Failed to load role details for authenticated user";
    public static final String PERMISSIONS_UPDATED = "Permissions updated for roleId={}";
    public static final String ROLE_DUPLICATE_INTEGRITY_FAILURE =
            "Role duplicate failed due to data integrity code={}";
    public static final String ROLE_DUPLICATED = "Role duplicated id={} code={}";

    private RoleLogMessages() {
    }
}
