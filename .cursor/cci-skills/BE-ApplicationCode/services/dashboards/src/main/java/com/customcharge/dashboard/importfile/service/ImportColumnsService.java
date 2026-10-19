package com.customcharge.dashboard.importfile.service;

import com.customcharge.dashboard.cache.DashboardCacheNames;
import com.customcharge.dashboard.constants.ImportConstants;
import com.customcharge.persistencelayer.cache.CategoryCodesGlobalDataCacheService;
import com.customcharge.persistencelayer.entities.CategoryValue;
import com.customcharge.persistencelayer.exception.BadRequestException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class ImportColumnsService {

    private final CategoryCodesGlobalDataCacheService categoryCodesGlobalDataCacheService;
    private final ImportColumnHeaderSupport importColumnHeaderSupport;

    public ImportColumnsService(
            CategoryCodesGlobalDataCacheService categoryCodesGlobalDataCacheService,
            ImportColumnHeaderSupport importColumnHeaderSupport) {
        this.categoryCodesGlobalDataCacheService = categoryCodesGlobalDataCacheService;
        this.importColumnHeaderSupport = importColumnHeaderSupport;
    }

    @Cacheable(cacheNames = DashboardCacheNames.IMPORT_COLUMNS, key = "{#entity, #fileType}")
    public List<String> resolveColumns(String entity, String fileType) {
        String entityCode = requireNonBlank(entity, "entity");
        String normalizedFileType = requireNonBlank(fileType, "fileType");
        assertSupportedFileType(normalizedFileType);

        List<String> mandatoryColumns = resolveMandatoryColumns(normalizedFileType, entityCode);
        List<String> chargeTypes = resolveChargeTypes(normalizedFileType);
        List<String> serviceColumns =
                importColumnHeaderSupport.resolveServiceDescriptionHeaders(entityCode, chargeTypes);

        return ImportColumnHeaderSupport.mergeCaseInsensitiveUnique(mandatoryColumns, serviceColumns);
    }

    private List<String> resolveMandatoryColumns(String fileType, String entityCode) {
        String cacheCode = fileType + ImportConstants.MANDATORY_COLUMNS_SUFFIX + entityCode;
        return categoryCodesGlobalDataCacheService.findCategoryByCode(cacheCode)
                .map(CategoryValue::getName)
                .map(ImportColumnHeaderSupport::parseCommaSeparated)
                .orElse(List.of());
    }

    private List<String> resolveChargeTypes(String fileType) {
        return categoryCodesGlobalDataCacheService.findCategoryByCode(fileType + ImportConstants.CHARGE_SUFFIX)
                .map(CategoryValue::getLabel)
                .map(ImportColumnHeaderSupport::parseCommaSeparated)
                .orElse(List.of());
    }

    private static void assertSupportedFileType(String fileType) {
        String lower = fileType.toLowerCase(Locale.ROOT);
        if (!lower.contains(ImportConstants.FILE_TYPE_ACTUAL) && !lower.contains(ImportConstants.FILE_TYPE_ESTIMATE)) {
            throw new BadRequestException(
                    String.format(ImportConstants.MESSAGE_FILE_TYPE_MUST_CONTAIN_ACTUAL_OR_ESTIMATE, fileType));
        }
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(String.format(ImportConstants.MESSAGE_FIELD_REQUIRED, fieldName));
        }
        return value.strip();
    }
}
