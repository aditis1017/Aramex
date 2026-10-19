package com.customcharge.persistencelayer.security.entra;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.AntPathMatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * Matches public paths on servlet path (after context path) and on full {@link HttpServletRequest#getRequestURI()},
 * so YAML may list {@code /swagger-ui/**} or {@code /dashboard/swagger-ui/**} when {@code context-path} is set.
 */
public final class EntraPublicPathMatchers {

    private static final AntPathMatcher URI_MATCHER = new AntPathMatcher();

    private EntraPublicPathMatchers() {
    }

    public static RequestMatcher fromPatterns(List<String> patterns) {
        List<RequestMatcher> matchers = new ArrayList<>();
        if (patterns != null) {
            for (String pattern : patterns) {
                if (pattern == null || pattern.isBlank()) {
                    continue;
                }
                String trimmed = pattern.trim();
                matchers.add(new AntPathRequestMatcher(trimmed));
                matchers.add(request -> matchesRequestUri(request, trimmed));
            }
        }
        if (matchers.isEmpty()) {
            return request -> false;
        }
        return new OrRequestMatcher(matchers);
    }

    private static boolean matchesRequestUri(HttpServletRequest request, String pattern) {
        String uri = request.getRequestURI();
        return uri != null && URI_MATCHER.match(pattern, uri);
    }
}
