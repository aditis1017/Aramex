package com.customcharge.persistencelayer.cache;

import com.customcharge.persistencelayer.cache.dto.MasterCategorySummary;
import com.customcharge.persistencelayer.cache.dto.MasterCategoryValueRow;
import com.customcharge.persistencelayer.cache.dto.MasterCategoryValueRowMapper;
import com.customcharge.persistencelayer.common.CategoryNode;
import com.customcharge.persistencelayer.entities.Category;
import com.customcharge.persistencelayer.entities.CategoryValue;
import com.customcharge.persistencelayer.entities.Country;
import com.customcharge.persistencelayer.entities.Entity;
import com.customcharge.persistencelayer.repository.CategoryRepository;
import com.customcharge.persistencelayer.repository.CategoryValueRepository;
import com.customcharge.persistencelayer.repository.CountryRepository;
import com.customcharge.persistencelayer.repository.EntityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class MasterCategoryTreeLoader {

    private final CategoryValueRepository categoryValueRepository;
    private final CategoryRepository categoryRepository;
    private final CountryRepository countryRepository;
    private final EntityRepository entityRepository;

    public MasterCategoryTreeLoader(
            CategoryValueRepository categoryValueRepository,
            CategoryRepository categoryRepository,
            CountryRepository countryRepository,
            EntityRepository entityRepository) {
        this.categoryValueRepository = categoryValueRepository;
        this.categoryRepository = categoryRepository;
        this.countryRepository = countryRepository;
        this.entityRepository = entityRepository;
    }

    public record MasterCategoryIndex(
            Map<String, List<MasterCategoryValueRow>> closureByCode,
            Map<String, Map<String, String>> loadFromByCategoryCode,
            Map<String, MasterCategorySummary> summariesByCode,
            Map<String, List<MasterCategoryValueRow>> directRowsByCode,
            Map<String, List<MasterCategoryValueRow>> entityRowsByCode) {
    }

    @Transactional(readOnly = true)
    public MasterCategoryIndex loadIndex() {
        List<Category> categories = categoryRepository.findAll().stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
                .toList();
        List<CategoryValue> values = categoryValueRepository.findAll().stream()
                .filter(v -> Boolean.TRUE.equals(v.getIsActive()))
                .toList();

        Map<String, List<CategoryValue>> byCategoryCode = values.stream()
                .filter(v -> v.getCategory() != null && v.getCategory().getCode() != null)
                .collect(Collectors.groupingBy(v -> v.getCategory().getCode()));

        List<MasterCategoryValueRow> globalRows = values.stream().map(MasterCategoryValueRowMapper::from).toList();
        Map<Long, List<MasterCategoryValueRow>> childrenByParentId = globalRows.stream()
                .filter(r -> r.parentId() != null)
                .collect(Collectors.groupingBy(MasterCategoryValueRow::parentId));

        Map<String, List<MasterCategoryValueRow>> closureByCode = new HashMap<>();
        Map<String, Map<String, String>> loadFromByCategoryCode = new HashMap<>();
        Map<String, MasterCategorySummary> summariesByCode = new HashMap<>();
        Map<String, List<MasterCategoryValueRow>> directRowsByCode = new HashMap<>();
        Map<String, List<MasterCategoryValueRow>> entityRowsByCode = new HashMap<>();

        List<Country> countries = countryRepository.findByActiveTrue();
        List<Entity> entities = entityRepository.findAllActiveWithActiveCountry();
        String countryCode = MasterDataCacheKeys.SYNTHETIC_COUNTRY_CATEGORY_CODE;
        String entityCode = MasterDataCacheKeys.SYNTHETIC_ENTITY_CATEGORY_CODE;
        List<MasterCategoryValueRow> countryRows =
                countries.stream().map(x -> MasterCategoryValueRowMapper.fromCountry(x, false)).toList();
        summariesByCode.put(countryCode, new MasterCategorySummary(null, countryCode, countryCode, true, false));
        directRowsByCode.put(countryCode, countryRows);
        closureByCode.put(countryCode, countryRows);
        loadFromByCategoryCode.put(countryCode, Map.of());

        List<MasterCategoryValueRow> entityClosure = Stream.concat(
                countries.stream().map(x -> MasterCategoryValueRowMapper.fromCountry(x, true)),
                entities.stream().map(MasterCategoryValueRowMapper::fromEntityRow)).toList();
        summariesByCode.put(entityCode, new MasterCategorySummary(null, entityCode, entityCode, true, true));
        directRowsByCode.put(entityCode, countryRows);
        closureByCode.put(entityCode, entityClosure);
        loadFromByCategoryCode.put(entityCode, Map.of());

        for (Category cat : categories) {
            String catCode = cat.getCode();
            List<CategoryValue> catVals = byCategoryCode.getOrDefault(catCode, List.of());
            boolean entityScoped = catVals.stream().anyMatch(MasterCategoryTreeLoader::hasEntityCode);
            summariesByCode.put(catCode, new MasterCategorySummary(
                    cat.getId(), cat.getCode(), cat.getName(), cat.getIsActive(), entityScoped));

            List<MasterCategoryValueRow> directRows = catVals.stream().map(MasterCategoryValueRowMapper::from).toList();
            directRowsByCode.put(catCode, directRows);
            closureByCode.put(catCode, closureValueRowsForCategory(catCode, byCategoryCode, childrenByParentId));

            Map<String, String> loadFromByCode = catVals.stream()
                    .filter(v -> v.getCode() != null && !v.getCode().isBlank())
                    .filter(v -> v.getLoadDataFrom() != null && !v.getLoadDataFrom().isBlank())
                    .collect(Collectors.toMap(
                            CategoryValue::getCode,
                            v -> v.getLoadDataFrom().trim(),
                            (a, b) -> a));
            loadFromByCategoryCode.put(catCode, loadFromByCode);

            List<MasterCategoryValueRow> entityRows = catVals.stream()
                    .filter(MasterCategoryTreeLoader::hasEntityCode)
                    .map(MasterCategoryValueRowMapper::from)
                    .toList();
            if (!entityRows.isEmpty()) {
                entityRowsByCode.put(catCode, entityRows);
            }
        }

        return new MasterCategoryIndex(
                closureByCode, loadFromByCategoryCode, summariesByCode, directRowsByCode, entityRowsByCode);
    }

    public Optional<CategoryNode> buildTree(String categoryCode, MasterCategoryIndex index) {
        if (categoryCode == null || categoryCode.isBlank() || index == null) {
            return Optional.empty();
        }
        String code = categoryCode.strip();
        List<MasterCategoryValueRow> closure = index.closureByCode().get(code);
        if (closure == null || closure.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(buildTree(
                code,
                closure,
                index.loadFromByCategoryCode().getOrDefault(code, Map.of()),
                index.closureByCode()));
    }

    private static boolean hasEntityCode(CategoryValue value) {
        return value.getEntityCode() != null && !value.getEntityCode().isBlank();
    }

    private static List<MasterCategoryValueRow> closureValueRowsForCategory(
            String catCode,
            Map<String, List<CategoryValue>> byCategoryCode,
            Map<Long, List<MasterCategoryValueRow>> childrenByParentId) {
        List<CategoryValue> seeds = byCategoryCode.getOrDefault(catCode, List.of());
        if (seeds.isEmpty()) {
            return List.of();
        }
        List<MasterCategoryValueRow> out = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        ArrayDeque<Long> queue = new ArrayDeque<>();
        for (CategoryValue seed : seeds) {
            MasterCategoryValueRow row = MasterCategoryValueRowMapper.from(seed);
            if (row.id() != null && seen.add(row.id())) {
                out.add(row);
                queue.add(row.id());
            }
        }
        while (!queue.isEmpty()) {
            Long parentId = queue.removeFirst();
            for (MasterCategoryValueRow child : childrenByParentId.getOrDefault(parentId, List.of())) {
                if (child.id() != null && seen.add(child.id())) {
                    out.add(child);
                    queue.add(child.id());
                }
            }
        }
        return out;
    }

    private CategoryNode buildTree(
            String categoryCode,
            List<MasterCategoryValueRow> rows,
            Map<String, String> loadFromByCode,
            Map<String, List<MasterCategoryValueRow>> closureByCode) {
        CategoryNode root = new CategoryNode();
        root.setLabel(categoryCode);
        root.setValue(categoryCode);
        root.setActive(Boolean.TRUE);
        Map<Long, List<MasterCategoryValueRow>> byParent = rows.stream()
                .filter(r -> r.parentId() != null)
                .collect(Collectors.groupingBy(MasterCategoryValueRow::parentId));
        List<MasterCategoryValueRow> roots = rows.stream().filter(r -> r.parentId() == null).toList();
        for (MasterCategoryValueRow row : roots) {
            CategoryNode child = toNode(row);
            root.getChildren().add(child);
            fillNode(child, row, byParent, loadFromByCode, closureByCode);
        }
        return root;
    }

    private void fillNode(
            CategoryNode node,
            MasterCategoryValueRow row,
            Map<Long, List<MasterCategoryValueRow>> byParent,
            Map<String, String> loadFromByCode,
            Map<String, List<MasterCategoryValueRow>> closureByCode) {
        List<MasterCategoryValueRow> kids = byParent.getOrDefault(row.id(), List.of());
        for (MasterCategoryValueRow kid : kids) {
            CategoryNode child = toNode(kid);
            node.getChildren().add(child);
            fillNode(child, kid, byParent, loadFromByCode, closureByCode);
        }
        String loadFrom = row.loadDataFrom();
        if ((loadFrom == null || loadFrom.isBlank()) && row.code() != null) {
            loadFrom = loadFromByCode.get(row.code());
        }
        if (loadFrom == null || loadFrom.isBlank()) {
            return;
        }
        String normalized = MasterDataCacheKeys.syntheticCategoryCode(loadFrom.trim()).orElse(loadFrom.trim());
        List<MasterCategoryValueRow> delegated = closureByCode.getOrDefault(normalized, List.of());
        if (delegated.isEmpty()) {
            return;
        }
        node.getChildren().clear();
        node.getChildren().addAll(buildTree(normalized, delegated, Map.of(), closureByCode).getChildren());
    }

    private static CategoryNode toNode(MasterCategoryValueRow row) {
        CategoryNode node = new CategoryNode();
        node.setId(row.id());
        node.setLabel(row.label());
        node.setValue(row.code());
        node.setActive(row.isActive() != null ? row.isActive() : Boolean.TRUE);
        return node;
    }
}
