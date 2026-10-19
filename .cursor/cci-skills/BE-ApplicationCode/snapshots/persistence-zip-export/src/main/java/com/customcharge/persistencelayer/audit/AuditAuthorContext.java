package com.customcharge.persistencelayer.audit;

import java.util.Optional;


public final class AuditAuthorContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private AuditAuthorContext() {
    }

    public static void set(String author) {
        if (author == null || author.isBlank()) {
            clear();
            return;
        }
        CURRENT.set(author.trim());
    }

    public static Optional<String> current() {
        String value = CURRENT.get();
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value);
    }

    public static void clear() {
        CURRENT.remove();
    }
}
