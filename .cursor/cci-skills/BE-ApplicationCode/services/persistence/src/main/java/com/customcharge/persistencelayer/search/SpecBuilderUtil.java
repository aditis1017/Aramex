package com.customcharge.persistencelayer.search;

import com.customcharge.persistencelayer.util.TransactionIdUtil;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.*;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.support.Repositories;
import org.springframework.stereotype.Component;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

@Component
public class SpecBuilderUtil {

    public static final DateTimeFormatter ISO_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                    .withZone(ZoneId.of("UTC"));

    private static final String NO_REPO_MSG = "No Repository Specified";
    private static final String NO_CONTEXT_MSG = "No context provided";
    private static final String INVALID_PAGE_MSG = "Invalid Page Found";

    private ApplicationContext context;
    private JpaSpecificationExecutor<?> executor;
    private Pageable pageable;
    private Sort sort;
    private final List<SearchCriteria> criteriaList = new ArrayList<>();

    public static SpecBuilderUtil getInstance() {
        return new SpecBuilderUtil();
    }

    public SpecBuilderUtil usingContext(ApplicationContext context) {
        this.context = context;
        return this;
    }

    public SpecBuilderUtil addCriteria(SearchCriteria criteria) {
        if (criteria != null) {
            this.criteriaList.add(criteria);
        }
        return this;
    }

    public SpecBuilderUtil addCriteria(List<SearchCriteria> criteriaList) {
        if (criteriaList != null) {
            this.criteriaList.addAll(criteriaList);
        }
        return this;
    }

    public SpecBuilderUtil clearCriteria() {
        this.criteriaList.clear();
        return this;
    }

    public SpecBuilderUtil setupPageDesc(Integer page, Integer size, String... sortBy) {
        this.pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
        return this;
    }

    public SpecBuilderUtil setupPageAsc(Integer page, Integer size, String... sortBy) {
        this.pageable = PageRequest.of(page, size, Sort.by(sortBy).ascending());
        return this;
    }

    public SpecBuilderUtil setupPage(Integer page, Integer size) {
        this.pageable = PageRequest.of(page, size);
        return this;
    }

    public SpecBuilderUtil from(JpaSpecificationExecutor<?> executor) {
        this.executor = executor;
        return this;
    }

    public SpecBuilderUtil sortDesc(String... sortBy) {
        return applySort(Sort.by(sortBy).descending());
    }

    public SpecBuilderUtil sortAsc(String... sortBy) {
        return applySort(Sort.by(sortBy).ascending());
    }

    private SpecBuilderUtil applySort(Sort newSort) {
        if (pageable != null) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    pageable.getSort().and(newSort));
        } else {
            sort = (sort == null) ? newSort : sort.and(newSort);
        }
        return this;
    }

    public SpecBuilderUtil from(Class<?> entityClass) {
        Repositories repos = loadRepositories();
        if (!repos.hasRepositoryFor(entityClass)) {
            throw new IllegalArgumentException("No Repo found for class : " + entityClass.getName());
        }

        JpaSpecificationExecutor<?> repo =
                (JpaSpecificationExecutor<?>) repos.getRepositoryFor(entityClass).orElseThrow();

        return from(repo);
    }

    public SpecBuilderUtil from(String className) {
        try {
            return from(Class.forName(className));
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Class not found : " + className, e);
        }
    }

    public SpecBuilderUtil from(String basePackage, String name) {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(true);

        scanner.addIncludeFilter(new AnnotationTypeFilter(Table.class));

        List<String> classNames = scanner.findCandidateComponents(basePackage).stream()
                .map(BeanDefinition::getBeanClassName)
                .filter(Objects::nonNull)
                .filter(c -> c.toLowerCase().endsWith("." + name.toLowerCase()))
                .distinct()
                .toList();

        if (classNames.isEmpty()) {
            throw new IllegalArgumentException("No Class found with name: " + name);
        }

        return from(classNames.getFirst());
    }

    private Repositories loadRepositories() {
        if (context == null) {
            throw new IllegalArgumentException(NO_CONTEXT_MSG);
        }
        return new Repositories(context);
    }

    private void requireExecutor() {
        if (executor == null) {
            throw new IllegalStateException(NO_REPO_MSG);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> List<T> findAll() {
        requireExecutor();
        Specification spec = getSpecification();

        return (sort != null && !sort.isEmpty())
                ? (executor).findAll(spec, sort)
                : (executor).findAll(spec);
    }

    @SuppressWarnings("rawtypes")
    public long delete() {
        requireExecutor();
        return ((JpaSpecificationExecutor) executor).delete(getSpecification());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> Page<T> findAllWithPage() {
        requireExecutor();
        return ((JpaSpecificationExecutor) executor).findAll(getSpecification(), pageable);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> T findOne(Class<T> outClass) {
        requireExecutor();
        Optional<?> response = ((JpaSpecificationExecutor) executor).findOne(getSpecification());
        return response.map(obj -> TransactionIdUtil.convertObject(obj, outClass)).orElse(null);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> Page<T> findPage(Class<T> outClass) {
        if (pageable == null) {
            throw new IllegalArgumentException(INVALID_PAGE_MSG);
        }
        requireExecutor();
        Page<?> pageResult =
                ((JpaSpecificationExecutor) executor)
                        .findAll(getSpecification(), pageable);
        return pageResult.map(obj ->
                TransactionIdUtil.convertObject(obj, outClass)
        );
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> Page<T> findPageEntities(Class<T> ignoredEntityType) {
        if (pageable == null) {
            throw new IllegalArgumentException(INVALID_PAGE_MSG);
        }
        requireExecutor();
        return ((JpaSpecificationExecutor) executor).findAll(getSpecification(), pageable);
    }

    public Specification<Object> getSpecification() {
        return (root, query, cb) -> {
            if (criteriaList.isEmpty()) {
                return cb.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();
            for (SearchCriteria sc : criteriaList) {
                Predicate p = buildPredicate(root, cb, sc);
                if (p != null) {
                    predicates.add(p);
                }
            }

            return predicates.isEmpty()
                    ? cb.conjunction()
                    : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static <T> Path<?> resolvePath(Root<T> root, String fieldName) {
        if (fieldName == null || fieldName.isBlank()) {
            throw new IllegalArgumentException("SearchCriteria fieldName must not be blank");
        }
        String[] segments = Stream.of(fieldName.split("\\."))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        if (segments.length == 0) {
            throw new IllegalArgumentException("Invalid fieldName: " + fieldName);
        }
        Path<?> path = root.get(segments[0]);
        for (int i = 1; i < segments.length; i++) {
            path = path.get(segments[i]);
        }
        return path;
    }

    private static <T> Predicate buildPredicate(Root<T> root, CriteriaBuilder cb, SearchCriteria sc) {

        if (sc == null || sc.getFieldName() == null || sc.getCondition() == null) {
            return null;
        }

        Path<?> path = resolvePath(root, sc.getFieldName());
        Object value = sc.getValue();

        return switch (sc.getCondition().toLowerCase()) {
            case "equals" -> cb.equal(path, value);
            case "equalsornull" -> cb.or(cb.isNull(path), cb.equal(path, value));
            case "notequals" -> cb.or(cb.isNull(path), cb.notEqual(path, value));
            case "contains" -> value == null ? null : cb.like(path.as(String.class), "%" + value + "%");
            case "datelessthanequal" -> value == null ? null :
                    cb.lessThanOrEqualTo(path.as(LocalDateTime.class),
                            parse(value).toLocalDate().atTime(LocalTime.MAX));
            case "dategreaterthanequal" -> value == null ? null :
                    cb.greaterThanOrEqualTo(path.as(LocalDateTime.class),
                            parse(value).toLocalDate().atStartOfDay());
            case "betweenrange" -> (value == null || sc.getSecondValue() == null) ? null :
                    cb.between(path.as(Double.class),
                            Double.parseDouble(value.toString()),
                            Double.parseDouble(sc.getSecondValue().toString()));
            case "betweendate" -> (value == null || sc.getSecondValue() == null) ? null :
                    cb.and(
                            cb.greaterThanOrEqualTo(path.as(LocalDateTime.class),
                                    parseLocalDate(value).atStartOfDay()),
                            cb.lessThan(path.as(LocalDateTime.class),
                                    parseLocalDate(sc.getSecondValue()).atStartOfDay()));
            case "betweenandequaldate" -> (value == null || sc.getSecondValue() == null) ? null :
                    cb.and(
                            cb.greaterThanOrEqualTo(path.as(LocalDateTime.class),
                                    parseLocalDate(value).atStartOfDay()),
                            cb.lessThanOrEqualTo(path.as(LocalDateTime.class),
                                    parseLocalDate(sc.getSecondValue()).atTime(LocalTime.MAX)));
            case "in" -> buildIn(path, value, cb, false);
            case "notin" -> buildIn(path, value, cb, true);
            default -> null;
        };
    }

    private static Predicate buildIn(Path<?> path, Object value, CriteriaBuilder cb,
                                     boolean negate) {

        List<?> list = (value instanceof Collection)
                ? new ArrayList<>((Collection<?>) value)
                : Collections.singletonList(value);

        Predicate predicate = path.in(list);
        return negate ? cb.not(predicate) : predicate;
    }

    private static LocalDateTime parse(Object value) {
        try {
            return LocalDateTime.parse(value.toString());
        } catch (Exception e) {
            return LocalDateTime.parse(value.toString(), ISO_DATE_FORMATTER);
        }
    }

    private static LocalDate parseLocalDate(Object value) {
        String text = value.toString().trim();
        if (text.length() >= 10) {
            try {
                return LocalDate.parse(text.substring(0, 10));
            } catch (Exception ignored) {
            }
        }
        return parse(value).toLocalDate();
    }
}