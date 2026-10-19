package com.customcharge.persistencelayer.audit.service;

public interface AuditFieldValueResolver {
    default String resolveOldValue(String fieldName, String rawValue) {
        return rawValue;
    }

    default String resolveNewValue(String fieldName, String rawValue) {
        return rawValue;
    }
}
