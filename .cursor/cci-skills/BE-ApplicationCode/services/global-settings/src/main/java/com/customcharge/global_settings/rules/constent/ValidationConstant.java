package com.customcharge.global_settings.rules.constent;

public  class ValidationConstant {
    private ValidationConstant() {
    }

    public static final String ID_MUST_NOT_NULL = "Global setting id is required";

    public static final String DUPLICATE_GLOBAL_SETTING_ACTIVE =
            "Rule already exists.";

    public static final String DUPLICATE_GLOBAL_SETTING_INACTIVE =
            "Rule already exists.";

    public static final String IN_OPERATOR_REQUIRES_MULTIPLE_VALUES =
            "IN operator requires at least two values. Use Equals when selecting a single value.";

    public static final String NOT_IN_OPERATOR_REQUIRES_MULTIPLE_VALUES =
            "NOT IN operator requires at least two values. Use Not Equals when excluding a single value.";
}