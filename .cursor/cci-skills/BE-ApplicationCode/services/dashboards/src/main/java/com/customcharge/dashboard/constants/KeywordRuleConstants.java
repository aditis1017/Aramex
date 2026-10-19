package com.customcharge.dashboard.constants;

import java.util.regex.Pattern;

/**
 * Keyword rule SQL function, schema validation, and match-type tokens.
 */
public final class KeywordRuleConstants {

    public static final String FALLBACK_SCHEMA = "public";
    public static final Pattern SAFE_SCHEMA_PATTERN = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");

    public static final String FUNCTION_NAME = "fn_is_b2b";
    public static final String TABLE_NAME = "keyword_rules";

    public static final String MATCH_TYPE_CONTAINS = "CONTAINS";
    public static final String MATCH_TYPE_EXACT = "EXACT";
    public static final String MATCH_TYPE_STARTS_WITH = "STARTWITH";
    public static final String MATCH_TYPE_ENDS_WITH = "ENDWITH";

    public static final String INVALID_SCHEMA_MESSAGE =
            "Invalid mvp.persistence.jpa.default_schema value: ";

    private KeywordRuleConstants() {
    }
}
