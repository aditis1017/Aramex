package com.customcharge.persistencelayer.config.javers;

import org.javers.repository.jql.QueryBuilder;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class AuditGroupCommitPropertyRegistry {

    private static final String ENTITIES_BASE_PACKAGE = "com.customcharge.persistencelayer.entities";

    private final Map<String, Map<String, AuditValueNormalize>> commitPropertyKeysByGroup;

    public AuditGroupCommitPropertyRegistry() {
        this.commitPropertyKeysByGroup = loadFromAuditableEntities();
    }

    public QueryBuilder applyCommitPropertyFilters(
            QueryBuilder builder,
            String groupName,
            Map<String, String> requestFilters) {
        Map<String, AuditValueNormalize> groupKeys =
                commitPropertyKeysByGroup.get(normalizeGroupName(groupName));
        if (groupKeys == null || groupKeys.isEmpty()) {
            return builder;
        }

        Map<String, String> filters = requestFilters == null ? Map.of() : requestFilters;
        QueryBuilder filtered = builder;
        for (Map.Entry<String, AuditValueNormalize> definition : groupKeys.entrySet()) {
            String rawValue = filters.get(definition.getKey());
            if (rawValue == null || rawValue.isBlank()) {
                continue;
            }
            filtered = filtered.withCommitProperty(
                    definition.getKey(),
                    definition.getValue().apply(rawValue.trim()));
        }
        return filtered;
    }

    private Map<String, Map<String, AuditValueNormalize>> loadFromAuditableEntities() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(AuditableEntity.class));

        Map<String, Map<String, AuditValueNormalize>> groups = new LinkedHashMap<>();
        scanner.findCandidateComponents(ENTITIES_BASE_PACKAGE).forEach(beanDefinition -> {
            try {
                Class<?> entityClass = Class.forName(beanDefinition.getBeanClassName());
                AuditableEntity annotation = entityClass.getAnnotation(AuditableEntity.class);
                if (annotation == null) {
                    return;
                }
                String groupName = normalizeGroupName(annotation.groupName());
                Map<String, AuditValueNormalize> keys =
                        groups.computeIfAbsent(groupName, ignored -> new LinkedHashMap<>());
                for (AuditCommitProperty property : annotation.commitProperties()) {
                    if (property.key() == null || property.key().isBlank()) {
                        continue;
                    }
                    keys.putIfAbsent(property.key(), property.normalize());
                }
            } catch (ClassNotFoundException ex) {
                throw new IllegalStateException(
                        "Failed to load auditable entity: " + beanDefinition.getBeanClassName(), ex);
            }
        });

        return groups.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> Map.copyOf(entry.getValue())));
    }

    private static String normalizeGroupName(String groupName) {
        return groupName == null ? null : groupName.trim().toUpperCase(Locale.ROOT);
    }
}
