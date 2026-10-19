package com.customcharge.persistencelayer.audit;

import com.customcharge.persistencelayer.recordtracking.RecordTrackingMdc;
import com.customcharge.persistencelayer.security.entra.EntraUserAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;


public final class AuditAuthorResolver {

    public static final String SYSTEM_AUDITOR = "SYSTEM";

    private AuditAuthorResolver() {
    }

    public static String resolve() {
        return fromSecurityContext()
                .or(AuditAuthorContext::current)
                .or(AuditAuthorResolver::fromRecordTracking)
                .orElse(SYSTEM_AUDITOR);
    }

    public static Optional<String> fromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof EntraUserAuthenticationToken token
                && token.getUser() != null
                && !isBlank(token.getUser().getEmail())) {
            return Optional.of(token.getUser().getEmail().trim());
        }
        return Optional.empty();
    }

    public static Optional<String> fromRecordTracking() {
        return RecordTrackingMdc.uploadedBy();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
