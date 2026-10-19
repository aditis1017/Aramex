package com.customcharge.persistencelayer.cache;

import com.customcharge.persistencelayer.cache.dto.MasterCategorySummary;
import com.customcharge.persistencelayer.cache.dto.MasterCategoryValueRow;
import com.customcharge.persistencelayer.cache.dto.MasterCategoryValueRowMapper;
import com.customcharge.persistencelayer.common.CategoryNode;
import com.customcharge.persistencelayer.constant.ApplicationConstant;
import com.customcharge.persistencelayer.entities.CategoryValue;
import com.customcharge.persistencelayer.exception.InternalServerException;
import com.customcharge.persistencelayer.entities.Category;
import com.customcharge.persistencelayer.entities.Country;
import com.customcharge.persistencelayer.entities.Entity;
import com.customcharge.persistencelayer.repository.CategoryRepository;
import com.customcharge.persistencelayer.repository.CategoryValueRepository;
import com.customcharge.persistencelayer.repository.CountryRepository;
import com.customcharge.persistencelayer.repository.EntityRepository;
import com.customcharge.persistencelayer.util.CurrencyCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "mvp.persistence.redis", name = "enabled", havingValue = "true")
public class CategoryCodesGlobalDataCacheService {

    private static final Duration TTL = Duration.ofHours(25);

    private final CacheService cacheService;
    private final CategoryValueRepository categoryValueRepository;
    private final CategoryRepository categoryRepository;
    private final CountryRepository countryRepository;
    private final EntityRepository entityRepository;
    private final MasterCategoryTreeLoader treeLoader;
    private final CurrencyCacheService currencyCacheService;

    public CategoryCodesGlobalDataCacheService(
            CacheService cacheService,
            CategoryValueRepository categoryValueRepository,
            CategoryRepository categoryRepository,
            CountryRepository countryRepository,
            EntityRepository entityRepository,
            MasterCategoryTreeLoader treeLoader,
            CurrencyCacheService currencyCacheService) {
        this.cacheService = cacheService;
        this.categoryValueRepository = categoryValueRepository;
        this.categoryRepository = categoryRepository;
        this.countryRepository = countryRepository;
        this.entityRepository = entityRepository;
        this.treeLoader = treeLoader;
        this.currencyCacheService = currencyCacheService;
    }

    public CategoryValue getCategoryCodeByCode(String code) {
        String key = ApplicationConstant.KEY_PREFIX + code.trim();
        String trimmed = code.trim();
        return readSnapshot(key)
                .map(this::fromSnapshot)
                .or(() -> categoryValueRepository.findByCode(code)
                        .map(data -> {
                            MasterCategoryValueRow snapshot = MasterCategoryValueRowMapper.from(data);
                            if (Boolean.TRUE.equals(data.getIsActive())) {
                                cacheService.put(key, snapshot, TTL);
                            }
                            return fromSnapshot(snapshot);
                        }))
                .or(() -> countryRepository.findByCodeIgnoreCase(trimmed)
                        .filter(c -> !Boolean.FALSE.equals(c.getActive()))
                        .map(c -> putSnapshot(key, MasterCategoryValueRowMapper.fromCountry(c, false))))
                .or(() -> entityRepository.findByCodeIgnoreCase(trimmed)
                        .filter(e -> !Boolean.FALSE.equals(e.getActive()))
                        .map(e -> putSnapshot(key, MasterCategoryValueRowMapper.fromEntityRow(e))))
                .orElseThrow(() -> new InternalServerException(
                        "Category Codes not found for code : " + code
                ));
    }

    /**
     * Loads one category tree from DB and writes Redis keys when missing (no global cache wipe).
     */
    @Transactional(readOnly = true)
    public boolean ensureCategoryTreeCached(String categoryCode) {
        if (categoryCode == null || categoryCode.isBlank()) {
            return false;
        }
        String code = categoryCode.strip();
        MasterCategoryTreeLoader.MasterCategoryIndex index = treeLoader.loadIndex();
        Optional<CategoryNode> tree = treeLoader.buildTree(code, index);
        if (tree.isEmpty()) {
            return false;
        }
        cacheCategoryBundle(code, index);
        cacheService.put(MasterDataCacheKeys.categoryTree(code), tree.get(), TTL);
        cachePathTrees(code, tree.get());
        log.info("Warmed master category cache for categoryCode={}", code);
        return true;
    }

    @Transactional(readOnly = true)
    public void refreshAll() {
        cacheService.deleteCacheUsingPattern(MasterDataCacheKeys.LEGACY_GLOBAL_SETTING_PATTERN);
        cacheService.deleteCacheUsingPattern(MasterDataCacheKeys.CATEGORY_TREE_PATTERN);
        cacheService.deleteCacheUsingPattern(MasterDataCacheKeys.CATEGORY_VALUE_TREE_PATTERN);
        cacheService.deleteCacheUsingPattern(MasterDataCacheKeys.CURRENCY_PATTERN);
        currencyCacheService.setAll();

        MasterCategoryTreeLoader.MasterCategoryIndex index = treeLoader.loadIndex();
        List<CategoryValue> values = categoryValueRepository.findAll().stream()
                .filter(v -> Boolean.TRUE.equals(v.getIsActive()))
                .toList();
        List<Country> countries = countryRepository.findByActiveTrue();
        List<Entity> entities = entityRepository.findAllActiveWithActiveCountry();

        List<MasterCategorySummary> allSummaries = new ArrayList<>(index.summariesByCode().values());
        cacheService.put(MasterDataCacheKeys.categoryAll(), allSummaries, TTL);

        index.summariesByCode().keySet().forEach(code -> cacheCategoryBundle(code, index));

        index.closureByCode().forEach((catCode, rows) -> {
            CategoryNode tree = treeLoader.buildTree(catCode, index).orElse(null);
            if (tree == null) {
                return;
            }
            cacheService.put(MasterDataCacheKeys.categoryTree(catCode), tree, TTL);
            cachePathTrees(catCode, tree);
        });

        Stream.concat(
                        countries.stream().map(x -> MasterCategoryValueRowMapper.fromCountry(x, false)),
                        entities.stream().map(MasterCategoryValueRowMapper::fromEntityRow))
                .filter(r -> r.code() != null && !r.code().isBlank())
                .forEach(r -> cacheService.put(ApplicationConstant.KEY_PREFIX + r.code().trim(), r, TTL));

        for (CategoryValue row : values) {
            if (row == null || row.getCode() == null || row.getCode().isBlank()) {
                continue;
            }
            cacheService.put(ApplicationConstant.KEY_PREFIX + row.getCode().trim(), MasterCategoryValueRowMapper.from(row), TTL);
        }

        log.info("Master category cache refresh complete ({} categories, {} values, {} countries, {} entities)",
                categoryRepository.findAll().stream().filter(c -> Boolean.TRUE.equals(c.getIsActive())).count(),
                values.size(), countries.size(), entities.size());
    }

    private void cacheCategoryBundle(String code, MasterCategoryTreeLoader.MasterCategoryIndex index) {
        MasterCategorySummary summary = index.summariesByCode().get(code);
        if (summary != null) {
            cacheService.put(MasterDataCacheKeys.category(code), summary, TTL);
        }
        List<MasterCategoryValueRow> direct = index.directRowsByCode().getOrDefault(code, List.of());
        cacheService.put(MasterDataCacheKeys.categoryValues(code), direct, TTL);
        List<MasterCategoryValueRow> closure = index.closureByCode().getOrDefault(code, List.of());
        cacheService.put(MasterDataCacheKeys.categoryValuesClosure(code), closure, TTL);
        Map<String, String> loadFrom = index.loadFromByCategoryCode().getOrDefault(code, Map.of());
        if (!loadFrom.isEmpty()) {
            cacheService.put(MasterDataCacheKeys.categoryLoadDataFromMap(code), loadFrom, TTL);
        }
        List<MasterCategoryValueRow> entityRows = index.entityRowsByCode().get(code);
        if (entityRows != null && !entityRows.isEmpty()) {
            cacheService.put(MasterDataCacheKeys.categoryEntityValues(code), entityRows, TTL);
        }
    }

    private CategoryValue putSnapshot(String key, MasterCategoryValueRow row) {
        cacheService.put(key, row, TTL);
        return fromSnapshot(row);
    }

    private Optional<MasterCategoryValueRow> readSnapshot(String key) {
        try {
            return cacheService.get(key, MasterCategoryValueRow.class);
        } catch (IllegalArgumentException ex) {
            log.warn("Evicting unreadable cache entry {}: {}", key, ex.getMessage());
            cacheService.evict(key);
            return Optional.empty();
        } catch (Exception ex) {
            log.warn("Category cache read failed for key {}: {}", key, ex.getMessage());
            return Optional.empty();
        }
    }

    private CategoryValue fromSnapshot(MasterCategoryValueRow row) {
        CategoryValue cv = new CategoryValue();
        cv.setId(row.id());
        cv.setCode(row.code());
        cv.setName(row.name());
        cv.setLabel(row.label());
        cv.setIsActive(row.isActive() != null ? row.isActive() : Boolean.TRUE);
        cv.setLoadDataFrom(row.loadDataFrom());
        cv.setChildren(null);
        cv.setParent(null);
        if (row.categoryId() != null) {
            Category c = new Category();
            c.setId(row.categoryId());
            cv.setCategory(c);
        }
        cv.setEntityCode(row.entityCode());
        return cv;
    }

    private void cachePathTrees(String categoryCode, CategoryNode root) {
        cachePathTrees(categoryCode, root, new ArrayList<>());
    }

    private void cachePathTrees(String categoryCode, CategoryNode node, List<String> path) {
        if (node.getValue() != null && !node.getValue().isBlank() && !path.isEmpty()) {
            cacheService.put(MasterDataCacheKeys.categoryTreePath(categoryCode, path), node, TTL);
        }
        for (CategoryNode child : node.getChildren()) {
            List<String> childPath = new ArrayList<>(path);
            if (child.getValue() != null && !child.getValue().isBlank()) {
                childPath.add(child.getValue());
            }
            cachePathTrees(categoryCode, child, childPath);
        }
    }

    public Optional<CategoryValue> findCategoryByCode(String code) {
        try {
            return Optional.of(getCategoryCodeByCode(code));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }
}
