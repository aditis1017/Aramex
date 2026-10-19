package com.customcharge.persistencelayer.util;

import jakarta.annotation.Nonnull;

public final class RedisKeyUtil {

    private static final String SEPARATOR = ":";

    private RedisKeyUtil() {}

    public static String key(String namespace, String entity, Object id) {

        if (namespace == null || namespace.isBlank()) {
            throw new IllegalArgumentException("namespace must not be blank");
        }

        if (entity == null || entity.isBlank()) {
            throw new IllegalArgumentException("entity must not be blank");
        }

        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }

        return getString(namespace, entity, id);
    }

    @Nonnull
    private static String getString(String namespace, String entity, Object id) {
        return namespace + SEPARATOR + entity + SEPARATOR + id;
    }
}