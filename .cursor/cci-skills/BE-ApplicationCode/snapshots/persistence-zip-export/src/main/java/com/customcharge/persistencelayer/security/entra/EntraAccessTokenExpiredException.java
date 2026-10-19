package com.customcharge.persistencelayer.security.entra;

import com.nimbusds.jose.JOSEException;

/**
 * Raised when the access token's {@code exp} (with configured clock skew) is in the past.
 * The servlet filter maps this to HTTP 401 with {@link #ERROR_CODE} and {@link #USER_FACING_MESSAGE} for clients.
 */
public final class EntraAccessTokenExpiredException extends JOSEException {

    public static final String ERROR_CODE = "TOKEN_EXPIRED";

    /**
     * Message returned in API JSON for end users (browsers / mobile); safe to display or log at INFO.
     */
    public static final String USER_FACING_MESSAGE =
            "Your session has expired. Please sign in again to continue.";

    public EntraAccessTokenExpiredException() {
        super("Access token expired: exp is before current time (within clock skew)");
    }
}
