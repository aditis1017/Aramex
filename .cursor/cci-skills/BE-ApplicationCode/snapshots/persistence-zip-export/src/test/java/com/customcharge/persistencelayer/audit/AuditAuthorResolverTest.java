package com.customcharge.persistencelayer.audit;

import com.customcharge.persistencelayer.entities.User;
import com.customcharge.persistencelayer.recordtracking.RecordTrackingMdc;
import com.customcharge.persistencelayer.security.entra.EntraUserAuthenticationToken;
import com.customcharge.persistencelayer.security.entra.JwtUserIdentity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuditAuthorResolverTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        MDC.clear();
        AuditAuthorContext.clear();
    }

    @Test
    void resolve_shouldPreferAuthenticatedEntraUser() {
        User user = new User();
        user.setEmail("user@aramex.com");
        SecurityContextHolder.getContext().setAuthentication(
                new EntraUserAuthenticationToken(
                        user,
                        new JwtUserIdentity(
                                "sub",
                                "user@aramex.com",
                                "user@aramex.com",
                                "oid",
                                "user@aramex.com",
                                "user@aramex.com",
                                List.of(),
                                List.of()),
                        List.of()));
        RecordTrackingMdc.putUploadedBy("batch-uploader@aramex.com");

        assertThat(AuditAuthorResolver.resolve()).isEqualTo("user@aramex.com");
    }

    @Test
    void resolve_shouldUseUploadedByWhenNoSecurityContext() {
        RecordTrackingMdc.putUploadedBy("batch-uploader@aramex.com");

        assertThat(AuditAuthorResolver.resolve()).isEqualTo("batch-uploader@aramex.com");
    }

    @Test
    void resolve_shouldUseAuditAuthorContextWhenMdcCleared() {
        RecordTrackingMdc.putUploadedBy("batch-uploader@aramex.com");
        MDC.remove(RecordTrackingMdc.UPLOADED_BY);

        assertThat(AuditAuthorResolver.resolve()).isEqualTo("batch-uploader@aramex.com");
    }

    @Test
    void resolve_shouldFallbackToSystemWhenNoContextAvailable() {
        assertThat(AuditAuthorResolver.resolve()).isEqualTo(AuditAuthorResolver.SYSTEM_AUDITOR);
    }
}
