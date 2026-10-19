package com.customcharge.persistencelayer.security.entra;

import com.customcharge.persistencelayer.entities.Role;
import com.customcharge.persistencelayer.entities.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntraSecurityContextTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void currentUserReturnsAuthenticatedEntraUser() {
        User user = new User();
        user.setEmail("user@example.com");
        JwtUserIdentity identity = new JwtUserIdentity(
                "sub",
                "user@example.com",
                "user@example.com",
                "oid",
                "user@example.com",
                "user@example.com",
                List.of(),
                List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new EntraUserAuthenticationToken(user, identity, List.of(new SimpleGrantedAuthority("VIEW"))));

        assertTrue(EntraSecurityContext.currentUser().isPresent());
        assertEquals("user@example.com", EntraSecurityContext.requireCurrentUser().getEmail());
    }

    @Test
    void requireCurrentUserThrowsWhenUnauthenticated() {
        EntraRequestDeniedException ex =
                assertThrows(EntraRequestDeniedException.class, EntraSecurityContext::requireCurrentUser);
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getHttpStatus());
        assertEquals("UNAUTHORIZED", ex.getErrorCode());
    }
}
