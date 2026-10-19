package com.customcharge.persistencelayer.entity;

import com.customcharge.persistencelayer.audit.AuditAuthorResolver;
import org.springframework.data.domain.AuditorAware;
import org.springframework.lang.NonNull;

import java.util.Optional;

public class AuditorProvider implements AuditorAware<String> {

    @Override
    @NonNull
    public Optional<String> getCurrentAuditor() {
        return Optional.of(AuditAuthorResolver.resolve());
    }
}
