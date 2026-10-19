package com.customcharge.dashboard.service;

import com.customcharge.dashboard.constants.MasterCategoryConstants;
import com.customcharge.dashboard.constants.RateSheetConstants;
import com.customcharge.dashboard.constants.RateSheetMessageConstants;
import com.customcharge.dashboard.dto.ServiceCodeDropdownOption;
import com.customcharge.dashboard.service.impl.ServiceChargeService;
import com.customcharge.persistencelayer.cache.MasterCategoryValueLookupService;
import com.customcharge.persistencelayer.exception.BadRequestException;
import com.customcharge.persistencelayer.exception.ResourceNotFoundException;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Shared validation for rate sheet items: service code (dropdown), shipment type (master data),
 * and unique service code, shipment type, business type, and category type per rate sheet.
 */
@Component
public class RateSheetItemFieldValidation {

    private final MasterCategoryValueLookupService masterCategoryValueLookupService;
    private final ServiceChargeService serviceChargeService;
    private final RateSheetItemDuplicateValidator duplicateValidator;

    public RateSheetItemFieldValidation(
            MasterCategoryValueLookupService masterCategoryValueLookupService,
            ServiceChargeService serviceChargeService,
            RateSheetItemDuplicateValidator duplicateValidator) {
        this.masterCategoryValueLookupService = masterCategoryValueLookupService;
        this.serviceChargeService = serviceChargeService;
        this.duplicateValidator = duplicateValidator;
    }

    public ValidatedFields validateForCreate(
            UUID rateSheetId,
            String accountId,
            String entityCode,
            String chargeType,
            String serviceCode,
            String shipmentType,
            String businessType,
            String categoryType) {
        ValidatedFields fields = normalizeFields(
                accountId, entityCode, chargeType, serviceCode, shipmentType, businessType, categoryType, null);
        duplicateValidator.assertNotDuplicateOnCreate(
                rateSheetId,
                fields.serviceCode(),
                fields.shipmentType(),
                fields.businessType(),
                fields.categoryType());
        return fields;
    }

    public ValidatedFields validateForUpdate(
            UUID rateSheetId,
            UUID rateSheetItemId,
            String accountId,
            String entityCode,
            String chargeType,
            String serviceCode,
            String shipmentType,
            String businessType,
            String categoryType) {
        ValidatedFields fields = normalizeFields(
                accountId, entityCode, chargeType, serviceCode, shipmentType, businessType, categoryType, null);
        duplicateValidator.assertNotDuplicateOnUpdate(
                rateSheetId,
                rateSheetItemId,
                fields.serviceCode(),
                fields.shipmentType(),
                fields.businessType(),
                fields.categoryType());
        return fields;
    }

    /**
     * Normalizes fields only (no duplicate check). Import applies
     * {@link RateSheetItemDuplicateValidator#assertImportNotDuplicate} on the full file.
     */
    public ValidatedFields normalizeFields(
            String accountId,
            String entityCode,
            String chargeType,
            String serviceCode,
            String shipmentType,
            String businessType,
            String categoryType,
            Integer rowNumber) {
        String prefix = rowNumber == null
                ? ""
                : RateSheetMessageConstants.ROW_PREFIX + rowNumber + RateSheetMessageConstants.ROW_PREFIX_SEPARATOR;

        String normalizedServiceCode = requireValidServiceCode(
                accountId,
                entityCode,
                chargeType,
                serviceCode,
                prefix + RateSheetMessageConstants.SERVICE_CODE_REQUIRED,
                prefix + RateSheetMessageConstants.SERVICE_CODE_INVALID_PREFIX
                        + safeDisplay(serviceCode)
                        + RateSheetMessageConstants.SERVICE_CODE_INVALID_SUFFIX);

        String normalizedShipmentType = normalizeShipmentType(
                shipmentType,
                prefix + RateSheetMessageConstants.SHIPMENT_TYPE_REQUIRED,
                prefix + RateSheetMessageConstants.SHIPMENT_TYPE_INVALID_PREFIX
                        + safeDisplay(shipmentType)
                        + RateSheetMessageConstants.SHIPMENT_TYPE_INVALID_SUFFIX);

        String normalizedBusinessType = normalizeAllAwareType(
                businessType, prefix + RateSheetMessageConstants.BUSINESS_TYPE_REQUIRED);
        String normalizedCategoryType = normalizeAllAwareType(
                categoryType, prefix + RateSheetMessageConstants.CATEGORY_TYPE_REQUIRED);

        return new ValidatedFields(
                normalizedServiceCode,
                normalizedShipmentType,
                normalizedBusinessType,
                normalizedCategoryType);
    }

    private String requireValidServiceCode(
            String accountId,
            String entityCode,
            String chargeType,
            String rawValue,
            String blankMessage,
            String invalidMessage) {
        String normalized = requireNonBlank(rawValue, blankMessage);
        boolean valid = serviceChargeService
                .getActiveServiceCodesForDropdown(accountId, entityCode, chargeType)
                .stream()
                .map(ServiceCodeDropdownOption::getServiceCode)
                .anyMatch(code -> code != null && code.equalsIgnoreCase(normalized));
        if (!valid) {
            throw new BadRequestException(invalidMessage);
        }
        return normalized;
    }

    private static String requireNonBlank(String rawValue, String blankMessage) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new BadRequestException(blankMessage);
        }
        return rawValue.strip();
    }

    private String normalizeShipmentType(String shipmentType, String blankMessage, String invalidMessage) {
        String trimmed = requireNonBlank(shipmentType, blankMessage);
        if (RateSheetItemKeyUtil.isAll(trimmed)) {
            return RateSheetConstants.VALUE_ALL;
        }
        return requireMasterCode(trimmed, MasterCategoryConstants.SHIPMENT_TYPE, blankMessage, invalidMessage);
    }

    private static String normalizeAllAwareType(String rawValue, String blankMessage) {
        String trimmed = requireNonBlank(rawValue, blankMessage);
        if (RateSheetItemKeyUtil.isAll(trimmed)) {
            return RateSheetConstants.VALUE_ALL;
        }
        return trimmed;
    }

    private String requireMasterCode(
            String rawValue,
            String categoryCode,
            String blankMessage,
            String invalidMessage) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new BadRequestException(blankMessage);
        }
        String trimmed = rawValue.strip();
        try {
            masterCategoryValueLookupService.requireActiveCodeInCategory(
                    categoryCode, trimmed, invalidMessage);
        } catch (ResourceNotFoundException ex) {
            throw new BadRequestException(invalidMessage);
        }
        return trimmed;
    }

    private static String safeDisplay(String value) {
        return value == null ? "" : value.strip();
    }

    public record ValidatedFields(
            String serviceCode,
            String shipmentType,
            String businessType,
            String categoryType) {
    }
}
