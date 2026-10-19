package com.customcharge.dashboard.service.impl;

import com.customcharge.dashboard.constants.AccountConstants;
import com.customcharge.dashboard.constants.ChargeTypeConstants;
import com.customcharge.dashboard.constants.ServiceCodesConstants;
import com.customcharge.dashboard.constants.ServiceCodesMessageConstants;
import com.customcharge.dashboard.util.ServiceCodeActiveSupport;
import com.customcharge.dashboard.service.AccountService;
import com.customcharge.persistencelayer.entities.Account;
import com.customcharge.persistencelayer.entities.ServiceCodes;
import com.customcharge.persistencelayer.enums.AccountNature;
import com.customcharge.persistencelayer.exception.BadRequestException;
import com.customcharge.persistencelayer.exception.ConflictException;
import com.customcharge.persistencelayer.exception.ResourceNotFoundException;
import com.customcharge.persistencelayer.repository.EntityRepository;
import com.customcharge.persistencelayer.repository.ServiceCodesRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ServiceCodesService {

    private final ServiceCodesRepository serviceCodesRepository;
    private final EntityRepository entityRepository;
    private final AccountService accountService;

    public ServiceCodesService(
            ServiceCodesRepository serviceCodesRepository,
            EntityRepository entityRepository,
            @Lazy AccountService accountService) {
        this.serviceCodesRepository = serviceCodesRepository;
        this.entityRepository = entityRepository;
        this.accountService = accountService;
    }

    /**
     * Non-deleted master service codes for the given entity and charge type (both required).
     */
    public List<ServiceCodes> findByEntityCodeAndChargeType(String entityCode, String chargeType) {
        return serviceCodesRepository.findByEntityCodeAndChargeTypeAndIsDeletedFalse(
                requireEntityCode(entityCode),
                requireChargeType(chargeType));
    }

    /**
     * Resolves an existing non-deleted master row (never creates).
     */
    public ServiceCodes requireExistingMaster(String entityCode, String chargeType, String serviceCode) {
        return serviceCodesRepository
                .findByEntity_CodeAndServiceCodeAndChargeTypeAndIsDeletedFalse(
                        requireEntityCode(entityCode),
                        requireServiceCode(serviceCode),
                        requireChargeType(chargeType))
                .orElseThrow(() -> new ResourceNotFoundException(
                        ServiceCodesMessageConstants.SERVICE_CODE_NOT_FOUND));
    }

    /**
     * Resolves master data by service code across multiple charge types (first match in list order).
     * Prefer {@link #requireExistingMaster(String, String, String)} for single charge-type resolution.
     */
    public ServiceCodes requireExistingMasterInChargeTypes(
            String entityCode, List<String> chargeTypes, String serviceCode) {
        String normalizedEntityCode = requireEntityCode(entityCode);
        String normalizedServiceCode = requireServiceCode(serviceCode);
        if (chargeTypes == null || chargeTypes.isEmpty()) {
            throw new BadRequestException(ServiceCodesMessageConstants.CHARGE_TYPE_REQUIRED_FOR_RESOLUTION);
        }
        for (String chargeType : chargeTypes) {
            Optional<ServiceCodes> master = serviceCodesRepository
                    .findByEntity_CodeAndServiceCodeAndChargeTypeAndIsDeletedFalse(
                            normalizedEntityCode,
                            normalizedServiceCode,
                            requireChargeType(chargeType));
            if (master.isPresent()) {
                return master.get();
            }
        }
        throw new ResourceNotFoundException(
                ServiceCodesMessageConstants.SERVICE_CODE_NOT_FOUND_ACROSS_CHARGE_TYPES_PREFIX
                        + chargeTypes);
    }

    /**
     * For {@link AccountNature#ENTITY_SETTINGS} upserts: routes {@code active} to the cost or revenue flag on the master.
     * AR/AP accounts are ignored so their existing service-charge flow is unchanged.
     */
    @Transactional(readOnly = false)
    public void updateEntitySettingsActiveFlag(
            Account account, ServiceCodes master, String accountChargeType, Boolean active) {
        if (account == null
                || master == null
                || active == null
                || !AccountNature.ENTITY_SETTINGS.equals(account.getAccountNature())) {
            return;
        }
        String resolvedChargeType = StringUtils.hasText(accountChargeType)
                ? accountChargeType
                : account.getChargeType();
        if (!StringUtils.hasText(resolvedChargeType)) {
            return;
        }
        if (ServiceCodeActiveSupport.applyEntitySettingsActiveFlag(master, resolvedChargeType, active)) {
            serviceCodesRepository.save(master);
            accountService.syncAccountToRedis(
                    null, AccountNature.ENTITY_SETTINGS, account.getEntityCode().trim(), resolvedChargeType);
        }
    }

    /**
     * Ensures sample master service codes exist for one entity and charge type.
     */
    @Transactional(readOnly = false)
    public int ensureSampleMastersForEntityAndChargeType(String entityCode, String chargeType) {
        String normalizedEntityCode = requireEntityCode(entityCode);
        String normalizedChargeType = requireChargeType(chargeType);
        List<ServiceCodesConstants.SampleServiceCodeTemplate> templates =
                ChargeTypeConstants.ENTITY_SETTINGS_REVENUE.contains(normalizedChargeType)
                        ? ServiceCodesConstants.SAMPLE_REVENUE_SERVICE_CODES
                        : ServiceCodesConstants.SAMPLE_COST_SERVICE_CODES;

        int created = 0;
        for (ServiceCodesConstants.SampleServiceCodeTemplate template : templates) {
            boolean existed = serviceCodesRepository.existsByEntity_CodeAndServiceCodeAndChargeTypeAndIsDeletedFalse(
                    normalizedEntityCode, template.serviceCode(), normalizedChargeType);
            ensureMaster(
                    normalizedEntityCode,
                    normalizedChargeType,
                    template.serviceCode(),
                    template.serviceDescription(),
                    template.taxCode());
            if (!existed) {
                created++;
            }
        }
        return created;
    }

    /**
     * Ensures reusable master data exists without overwriting an existing row (bootstrap / samples only).
     */
    @Transactional(readOnly = false)
    public ServiceCodes ensureMaster(
            String entityCode,
            String chargeType,
            String serviceCode,
            String serviceDescription,
            String taxCode) {
        String normalizedEntityCode = requireEntityCode(entityCode);
        String normalizedChargeType = normalizeChargeType(chargeType);
        String normalizedServiceCode = requireServiceCode(serviceCode);
        validateTaxCode(taxCode);
        validateDescription(serviceDescription);

        return serviceCodesRepository
                .findByEntity_CodeAndServiceCodeAndChargeTypeAndIsDeletedFalse(
                        normalizedEntityCode, normalizedServiceCode, normalizedChargeType)
                .orElseGet(() -> persistNewMaster(
                        normalizedEntityCode,
                        normalizedChargeType,
                        normalizedServiceCode,
                        serviceDescription,
                        taxCode));
    }

    private ServiceCodes persistNewMaster(
            String entityCode,
            String chargeType,
            String serviceCode,
            String serviceDescription,
            String taxCode) {
        com.customcharge.persistencelayer.entities.Entity entity = entityRepository.findByCodeIgnoreCase(entityCode)
                .filter(e -> Boolean.TRUE.equals(e.getActive()))
                .orElseThrow(() -> new BadRequestException(
                        ServiceCodesMessageConstants.ENTITY_NOT_FOUND_OR_INACTIVE_PREFIX + entityCode));

        ServiceCodes master = new ServiceCodes();
        master.setEntity(entity);
        master.setChargeType(chargeType);
        master.setServiceCode(serviceCode);
        master.setServiceDescription(trimToNull(serviceDescription));
        master.setTaxCode(trimToNull(taxCode));
        master.setIsDeleted(false);
        master.setIsActive(true);
//        master.setIsRevenueActive(true);

        try {
            ServiceCodes saved = serviceCodesRepository.save(master);
            accountService.syncAccountToRedis(null, AccountNature.ENTITY_SETTINGS, entityCode, chargeType);
            return saved;
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException(
                    ServiceCodesMessageConstants.SERVICE_CODE_ALREADY_EXISTS_PREFIX
                            + entityCode + " / " + chargeType + " / " + serviceCode);
        }
    }

    private static String normalizeChargeType(String chargeType) {
        if (!StringUtils.hasText(chargeType)) {
            return null;
        }
        return requireChargeType(chargeType);
    }

    public static String requireChargeType(String chargeType) {
        if (!StringUtils.hasText(chargeType)) {
            throw new BadRequestException(ServiceCodesMessageConstants.CHARGE_TYPE_REQUIRED);
        }
        String trimmed = chargeType.trim().toUpperCase();
        if (trimmed.length() > ServiceCodesConstants.MAX_CHARGE_TYPE_LENGTH) {
            throw new BadRequestException(
                    ServiceCodesMessageConstants.CHARGE_TYPE_MAX_LENGTH_PREFIX
                            + ServiceCodesConstants.MAX_CHARGE_TYPE_LENGTH
                            + ServiceCodesMessageConstants.CHARGE_TYPE_MAX_LENGTH_SUFFIX);
        }
        return trimmed;
    }

    public static String requireEntityCode(String entityCode) {
        if (!StringUtils.hasText(entityCode)) {
            throw new BadRequestException(ServiceCodesMessageConstants.ENTITY_CODE_REQUIRED);
        }
        return entityCode.trim();
    }

    private static String requireServiceCode(String serviceCode) {
        if (!StringUtils.hasText(serviceCode)) {
            throw new BadRequestException(ServiceCodesMessageConstants.SERVICE_CODE_REQUIRED);
        }
        String trimmed = serviceCode.trim();
        if (trimmed.length() > ServiceCodesConstants.MAX_SERVICE_CODE_LENGTH) {
            throw new BadRequestException(
                    ServiceCodesMessageConstants.SERVICE_CODE_MAX_LENGTH_PREFIX
                            + ServiceCodesConstants.MAX_SERVICE_CODE_LENGTH
                            + ServiceCodesMessageConstants.SERVICE_CODE_MAX_LENGTH_SUFFIX);
        }
        return trimmed;
    }

    private static void validateDescription(String serviceDescription) {
        if (serviceDescription != null
                && serviceDescription.trim().length() > ServiceCodesConstants.MAX_DESCRIPTION_LENGTH) {
            throw new BadRequestException(
                    ServiceCodesMessageConstants.SERVICE_DESCRIPTION_MAX_LENGTH_PREFIX
                            + ServiceCodesConstants.MAX_DESCRIPTION_LENGTH
                            + ServiceCodesMessageConstants.SERVICE_DESCRIPTION_MAX_LENGTH_SUFFIX);
        }
    }

    private static void validateTaxCode(String taxCode) {
        if (taxCode != null && taxCode.trim().length() > ServiceCodesConstants.MAX_TAX_CODE_LENGTH) {
            throw new BadRequestException(
                    ServiceCodesMessageConstants.TAX_CODE_MAX_LENGTH_PREFIX
                            + ServiceCodesConstants.MAX_TAX_CODE_LENGTH
                            + ServiceCodesMessageConstants.TAX_CODE_MAX_LENGTH_SUFFIX);
        }
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
