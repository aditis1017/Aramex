package com.customcharge.persistencelayer.common;

import com.customcharge.persistencelayer.cache.CategoryCodesGlobalDataCacheService;
import com.customcharge.persistencelayer.cache.CacheService;
import com.customcharge.persistencelayer.cache.MasterCategoryTreeLoader;
import com.customcharge.persistencelayer.cache.MasterDataCacheKeys;
import com.customcharge.persistencelayer.entities.Category;
import com.customcharge.persistencelayer.exception.BadRequestException;
import com.customcharge.persistencelayer.exception.ResourceNotFoundException;
import com.customcharge.persistencelayer.repository.CategoryRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ObjectProvider<CacheService> cacheServiceProvider;
    private final ObjectProvider<CategoryCodesGlobalDataCacheService> categoryCacheServiceProvider;
    private final MasterCategoryTreeLoader treeLoader;

    public CategoryService(
            CategoryRepository categoryRepository,
            ObjectProvider<CacheService> cacheServiceProvider,
            ObjectProvider<CategoryCodesGlobalDataCacheService> categoryCacheServiceProvider,
            MasterCategoryTreeLoader treeLoader) {
        this.categoryRepository = categoryRepository;
        this.cacheServiceProvider = cacheServiceProvider;
        this.categoryCacheServiceProvider = categoryCacheServiceProvider;
        this.treeLoader = treeLoader;
    }

    @Transactional(readOnly = true)
    public List<MasterDropdownResponse> loadMasterData(List<HierarchyRequest> requests) {
        if (requests == null) {
            throw new BadRequestException("Request body is required");
        }
        return requests.stream().map(this::loadOne).toList();
    }

    private MasterDropdownResponse loadOne(HierarchyRequest request) {
        if (request == null || request.getCategoryName() == null || request.getCategoryName().isBlank()) {
            throw new BadRequestException("categoryName is required for each item");
        }
        String requestedName = request.getCategoryName().trim();
        String categoryCode = MasterDataCacheKeys.syntheticCategoryCode(requestedName)
                .orElseGet(() -> resolveCategory(requestedName).getCode());
        List<String> path = normalizedPath(request.getPath());
        CategoryNode tree = path.isEmpty()
                ? readTree(categoryCode)
                : readTreeByPath(categoryCode, path);
        if (tree == null) {
            throw new ResourceNotFoundException("Category tree not found: " + requestedName);
        }
        if (path.isEmpty()) {
            tree.setLabel(requestedName);
            tree.setValue(requestedName);
        }
        return new MasterDropdownResponse(requestedName, null, tree);
    }

    private Category resolveCategory(String categoryName) {
        return categoryRepository.findByNameIgnoreCase(categoryName)
                .or(() -> categoryRepository.findByCodeIgnoreCase(categoryName))
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryName));
    }

    private CacheService cache() {
        CacheService cache = cacheServiceProvider.getIfAvailable();
        if (cache == null) {
            return null;
        }
        return cache;
    }

    private CategoryNode readTree(String categoryCode) {
        CategoryNode fromCache = readTreeFromCache(categoryCode, List.of());
        if (fromCache != null) {
            return fromCache;
        }
        return loadTreeWithFallback(categoryCode, List.of());
    }

    private CategoryNode readTreeByPath(String categoryCode, List<String> path) {
        CategoryNode fromCache = readTreeFromCache(categoryCode, path);
        if (fromCache != null) {
            return fromCache;
        }
        return loadTreeWithFallback(categoryCode, path);
    }

    private CategoryNode readTreeFromCache(String categoryCode, List<String> path) {
        CacheService cache = cache();
        if (cache == null) {
            return null;
        }
        String key = path.isEmpty()
                ? MasterDataCacheKeys.categoryTree(categoryCode)
                : MasterDataCacheKeys.categoryTreePath(categoryCode, path);
        return cache.get(key, CategoryNode.class).orElse(null);
    }

    private CategoryNode loadTreeWithFallback(String categoryCode, List<String> path) {
        warmCacheIfAvailable(categoryCode);
        CategoryNode warmed = readTreeFromCache(categoryCode, path);
        if (warmed != null) {
            return warmed;
        }
        MasterCategoryTreeLoader.MasterCategoryIndex index = treeLoader.loadIndex();
        CategoryNode tree = treeLoader.buildTree(categoryCode, index).orElse(null);
        if (tree == null) {
            return null;
        }
        if (path.isEmpty()) {
            return tree;
        }
        return findNodeByPath(tree, path);
    }

    private void warmCacheIfAvailable(String categoryCode) {
        CategoryCodesGlobalDataCacheService cacheService = categoryCacheServiceProvider.getIfAvailable();
        if (cacheService != null) {
            cacheService.ensureCategoryTreeCached(categoryCode);
        }
    }

    private static CategoryNode findNodeByPath(CategoryNode root, List<String> path) {
        CategoryNode current = root;
        for (String segment : path) {
            if (current.getChildren() == null || current.getChildren().isEmpty()) {
                return null;
            }
            String normalizedSegment = segment == null ? "" : segment.strip().toLowerCase();
            CategoryNode next = null;
            for (CategoryNode child : current.getChildren()) {
                String childValue = child.getValue() == null ? "" : child.getValue().strip().toLowerCase();
                if (childValue.equals(normalizedSegment)) {
                    next = child;
                    break;
                }
            }
            if (next == null) {
                return null;
            }
            current = next;
        }
        return current;
    }
    private List<String> normalizedPath(List<String> path) {
        if (path == null || path.isEmpty()) {
            return List.of();
        }
        return path.stream()
                .map(s -> s == null ? "" : s.strip())
                .filter(s -> !s.isBlank())
                .toList();
    }
}
