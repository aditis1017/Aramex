package com.customcharge.dashboard.search;

import com.customcharge.dashboard.constants.EntityChargeFilterConstants;
import com.customcharge.dashboard.constants.ValidationConstants;
import com.customcharge.persistencelayer.exception.BadRequestException;
import com.customcharge.persistencelayer.repository.EntityRepository;
import com.customcharge.persistencelayer.search.SearchCriteria;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Resolves dashboard {@code entity} filter codes to {@code entity_id} values for charge-first pagination.
 */
@Component
public class EntityChargeEntityFilterResolver {

    private final EntityRepository entityRepository;

    public EntityChargeEntityFilterResolver(EntityRepository entityRepository) {
        this.entityRepository = entityRepository;
    }

    public record EntityFilterResolution(
            List<Long> entityIds,
            boolean entityFilterApplied,
            List<SearchCriteria> criteriaWithoutEntity) {
    }

    /**
     * Legacy helper: present only when the request includes an {@code entity} filter.
     */
    public Optional<EntityFilterResolution> resolve(List<SearchCriteria> criteria) {
        EntityFilterResolution scope = resolveScope(criteria);
        return scope.entityFilterApplied() ? Optional.of(scope) : Optional.empty();
    }

    /**
     * Splits entity codes from charge-table criteria for charge-first pagination.
     */
    public EntityFilterResolution resolveScope(List<SearchCriteria> criteria) {
        if (criteria == null || criteria.isEmpty()) {
            return new EntityFilterResolution(List.of(), false, List.of());
        }
        List<String> entityCodes = new ArrayList<>();
        List<SearchCriteria> withoutEntity = new ArrayList<>();
        for (SearchCriteria filter : criteria) {
            if (filter == null) {
                continue;
            }
            if (EntityChargeChargeFieldPaths.isEntityField(filter.getFieldName())) {
                entityCodes.addAll(extractEntityCodes(filter));
            } else {
                withoutEntity.add(filter);
            }
        }
        if (entityCodes.isEmpty()) {
            return new EntityFilterResolution(List.of(), false, withoutEntity);
        }
        List<String> normalizedCodes = entityCodes.stream()
                .filter(Objects::nonNull)
                .map(code -> code.trim().toUpperCase(Locale.ROOT))
                .filter(code -> !code.isEmpty())
                .distinct()
                .toList();
        if (normalizedCodes.isEmpty()) {
            return new EntityFilterResolution(List.of(), true, withoutEntity);
        }
        List<Long> entityIds = entityRepository.findIdsByCodeInIgnoreCase(normalizedCodes);
        return new EntityFilterResolution(entityIds, true, withoutEntity);
    }

    private static List<String> extractEntityCodes(SearchCriteria filter) {
        String condition = normalizeCondition(filter.getCondition());
        if (!EntityChargeFilterConstants.FILTER_CONDITION_IN.equals(condition)) {
            throw new BadRequestException(ValidationConstants.UNKNOWN_FILTER_FIELD_PREFIX + filter.getFieldName());
        }
        return toStringList(filter.getValue());
    }

    private static String normalizeCondition(String condition) {
        if (condition == null || condition.isBlank()) {
            throw new BadRequestException(ValidationConstants.FILTER_CONDITION_BLANK);
        }
        return condition.trim().toLowerCase(Locale.ROOT);
    }

    private static List<String> toStringList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .map(Object::toString)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }
        String text = value.toString().trim();
        return text.isEmpty() ? List.of() : List.of(text);
    }
}
