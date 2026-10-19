package com.customcharge.dashboard.constants;

public final class RoleMessageConstants {

    public static final String ROLE_NAME_ALREADY_EXISTS = "Role name already exists";
    public static final String ROLE_SAVE_REJECTED_NAME_EXISTS =
            "Role save rejected: name already exists name={}";
    public static final String ROLE_NAME_REQUIRED = "Role name is required";
    public static final String ROLE_ALREADY_EXISTS = "Role already exists";
    public static final String ROLE_CODE_ALREADY_EXISTS = "A role with this code already exists";
    public static final String ROLE_ID_CANNOT_BE_NULL = "Role ID cannot be null";
    public static final String ROLE_PAYLOAD_CANNOT_BE_NULL = "Role payload cannot be null";
    public static final String ROLE_NOT_FOUND = "Role not found";
    public static final String ROLE_CODE_ALREADY_IN_USE = "Role code already in use";
    public static final String ROLE_NOT_FOUND_FOR_ID_PREFIX = "Role not found for id: ";
    public static final String UNABLE_TO_LOAD_ROLE_DETAILS =
            "Unable to load role details. Please try again later.";
    public static final String ROLE_NOT_FOUND_FOR_AUTHENTICATED_USER =
            "Role not found for authenticated user";
    public static final String REQUEST_BODY_WITH_MODULES_REQUIRED =
            "Request body with modules is required";
    public static final String MODULE_NOT_FOUND = "Module not found";
    public static final String PERMISSION_CODE_CANNOT_BE_BLANK =
            "Permission code cannot be null or blank";
    public static final String INVALID_PERMISSION_FOR_MODULE_PREFIX =
            "Invalid permission for module: ";
    public static final String ROLE_NAME_REQUIRED_TO_DERIVE_CODE =
            "Role name is required to derive a role code";
    public static final String ROLE_NAME_MUST_CONTAIN_ALPHANUMERIC =
            "Role name must contain letters or digits";
    public static final String ROLE_CODE_FORMAT_INVALID =
            "Role code must start with a letter and contain only uppercase letters, digits, and underscores";

    private RoleMessageConstants() {
    }
}
