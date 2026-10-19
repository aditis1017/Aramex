package com.customcharge.persistencelayer.cache;

import com.customcharge.persistencelayer.cache.dto.MasterCategoryValueRow;
import com.customcharge.persistencelayer.cache.dto.MasterCategoryValueRowMapper;
import com.customcharge.persistencelayer.constant.ApplicationConstant;
import com.customcharge.persistencelayer.exception.ResourceNotFoundException;
import com.customcharge.persistencelayer.repository.CategoryValueRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Resolves category values from Redis (global settings / master cache) with DB fallback
 * when cache is disabled or a key is missing.
 */
@Service
public class MasterCategoryValueLookupService {

    private final CategoryValueRepository categoryValueRepository;
    private final ObjectProvider<CacheService> cacheServiceProvider;

    public MasterCategoryValueLookupService(
            CategoryValueRepository categoryValueRepository,
            ObjectProvider<CacheService> cacheServiceProvider) {
        this.categoryValueRepository = categoryValueRepository;
        this.cacheServiceProvider = cacheServiceProvider;
    }

    /**
     * All active values for a category code (e.g. {@code CURRENCY}), preferring Redis.
     */
    public List<MasterCategoryValueRow> listForCategory(String categoryCode) {
        if (categoryCode == null || categoryCode.isBlank()) {
            return List.of();
        }
        String code = categoryCode.trim();
        CacheService cache = cacheServiceProvider.getIfAvailable();
        if (cache != null) {
            Optional<List<MasterCategoryValueRow>> cached =
                    cache.getList(MasterDataCacheKeys.categoryValues(code), MasterCategoryValueRow.class);
            if (cached.isPresent() && !cached.get().isEmpty()) {
                return List.copyOf(cached.get());
            }
        }
        return categoryValueRepository.findByCategory_CodeAndIsActiveTrue(code).stream()
                .map(MasterCategoryValueRowMapper::from)
                .toList();
    }

    /**
     * Lookup by globally unique value {@code code} (Redis per-value key, then DB).
     */
    public Optional<MasterCategoryValueRow> findByCode(String valueCode) {
        if (valueCode == null || valueCode.isBlank()) {
            return Optional.empty();
        }
        String trimmed = valueCode.trim();
        CacheService cache = cacheServiceProvider.getIfAvailable();
        if (cache != null) {
            Optional<MasterCategoryValueRow> fromRedis =
                    cache.get(ApplicationConstant.KEY_PREFIX + trimmed, MasterCategoryValueRow.class);
            if (fromRedis.isPresent()) {
                return fromRedis;
            }
        }
        return categoryValueRepository.findByCode(trimmed).map(MasterCategoryValueRowMapper::from);
    }

    /**
     * Display string for API responses; falls back to the stored code when cache/DB miss.
     */
    public String resolveDisplayName(String valueCode) {
        if (valueCode == null || valueCode.isBlank()) {
            return null;
        }
        return findByCode(valueCode.trim())
                .map(r -> firstNonBlank(r.name(), r.label(), r.code()))
                .orElse(valueCode.trim());
    }

    /**
     * Resolves a UI / API numeric id to the canonical value code, ensuring the value belongs to the category.
     */
    public String requireCodeByIdInCategory(String categoryCode, Long id, String notFoundMessage) {
        if (id == null) {
            throw new ResourceNotFoundException(notFoundMessage);
        }
        return listForCategory(categoryCode).stream()
                .filter(r -> Objects.equals(id, r.id()))
                .map(MasterCategoryValueRow::code)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(notFoundMessage));
    }

    /**
     * Ensures {@code valueCode} is an active member of {@code categoryCode} (Redis list first, else DB-backed list).
     */
    public void requireActiveCodeInCategory(String categoryCode, String valueCode, String message) {
        if (valueCode == null || valueCode.isBlank()) {
            throw new ResourceNotFoundException(message);
        }
        String trimmed = valueCode.trim();
        boolean ok = listForCategory(categoryCode).stream()
                .filter(r -> !Boolean.FALSE.equals(r.isActive()))
                .anyMatch(r -> trimmed.equals(r.name()));
        if (!ok) {
            throw new ResourceNotFoundException(message);
        }
    }

    /**
     * Ensures a globally unique master {@code valueCode} exists and is active (Redis per-code entry, else DB).
     */
    public void requireActiveValueCode(String valueCode, String message) {
        if (valueCode == null || valueCode.isBlank()) {
            throw new ResourceNotFoundException(message);
        }
        findByCode(valueCode.trim())
                .filter(r -> !Boolean.FALSE.equals(r.isActive()))
                .orElseThrow(() -> new ResourceNotFoundException(message));
    }

    private static String firstNonBlank(String a, String b, String c) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        if (c != null && !c.isBlank()) {
            return c;
        }
        return "";
    }
}
