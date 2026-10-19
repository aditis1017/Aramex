package com.customcharge.dashboard.rbac;

import com.customcharge.dashboard.cache.DashboardCacheNames;
import com.customcharge.dashboard.dto.RoleDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(RouteAccessCookieProperties.class)
public class RouteAccessCookieService {

    private final RouteAccessCookieProperties properties;
    private final ObjectMapper objectMapper;

    public void attachRouteAccessCookie(HttpServletResponse response, RoleDTO role) {
        if (response == null || role == null) {
            return;
        }
        String encoded = buildEncodedCookieValue(role);
        ResponseCookie cookie = ResponseCookie.from(properties.getName(), encoded)
                .httpOnly(true)
                .secure(properties.isSecure())
                .sameSite(properties.getSameSite())
                .path(normalizePath(properties.getPath()))
                .maxAge(Duration.ofDays(properties.getMaxAgeDays()))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    @Cacheable(cacheNames = DashboardCacheNames.ROUTE_ACCESS_COOKIE, key = "#role.id")
    public String buildEncodedCookieValue(RoleDTO role) {
        String json = RouteAccessCookieFactory.buildCookieJson(role.getPermissions(), objectMapper);
        return RouteAccessCookieValueCodec.encode(json);
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }
}
