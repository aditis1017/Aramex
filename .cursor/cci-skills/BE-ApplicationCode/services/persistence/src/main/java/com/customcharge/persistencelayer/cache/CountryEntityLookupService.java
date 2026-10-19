package com.customcharge.persistencelayer.cache;

import com.customcharge.persistencelayer.common.CategoryNode;
import com.customcharge.persistencelayer.dto.CountryWithEntitiesDropdownDto;
import com.customcharge.persistencelayer.dto.EntityDropdownOption;
import com.customcharge.persistencelayer.entities.Country;
import com.customcharge.persistencelayer.entities.Entity;
import com.customcharge.persistencelayer.repository.CountryRepository;
import com.customcharge.persistencelayer.repository.EntityRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Countries with nested entities for dropdowns: prefers Redis {@code master:category:Entity:tree}
 * ({@link MasterDataCacheKeys#categoryTree(String)} with synthetic {@code Entity}), else DB.
 */
@Service
public class CountryEntityLookupService {

    private static final Comparator<String> NAME_ORDER =
            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER);

    private final ObjectProvider<CacheService> cacheServiceProvider;
    private final CountryRepository countryRepository;
    private final EntityRepository entityRepository;

    public CountryEntityLookupService(
            ObjectProvider<CacheService> cacheServiceProvider,
            CountryRepository countryRepository,
            EntityRepository entityRepository) {
        this.cacheServiceProvider = cacheServiceProvider;
        this.countryRepository = countryRepository;
        this.entityRepository = entityRepository;
    }

    public List<CountryWithEntitiesDropdownDto> listCountriesWithActiveEntities() {
        CacheService cache = cacheServiceProvider.getIfAvailable();
        if (cache != null) {
            Optional<CategoryNode> root = readEntityTree(cache);
            if (root.isPresent()) {
                List<CountryWithEntitiesDropdownDto> fromTree = mapEntityTree(root.get());
                if (!fromTree.isEmpty()) {
                    return fromTree;
                }
            }
        }
        return buildFromDatabase();
    }

    private Optional<CategoryNode> readEntityTree(CacheService cache) {
        String key = MasterDataCacheKeys.categoryTree(MasterDataCacheKeys.SYNTHETIC_ENTITY_CATEGORY_CODE);
        try {
            return cache.get(key, CategoryNode.class).filter(n -> n.getChildren() != null && !n.getChildren().isEmpty());
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    /**
     * Country nodes under the Entity root use synthetic negative ids ({@code -dbCountryId}); entity nodes use positive ids.
     */
    private List<CountryWithEntitiesDropdownDto> mapEntityTree(CategoryNode root) {
        return root.getChildren().stream()
                .filter(n -> !Boolean.FALSE.equals(n.getActive()))
                .filter(n -> n.getValue() != null && !n.getValue().isBlank())
                .sorted(Comparator.comparing(CategoryNode::getLabel, NAME_ORDER))
                .map(this::countryNodeToDto)
                .filter(Objects::nonNull)
                .toList();
    }

    private CountryWithEntitiesDropdownDto countryNodeToDto(CategoryNode country) {
        Long countryId = resolveCountryDbId(country);
        if (countryId == null) {
            return null;
        }
        List<CategoryNode> kids = country.getChildren() != null ? country.getChildren() : List.of();
        List<EntityDropdownOption> entities = kids.stream()
                .filter(n -> !Boolean.FALSE.equals(n.getActive()))
                .filter(n -> n.getId() != null && n.getId() > 0)
                .filter(n -> n.getValue() != null && !n.getValue().isBlank())
                .sorted(Comparator.comparing(CategoryNode::getLabel, NAME_ORDER))
                .map(n -> toEntityDropdownOption(n.getId(), n.getValue()))
                .toList();
        return new CountryWithEntitiesDropdownDto(
                countryId,
                country.getValue().trim(),
                nonBlank(country.getLabel(), country.getValue()),
                entities);
    }

    private static Long resolveCountryDbId(CategoryNode country) {
        Long id = country.getId();
        if (id == null) {
            return null;
        }
        return id < 0 ? -id : id;
    }

    private List<CountryWithEntitiesDropdownDto> buildFromDatabase() {
        List<Country> countries = countryRepository.findByActiveTrue().stream()
                .filter(c -> !Boolean.FALSE.equals(c.getActive()))
                .sorted(Comparator.comparing(Country::getName, NAME_ORDER))
                .toList();
        Map<Long, List<EntityDropdownOption>> byCountry = new HashMap<>();
        for (Entity e : entityRepository.findAllActiveWithActiveCountry()) {
            if (e.getCountry() == null || e.getCountry().getId() == null) {
                continue;
            }
            if (Boolean.FALSE.equals(e.getActive())) {
                continue;
            }
            byCountry
                    .computeIfAbsent(e.getCountry().getId(), k -> new ArrayList<>())
                    .add(toEntityDropdownOption(e.getId(), e.getCode()));
        }
        byCountry.forEach((k, list) -> list.sort(Comparator.comparing(EntityDropdownOption::name, NAME_ORDER)));
        return countries.stream()
                .map(c -> new CountryWithEntitiesDropdownDto(
                        c.getId(),
                        c.getCode(),
                        c.getName(),
                        List.copyOf(byCountry.getOrDefault(c.getId(), List.of()))))
                .toList();
    }

    private static EntityDropdownOption toEntityDropdownOption(Long id, String code) {
        String trimmedCode = code != null ? code.trim() : "";
        return new EntityDropdownOption(id, trimmedCode, trimmedCode);
    }

    private static String nonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback != null ? fallback : "";
    }
}
