package com.customcharge.persistencelayer.security.entra;

/**
 * Safe logging helpers for JWT handling (never logs full tokens).
 */
public final class EntraJwtLogSupport {

    private EntraJwtLogSupport() {
    }

    /** Token length and non-secret prefix for debug logs only. */
    public static String tokenHint(String bearerToken) {
        if (bearerToken == null) {
            return "null";
        }
        int len = bearerToken.length();
        if (len <= 12) {
            return "len=" + len;
        }
        return "len=" + len + " prefix=" + bearerToken.substring(0, 8) + "...";
    }
}
