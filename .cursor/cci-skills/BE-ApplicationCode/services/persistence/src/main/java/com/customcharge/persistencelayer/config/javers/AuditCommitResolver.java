package com.customcharge.persistencelayer.config.javers;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
public class AuditCommitResolver {

    public Optional<Map<String, String>> resolve(Object entity, AuditableEntity annotation) {
        Map<String, String> properties = new HashMap<>();
        properties.put(AuditCommitProperties.GROUP_NAME, annotation.groupName());

        for (AuditCommitProperty definition : annotation.commitProperties()) {
            Optional<String> value = resolvePath(entity, definition);
            if (value.isEmpty()) {
                if (definition.required()) {
                    log.warn(
                            "Skipping Javers commit for {}: required property {} not resolved from path {}",
                            entity.getClass().getSimpleName(),
                            definition.key(),
                            definition.path());
                    return Optional.empty();
                }
                continue;
            }
            properties.put(definition.key(), value.get());
        }
        return Optional.of(properties);
    }

    private Optional<String> resolvePath(Object entity, AuditCommitProperty definition) {
        if (entity == null || definition.path() == null || definition.path().isBlank()) {
            return Optional.empty();
        }
        Object value = readPath(entity, definition.path().trim());
        return toStringValue(value).map(raw -> definition.normalize().apply(raw));
    }

    private Object readPath(Object root, String path) {
        Object current = root;
        for (String segment : path.split("\\.")) {
            if (current == null) {
                return null;
            }
            Hibernate.initialize(current);
            BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(current);
            if (!wrapper.isReadableProperty(segment)) {
                return null;
            }
            current = wrapper.getPropertyValue(segment);
        }
        if (current != null) {
            Hibernate.initialize(current);
        }
        return current;
    }

    private Optional<String> toStringValue(Object value) {
        if (value == null) {
            return Optional.empty();
        }
        if (value instanceof String s) {
            return s.isBlank() ? Optional.empty() : Optional.of(s);
        }
        if (value instanceof UUID uuid) {
            return Optional.of(uuid.toString());
        }
        if (value instanceof Enum<?> enumValue) {
            return Optional.of(enumValue.name());
        }
        return Optional.of(String.valueOf(value));
    }
}
