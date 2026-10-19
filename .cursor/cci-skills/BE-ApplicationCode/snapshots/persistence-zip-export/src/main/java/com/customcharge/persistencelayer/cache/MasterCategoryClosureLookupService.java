package com.customcharge.persistencelayer.cache;

import com.customcharge.persistencelayer.cache.dto.MasterCategoryValueRow;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Resolves master category closure lists (candidate codes for rules): Redis first, then DB tree index.
 */
@Service
public class MasterCategoryClosureLookupService {

    private final ObjectProvider<CacheService> cacheServiceProvider;
    private final MasterCategoryTreeLoader treeLoader;

    public MasterCategoryClosureLookupService(
            ObjectProvider<CacheService> cacheServiceProvider,
            MasterCategoryTreeLoader treeLoader) {
        this.cacheServiceProvider = cacheServiceProvider;
        this.treeLoader = treeLoader;
    }

    public List<String> loadCandidateCodes(String primaryCategoryCode) {
        return loadCandidateCodes(primaryCategoryCode, null);
    }

    public List<String> loadCandidateCodes(String primaryCategoryCode, String fallbackCategoryCode) {
        List<MasterCategoryValueRow> rows = listClosureRows(primaryCategoryCode);
        if (rows.isEmpty() && fallbackCategoryCode != null) {
            rows = listClosureRows(fallbackCategoryCode);
        }
        return rows.stream()
                .filter(row -> row != null && row.code() != null && !row.code().isBlank())
                .map(row -> row.code().trim())
                .distinct()
                .sorted(String::compareToIgnoreCase)
                .toList();
    }

    private List<MasterCategoryValueRow> listClosureRows(String categoryCode) {
        if (categoryCode == null || categoryCode.isBlank()) {
            return List.of();
        }
        String code = categoryCode.trim();
        CacheService cache = cacheServiceProvider.getIfAvailable();
        if (cache != null) {
            Optional<List<MasterCategoryValueRow>> cached =
                    cache.getList(MasterDataCacheKeys.categoryValuesClosure(code), MasterCategoryValueRow.class);
            if (cached.isPresent() && !cached.get().isEmpty()) {
                return List.copyOf(cached.get());
            }
        }
        MasterCategoryTreeLoader.MasterCategoryIndex index = treeLoader.loadIndex();
        return index.closureByCode().getOrDefault(code, List.of());
    }
}
