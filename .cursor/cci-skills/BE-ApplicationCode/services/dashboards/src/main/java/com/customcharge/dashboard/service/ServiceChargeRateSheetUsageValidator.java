package com.customcharge.dashboard.service;

import com.customcharge.dashboard.constants.RateSheetMessageConstants;
import com.customcharge.persistencelayer.exception.BadRequestException;
import com.customcharge.persistencelayer.repository.RateSheetItemRepository;
import com.customcharge.persistencelayer.repository.RateSheetRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * Blocks deactivating a service code when it is still referenced on the account rate sheet.
 */
@Component
public class ServiceChargeRateSheetUsageValidator {

    private final RateSheetRepository rateSheetRepository;
    private final RateSheetItemRepository rateSheetItemRepository;

    public ServiceChargeRateSheetUsageValidator(
            RateSheetRepository rateSheetRepository, RateSheetItemRepository rateSheetItemRepository) {
        this.rateSheetRepository = rateSheetRepository;
        this.rateSheetItemRepository = rateSheetItemRepository;
    }

    public void assertNotUsedOnRateSheet(UUID accountId, String serviceCode) {
        if (accountId == null || !StringUtils.hasText(serviceCode)) {
            return;
        }
        String normalizedServiceCode = serviceCode.trim();
        rateSheetRepository.findByAccount_Id(accountId).ifPresent(rateSheet -> {
            boolean inUse = rateSheetItemRepository.findByRateSheet_IdAndIsDeletedFalse(rateSheet.getId())
                    .stream()
                    .anyMatch(item -> normalizedServiceCode.equalsIgnoreCase(
                            RateSheetItemDuplicateValidator.resolveItemServiceCode(item)));
            if (inUse) {
                throw new BadRequestException(
                        RateSheetMessageConstants.CANNOT_DEACTIVATE_SERVICE_CODE_PREFIX
                                + normalizedServiceCode
                                + RateSheetMessageConstants.CANNOT_DEACTIVATE_SERVICE_CODE_SUFFIX);
            }
        });
    }
}
