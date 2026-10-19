package com.customcharge.dashboard.rbac;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** RFC 6265-safe cookie values (no raw JSON quotes in {@code Set-Cookie}). */
public final class RouteAccessCookieValueCodec {

    private RouteAccessCookieValueCodec() {}

    public static String encode(String jsonPayload) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(jsonPayload.getBytes(StandardCharsets.UTF_8));
    }

    public static String decode(String cookieValue) {
        if (cookieValue == null || cookieValue.isBlank()) {
            return cookieValue;
        }
        String trimmed = cookieValue.trim();
        if (trimmed.startsWith("{")) {
            return trimmed;
        }
        return new String(Base64.getUrlDecoder().decode(trimmed), StandardCharsets.UTF_8);
    }
}
