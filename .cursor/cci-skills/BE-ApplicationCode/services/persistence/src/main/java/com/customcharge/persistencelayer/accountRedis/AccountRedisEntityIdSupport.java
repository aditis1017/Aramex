package com.customcharge.persistencelayer.accountRedis;

import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.UUID;

final class AccountRedisEntityIdSupport {

    private AccountRedisEntityIdSupport() {}

    static void setId(Object entity, UUID id) {
        if (entity == null || id == null) {
            return;
        }
        Field idField = ReflectionUtils.findField(entity.getClass(), "id");
        if (idField == null) {
            return;
        }
        ReflectionUtils.makeAccessible(idField);
        ReflectionUtils.setField(idField, entity, id);
    }
}
