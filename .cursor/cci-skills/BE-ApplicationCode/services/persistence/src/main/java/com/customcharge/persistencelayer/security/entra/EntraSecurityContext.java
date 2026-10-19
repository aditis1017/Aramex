package com.customcharge.persistencelayer.security.entra;

import com.customcharge.persistencelayer.entities.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Accessors for the authenticated Entra user established by {@link EntraJwtAuthenticationFilter}.
 */
public final class EntraSecurityContext {

    private EntraSecurityContext() {
    }

    public static Optional<User> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof EntraUserAuthenticationToken token
                && token.isAuthenticated()
                && token.getUser() != null) {
            return Optional.of(token.getUser());
        }
        return Optional.empty();
    }

    public static User requireCurrentUser() {
        return currentUser().orElseThrow(() -> new EntraRequestDeniedException(
                HttpStatus.UNAUTHORIZED,
                "UNAUTHORIZED",
                "Authenticated user is required"));
    }
}
