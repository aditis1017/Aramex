package com.customcharge.dashboard.search;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/**
 * Runs view-based entity-charge queries with optional JPA timeout hints.
 */
public final class EntityChargeSearchQuerySupport {

    private static final String QUERY_TIMEOUT_HINT = "jakarta.persistence.query.timeout";

    private EntityChargeSearchQuerySupport() {
    }

    public static <T> long count(
            EntityManager entityManager,
            Class<T> domainClass,
            Specification<T> specification,
            int queryTimeoutMs) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = criteriaBuilder.createQuery(Long.class);
        Root<T> root = countQuery.from(domainClass);
        countQuery.select(criteriaBuilder.count(root));
        Predicate predicate = toPredicate(specification, root, countQuery, criteriaBuilder);
        if (predicate != null) {
            countQuery.where(predicate);
        }
        TypedQuery<Long> typedQuery = entityManager.createQuery(countQuery);
        applyTimeout(typedQuery, queryTimeoutMs);
        return typedQuery.getSingleResult();
    }

    public static <T> Page<T> findAll(
            EntityManager entityManager,
            Class<T> domainClass,
            Specification<T> specification,
            Pageable pageable,
            int queryTimeoutMs) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> dataQuery = criteriaBuilder.createQuery(domainClass);
        Root<T> root = dataQuery.from(domainClass);
        dataQuery.select(root);
        Predicate predicate = toPredicate(specification, root, dataQuery, criteriaBuilder);
        if (predicate != null) {
            dataQuery.where(predicate);
        }
        applySort(dataQuery, root, criteriaBuilder, pageable.getSort());

        TypedQuery<T> typedQuery = entityManager.createQuery(dataQuery);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());
        applyTimeout(typedQuery, queryTimeoutMs);
        List<T> content = typedQuery.getResultList();

        long total = count(entityManager, domainClass, specification, queryTimeoutMs);
        return new PageImpl<>(content, pageable, total);
    }

    private static <T> Predicate toPredicate(
            Specification<T> specification,
            Root<T> root,
            CriteriaQuery<?> query,
            CriteriaBuilder criteriaBuilder) {
        if (specification == null) {
            return null;
        }
        return specification.toPredicate(root, query, criteriaBuilder);
    }

    private static <T> void applySort(
            CriteriaQuery<T> query, Root<T> root, CriteriaBuilder criteriaBuilder, Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return;
        }
        List<jakarta.persistence.criteria.Order> orders = sort.stream()
                .map(order -> order.isAscending()
                        ? criteriaBuilder.asc(root.get(order.getProperty()))
                        : criteriaBuilder.desc(root.get(order.getProperty())))
                .toList();
        query.orderBy(orders);
    }

    private static void applyTimeout(TypedQuery<?> query, int queryTimeoutMs) {
        if (queryTimeoutMs > 0) {
            query.setHint(QUERY_TIMEOUT_HINT, queryTimeoutMs);
        }
    }
}
