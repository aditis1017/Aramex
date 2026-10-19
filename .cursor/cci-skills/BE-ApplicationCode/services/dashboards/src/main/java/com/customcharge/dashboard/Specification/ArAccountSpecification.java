package com.customcharge.dashboard.Specification;

import com.customcharge.dashboard.constants.AccountConstants;
import com.customcharge.dashboard.constants.ValidationConstants;
import com.customcharge.dashboard.dto.AccountSearchRequestDTO;
import com.customcharge.persistencelayer.entities.ARAccount;
import com.customcharge.persistencelayer.exception.BadRequestException;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Search criteria for AR master rows ({@link ARAccount}), which may exist without linked
 * {@code Account} charge-config rows.
 */
public final class ArAccountSpecification {

    private ArAccountSpecification() {}

    public static Specification<ARAccount> search(AccountSearchRequestDTO req, String resolvedEntityCode) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (Objects.nonNull(req.getSearchField())
                    && Objects.nonNull(req.getSearchText())
                    && !req.getSearchText().isBlank()) {
                Expression<String> field = resolveSearchField(root, req.getSearchField());
                String trimmedText = req.getSearchText().trim();
                String rawSearchType = req.getSearchType();
                if (rawSearchType == null || rawSearchType.isBlank()) {
                    throw new BadRequestException(ValidationConstants.SEARCH_TYPE_REQUIRED);
                }
                String searchType = rawSearchType.trim().toUpperCase(Locale.ROOT);

                switch (searchType) {
                    case AccountConstants.SEARCH_TYPE_CONTAINS -> predicates.add(cb.like(
                            cb.lower(field),
                            "%" + trimmedText.toLowerCase(Locale.ROOT) + "%"));
                    case AccountConstants.SEARCH_TYPE_EXACT_MATCH ->
                            predicates.add(cb.equal(
                                    cb.lower(field),
                                    trimmedText.toLowerCase(Locale.ROOT)));
                    default -> throw new BadRequestException(
                            ValidationConstants.INVALID_SEARCH_TYPE_PREFIX + rawSearchType);
                }
            }

            if (resolvedEntityCode != null && !resolvedEntityCode.isBlank()) {
                predicates.add(cb.equal(
                        cb.lower(root.get("entityCode")),
                        resolvedEntityCode.trim().toLowerCase(Locale.ROOT)));
            }

            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static Expression<String> resolveSearchField(Root<ARAccount> root, String searchField) {
        return switch (searchField) {
            case AccountConstants.SEARCH_FIELD_ACCOUNT_NUMBER -> root.get("accountNumber");
            case AccountConstants.SEARCH_FIELD_ACCOUNT_NAME -> root.get("accountName");
            default -> throw new BadRequestException(
                    ValidationConstants.INVALID_SEARCH_FIELD_PREFIX + searchField);
        };
    }
}
