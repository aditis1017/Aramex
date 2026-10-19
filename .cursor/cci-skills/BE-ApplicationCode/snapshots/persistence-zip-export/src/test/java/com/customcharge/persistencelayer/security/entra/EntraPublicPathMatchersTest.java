package com.customcharge.persistencelayer.security.entra;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntraPublicPathMatchersTest {

    @Test
    void matchesServletPathAndFullUri() {
        RequestMatcher matcher = EntraPublicPathMatchers.fromPatterns(
                List.of("/swagger-ui/**", "/dashboard/swagger-ui/**"));

        MockHttpServletRequest servletRelative = new MockHttpServletRequest("GET", "/swagger-ui/index.html");
        servletRelative.setContextPath("/dashboard");
        servletRelative.setServletPath("/swagger-ui/index.html");
        assertTrue(matcher.matches(servletRelative));

        MockHttpServletRequest fullUri = new MockHttpServletRequest("GET", "/dashboard/swagger-ui/index.html");
        fullUri.setContextPath("/dashboard");
        fullUri.setServletPath("/swagger-ui/index.html");
        assertTrue(matcher.matches(fullUri));
    }

    @Test
    void doesNotMatchProtectedApi() {
        RequestMatcher matcher = EntraPublicPathMatchers.fromPatterns(List.of("/swagger-ui/**"));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/dashboard/api/export");
        request.setContextPath("/dashboard");
        request.setServletPath("/api/export");
        assertFalse(matcher.matches(request));
    }
}
