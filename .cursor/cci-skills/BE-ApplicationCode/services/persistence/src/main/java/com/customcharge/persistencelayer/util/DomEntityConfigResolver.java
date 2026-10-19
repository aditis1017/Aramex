package com.customcharge.persistencelayer.util;

import com.customcharge.persistencelayer.cache.CategoryCodesGlobalDataCacheService;
import com.customcharge.persistencelayer.constant.DomEntityConstants;
import com.customcharge.persistencelayer.entities.CategoryValue;
import com.customcharge.persistencelayer.repository.CategoryValueRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DomEntityConfigResolver {

    private static volatile DomEntityConfigResolver instance;

    private final ObjectProvider<CategoryCodesGlobalDataCacheService> categoryCodesCacheService;
    private final CategoryValueRepository categoryValueRepository;

    public DomEntityConfigResolver(
            ObjectProvider<CategoryCodesGlobalDataCacheService> categoryCodesCacheService,
            CategoryValueRepository categoryValueRepository) {
        this.categoryCodesCacheService = categoryCodesCacheService;
        this.categoryValueRepository = categoryValueRepository;
    }

    @PostConstruct
    void registerInstance() {
        instance = this;
    }

    public static String resolveConfiguredDomEntityCode(String consigneeCountryCode) {
        DomEntityConfigResolver resolver = instance;
        return resolver != null
                ? resolver.resolveDomEntityCode(consigneeCountryCode)
                : DomEntityConstants.DEFAULT_DOM_ENTITY_CODE;
    }

    public String resolveDomEntityCode(String consigneeCountryCode) {
        return DomEntityConstants.buildDomEntityValueCode(consigneeCountryCode)
                .flatMap(this::resolveDomEntityCodeByValueCode)
                .orElse(DomEntityConstants.DEFAULT_DOM_ENTITY_CODE);
    }

    public String formatDomEntityValidationMessage(String consigneeCountryCode) {
        return DomEntityConstants.formatDomEntityValidationMessage(resolveDomEntityCode(consigneeCountryCode));
    }

    private java.util.Optional<String> resolveDomEntityCodeByValueCode(String domEntityValueCode) {
        CategoryCodesGlobalDataCacheService cache = categoryCodesCacheService.getIfAvailable();
        if (cache != null) {
            try {
                String entityCode = cache.getCategoryCodeByCode(domEntityValueCode).getName();
                if (StringUtils.hasText(entityCode)) {
                    return java.util.Optional.of(entityCode.strip());
                }
            } catch (RuntimeException ignored) {
                // Fall back to database lookup when cache is unavailable or value is missing.
            }
        }
        return categoryValueRepository.findByCode(domEntityValueCode)
                .filter(value -> Boolean.TRUE.equals(value.getIsActive()))
                .map(CategoryValue::getName)
                .filter(StringUtils::hasText)
                .map(String::strip);
    }
}
