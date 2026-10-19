package com.customcharge.chargescalculator.shipmentingest.masterdata;

import com.customcharge.persistencelayer.cache.CacheService;
import com.customcharge.persistencelayer.cache.MasterDataCacheKeys;
import com.customcharge.persistencelayer.cache.dto.MasterCategoryValueRow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;


@Service
@Slf4j
public class MasterDataCodeLookupService {

    private final ObjectProvider<CacheService> cacheServiceProvider;

    public MasterDataCodeLookupService(ObjectProvider<CacheService> cacheServiceProvider) {
        this.cacheServiceProvider = cacheServiceProvider;
    }

    public Optional<String> resolveCode(String categoryCode, String input) {
        if (categoryCode == null || categoryCode.isBlank() || input == null || input.isBlank()) {
            return Optional.empty();
        }

        CacheService cacheService = cacheServiceProvider.getIfAvailable();
        if (cacheService == null) {
            return Optional.empty();
        }

        String needle = normalize(input);
        String key = MasterDataCacheKeys.categoryValuesClosure(categoryCode.trim());

        List<MasterCategoryValueRow> rows;
        try {
            rows = cacheService.getList(key, MasterCategoryValueRow.class).orElse(List.of());
        } catch (Exception ex) {
            log.warn("event=redis_lookup_failed key={} reason={}", key, ex.getMessage());
            return Optional.empty();
        }

        if (rows.isEmpty()) {
            return Optional.empty();
        }

        return rows.stream()
                .filter(row -> row != null && row.code() != null)
                .filter(row -> normalize(row.code()).equals(needle))
                .map(MasterCategoryValueRow::code)
                .findFirst()
                .or(() -> rows.stream()
                        .filter(row -> row != null && row.label() != null && row.code() != null)
                        .filter(row -> normalize(row.label()).equals(needle))
                        .map(MasterCategoryValueRow::code)
                        .findFirst())
                .or(() -> rows.stream()
                        .filter(row -> row != null && row.name() != null && row.code() != null)
                        .filter(row -> normalize(row.name()).equals(needle))
                        .map(MasterCategoryValueRow::code)
                        .findFirst());
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
