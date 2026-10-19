package com.customcharge.persistencelayer.security.entra;

/**
 * OpenAPI security scheme name for Entra Bearer JWT (Swagger UI Authorize).
 */
public final class SwaggerBearerSecurity {

    public static final String BEARER_JWT_SCHEME = "bearer-jwt";

    /**
     * Optional alternative in Swagger UI: single field for the full {@code Authorization} header value
     * ({@code Bearer <jwt>}).
     */
    public static final String AUTHORIZATION_HEADER_API_KEY_SCHEME = "authorization-header";

    private SwaggerBearerSecurity() {
    }
}
