package com.customcharge.dashboard.service;

import com.customcharge.dashboard.constants.RateSheetMessageConstants;
import com.customcharge.persistencelayer.entities.RateSheetItem;
import com.customcharge.persistencelayer.exception.BadRequestException;
import com.customcharge.persistencelayer.exception.ValidationException;
import com.customcharge.persistencelayer.repository.RateSheetItemRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Ensures service code, shipment type, business type, and category type combination is unique per rate sheet,
 * and that wildcard (ALL) configurations do not overlap for the same service code.
 */
@Component
public class RateSheetItemDuplicateValidator {

    private final RateSheetItemRepository rateSheetItemRepository;

    public RateSheetItemDuplicateValidator(RateSheetItemRepository rateSheetItemRepository) {
        this.rateSheetItemRepository = rateSheetItemRepository;
    }

    public void assertNotDuplicateOnCreate(
            UUID rateSheetId,
            String serviceCode,
            String shipmentType,
            String businessType,
            String categoryType) {
        assertNoItemConflicts(
                rateSheetId, serviceCode, shipmentType, businessType, categoryType, null, null, false);
    }

    public void assertNotDuplicateOnUpdate(
            UUID rateSheetId,
            UUID rateSheetItemId,
            String serviceCode,
            String shipmentType,
            String businessType,
            String categoryType) {
        assertNoItemConflicts(
                rateSheetId, serviceCode, shipmentType, businessType, categoryType, rateSheetItemId, null, false);
    }

    /**
     * Rejects a batch request that maps multiple items to the same identity key or overlapping configurations.
     */
    public void assertNoDuplicatesInBatch(List<ItemKey> items) {
        Set<String> keys = new HashSet<>();
        for (ItemKey item : items) {
            String key = itemKey(item);
            if (!keys.add(key)) {
                throw new BadRequestException(duplicateMessage(
                        item.serviceCode(),
                        item.shipmentType(),
                        item.businessType(),
                        item.categoryType(),
                        null));
            }
        }
        assertNoConfigurationOverlapInBatch(items, null, false);
    }

    public void assertImportNotDuplicate(UUID rateSheetId, List<ItemKey> importItems) {
        Set<String> keysInFile = new HashSet<>();
        for (ItemKey item : importItems) {
            String key = itemKey(item);
            if (!keysInFile.add(key)) {
                throw validationFailure(
                        duplicateMessage(
                                item.serviceCode(),
                                item.shipmentType(),
                                item.businessType(),
                                item.categoryType(),
                                null));
            }
        }

        assertNoConfigurationOverlapInBatch(importItems, null, true);

        List<RateSheetItem> existingItems =
                rateSheetItemRepository.findByRateSheet_IdAndIsDeletedFalse(rateSheetId);
        Set<String> existingKeys = new HashSet<>();
        for (RateSheetItem item : existingItems) {
            existingKeys.add(itemKey(
                    resolveItemServiceCode(item),
                    item.getShipmentType(),
                    item.getBusinessType(),
                    item.getCategoryType()));
        }
        for (ItemKey item : importItems) {
            String key = itemKey(item);
            if (existingKeys.contains(key)) {
                throw validationFailure(
                        duplicateMessage(
                                item.serviceCode(),
                                item.shipmentType(),
                                item.businessType(),
                                item.categoryType(),
                                null));
            }
            assertNoConfigurationOverlap(
                    existingItems,
                    item.serviceCode(),
                    item.shipmentType(),
                    item.businessType(),
                    item.categoryType(),
                    null,
                    null,
                    true);
        }
    }

    private void assertNoItemConflicts(
            UUID rateSheetId,
            String serviceCode,
            String shipmentType,
            String businessType,
            String categoryType,
            UUID excludeItemId,
            Integer rowNumber,
            boolean importContext) {
        List<RateSheetItem> activeItems =
                rateSheetItemRepository.findByRateSheet_IdAndIsDeletedFalse(rateSheetId);

        String targetKey = itemKey(serviceCode, shipmentType, businessType, categoryType);
        boolean duplicate = activeItems.stream()
                .filter(item -> excludeItemId == null || !excludeItemId.equals(item.getId()))
                .anyMatch(item -> itemKey(
                                resolveItemServiceCode(item),
                                item.getShipmentType(),
                                item.getBusinessType(),
                                item.getCategoryType())
                        .equals(targetKey));
        if (duplicate) {
            throw conflictException(
                    duplicateMessage(serviceCode, shipmentType, businessType, categoryType, rowNumber),
                    importContext);
        }

        assertNoConfigurationOverlap(
                activeItems,
                serviceCode,
                shipmentType,
                businessType,
                categoryType,
                excludeItemId,
                rowNumber,
                importContext);
    }

    private void assertNoConfigurationOverlapInBatch(
            List<ItemKey> items, Integer rowNumber, boolean importContext) {
        Map<String, List<ItemKey>> itemsByServiceCode = new HashMap<>();
        for (ItemKey item : items) {
            itemsByServiceCode
                    .computeIfAbsent(normalizeServiceCode(item.serviceCode()), ignored -> new ArrayList<>())
                    .add(item);
        }
        for (List<ItemKey> group : itemsByServiceCode.values()) {
            for (int i = 0; i < group.size(); i++) {
                ItemKey left = group.get(i);
                for (int j = i + 1; j < group.size(); j++) {
                    ItemKey right = group.get(j);
                    if (configurationsOverlap(left, right)) {
                        throw conflictException(
                                overlapConflictMessage(
                                        left.serviceCode(),
                                        left.shipmentType(),
                                        left.businessType(),
                                        left.categoryType(),
                                        right.shipmentType(),
                                        right.businessType(),
                                        right.categoryType(),
                                        rowNumber),
                                importContext);
                    }
                }
            }
        }
    }

    private void assertNoConfigurationOverlap(
            List<RateSheetItem> activeItems,
            String serviceCode,
            String shipmentType,
            String businessType,
            String categoryType,
            UUID excludeItemId,
            Integer rowNumber,
            boolean importContext) {
        List<RateSheetItem> sameServiceCodeItems =
                filterSameServiceCodeItems(activeItems, serviceCode, excludeItemId);

        if (sameServiceCodeItems.isEmpty()) {
            return;
        }

        boolean incomingCatchAll = RateSheetItemKeyUtil.isCatchAll(shipmentType, businessType, categoryType);

        for (RateSheetItem existing : sameServiceCodeItems) {
            if (!configurationsOverlap(
                    shipmentType,
                    businessType,
                    categoryType,
                    existing.getShipmentType(),
                    existing.getBusinessType(),
                    existing.getCategoryType())) {
                continue;
            }

            if (isCatchAllItem(existing) && !incomingCatchAll) {
                throw conflictException(catchAllExistsMessage(serviceCode, rowNumber), importContext);
            }
            if (incomingCatchAll) {
                throw conflictException(catchAllNotAllowedMessage(serviceCode, rowNumber), importContext);
            }
            throw conflictException(
                    overlapMessage(serviceCode, shipmentType, businessType, categoryType, rowNumber),
                    importContext);
        }
    }

    private List<RateSheetItem> filterSameServiceCodeItems(
            List<RateSheetItem> activeItems, String serviceCode, UUID excludeItemId) {
        String normalizedServiceCode = normalizeServiceCode(serviceCode);
        List<RateSheetItem> matches = new ArrayList<>();
        for (RateSheetItem item : activeItems) {
            if (excludeItemId != null && excludeItemId.equals(item.getId())) {
                continue;
            }
            if (normalizeServiceCode(resolveItemServiceCode(item)).equals(normalizedServiceCode)) {
                matches.add(item);
            }
        }
        return matches;
    }

    private boolean configurationsOverlap(ItemKey left, ItemKey right) {
        return RateSheetItemKeyUtil.configurationsOverlap(
                left.shipmentType(),
                left.businessType(),
                left.categoryType(),
                right.shipmentType(),
                right.businessType(),
                right.categoryType());
    }

    private boolean configurationsOverlap(
            String shipmentType1,
            String businessType1,
            String categoryType1,
            String shipmentType2,
            String businessType2,
            String categoryType2) {
        return RateSheetItemKeyUtil.configurationsOverlap(
                shipmentType1,
                businessType1,
                categoryType1,
                shipmentType2,
                businessType2,
                categoryType2);
    }

    private boolean isCatchAllItem(RateSheetItem item) {
        return RateSheetItemKeyUtil.isCatchAll(
                item.getShipmentType(), item.getBusinessType(), item.getCategoryType());
    }

    static String resolveItemServiceCode(RateSheetItem item) {
        if (item.getServiceCode() != null && !item.getServiceCode().isBlank()) {
            return item.getServiceCode();
        }
        return item.getDescription();
    }

    static String duplicateMessage(
            String serviceCode,
            String shipmentType,
            String businessType,
            String categoryType,
            Integer rowNumber) {
        String prefix = rowNumber == null
                ? ""
                : RateSheetMessageConstants.ROW_PREFIX + rowNumber + RateSheetMessageConstants.ROW_PREFIX_SEPARATOR;
        return prefix
                + RateSheetMessageConstants.DUPLICATE_ITEM_PREFIX
                + serviceCode
                + RateSheetMessageConstants.DUPLICATE_ITEM_MIDDLE
                + shipmentType
                + RateSheetMessageConstants.DUPLICATE_ITEM_BUSINESS_TYPE_MIDDLE
                + businessType
                + RateSheetMessageConstants.DUPLICATE_ITEM_CATEGORY_TYPE_MIDDLE
                + categoryType
                + RateSheetMessageConstants.DUPLICATE_ITEM_SUFFIX;
    }

    static String catchAllExistsMessage(String serviceCode, Integer rowNumber) {
        String prefix = rowNumber == null
                ? ""
                : RateSheetMessageConstants.ROW_PREFIX + rowNumber + RateSheetMessageConstants.ROW_PREFIX_SEPARATOR;
        return prefix
                + RateSheetMessageConstants.CATCH_ALL_EXISTS_PREFIX
                + serviceCode
                + RateSheetMessageConstants.CATCH_ALL_EXISTS_SUFFIX;
    }

    static String catchAllNotAllowedMessage(String serviceCode, Integer rowNumber) {
        String prefix = rowNumber == null
                ? ""
                : RateSheetMessageConstants.ROW_PREFIX + rowNumber + RateSheetMessageConstants.ROW_PREFIX_SEPARATOR;
        return prefix
                + RateSheetMessageConstants.CATCH_ALL_NOT_ALLOWED_PREFIX
                + serviceCode
                + RateSheetMessageConstants.CATCH_ALL_NOT_ALLOWED_SUFFIX;
    }

    static String overlapMessage(
            String serviceCode,
            String shipmentType,
            String businessType,
            String categoryType,
            Integer rowNumber) {
        String prefix = rowNumber == null
                ? ""
                : RateSheetMessageConstants.ROW_PREFIX + rowNumber + RateSheetMessageConstants.ROW_PREFIX_SEPARATOR;
        return prefix
                + RateSheetMessageConstants.OVERLAP_PREFIX
                + serviceCode
                + RateSheetMessageConstants.OVERLAP_MIDDLE
                + shipmentType
                + RateSheetMessageConstants.OVERLAP_BUSINESS_TYPE_MIDDLE
                + businessType
                + RateSheetMessageConstants.OVERLAP_CATEGORY_TYPE_MIDDLE
                + categoryType
                + RateSheetMessageConstants.OVERLAP_SUFFIX;
    }

    private static String overlapConflictMessage(
            String serviceCode,
            String leftShipmentType,
            String leftBusinessType,
            String leftCategoryType,
            String rightShipmentType,
            String rightBusinessType,
            String rightCategoryType,
            Integer rowNumber) {
        if (RateSheetItemKeyUtil.isCatchAll(leftShipmentType, leftBusinessType, leftCategoryType)
                && !RateSheetItemKeyUtil.isCatchAll(rightShipmentType, rightBusinessType, rightCategoryType)) {
            return catchAllNotAllowedMessage(serviceCode, rowNumber);
        }
        if (RateSheetItemKeyUtil.isCatchAll(rightShipmentType, rightBusinessType, rightCategoryType)
                && !RateSheetItemKeyUtil.isCatchAll(leftShipmentType, leftBusinessType, leftCategoryType)) {
            return catchAllNotAllowedMessage(serviceCode, rowNumber);
        }
        return overlapMessage(serviceCode, leftShipmentType, leftBusinessType, leftCategoryType, rowNumber);
    }

    private static String normalizeServiceCode(String serviceCode) {
        return serviceCode == null ? "" : serviceCode.strip().toUpperCase(Locale.ROOT);
    }

    private static RuntimeException conflictException(String message, boolean importContext) {
        if (importContext) {
            return validationFailure(message);
        }
        return new BadRequestException(message);
    }

    private static ValidationException validationFailure(String message) {
        return new ValidationException(message + RateSheetMessageConstants.IMPORT_REJECTED_SUFFIX);
    }

    private static String itemKey(ItemKey item) {
        return itemKey(item.serviceCode(), item.shipmentType(), item.businessType(), item.categoryType());
    }

    private static String itemKey(
            String serviceCode,
            String shipmentType,
            String businessType,
            String categoryType) {
        return RateSheetItemKeyUtil.itemKey(serviceCode, shipmentType, businessType, categoryType);
    }

    public record ItemKey(
            String serviceCode,
            String shipmentType,
            String businessType,
            String categoryType) {
    }
}
