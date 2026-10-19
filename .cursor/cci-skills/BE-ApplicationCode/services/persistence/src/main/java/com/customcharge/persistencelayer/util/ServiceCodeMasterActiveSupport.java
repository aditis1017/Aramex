package com.customcharge.persistencelayer.util;

import com.customcharge.persistencelayer.entities.ServiceCodes;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Shared activation rules for {@link ServiceCodes} masters. Each charge type uses its own
 * {@code isActive} flag independently.
 */
public final class ServiceCodeMasterActiveSupport {

    private static final Set<String> COST_CHARGE_TYPES = Set.of("ECD", "ECS", "EC3");
    private static final Set<String> REVENUE_CHARGE_TYPES = Set.of("DTA", "DTC", "DTS", "DT3");

    private ServiceCodeMasterActiveSupport() {
    }

    public static String normalize(String chargeType) {
        if (!StringUtils.hasText(chargeType)) {
            return "";
        }
        return chargeType.trim().toUpperCase(Locale.ROOT);
    }

    public static String masterKey(String serviceCode, String chargeType) {
        return normalize(serviceCode) + "|" + normalize(chargeType);
    }

    public static Map<String, ServiceCodes> indexByServiceCodeAndChargeType(Collection<ServiceCodes> masters) {
        Map<String, ServiceCodes> indexed = new LinkedHashMap<>();
        if (masters == null) {
            return indexed;
        }
        for (ServiceCodes master : masters) {
            if (master == null || !StringUtils.hasText(master.getServiceCode())) {
                continue;
            }
            String chargeType = master.getChargeType();
            if (!StringUtils.hasText(chargeType)) {
                continue;
            }
            indexed.putIfAbsent(masterKey(master.getServiceCode(), chargeType), master);
        }
        return indexed;
    }

    public static Optional<String> linkedCostChargeType(String chargeType) {
        return switch (normalize(chargeType)) {
            case "DTA", "DTC" -> Optional.of("ECD");
            case "DTS" -> Optional.of("ECS");
            case "DT3" -> Optional.of("EC3");
            default -> Optional.empty();
        };
    }

    public static ServiceCodes resolveLinkedCostMaster(ServiceCodes master, Map<String, ServiceCodes> masterIndex) {
        if (master == null || masterIndex == null || masterIndex.isEmpty()) {
            return null;
        }
        Optional<String> linkedCostType = linkedCostChargeType(master.getChargeType());
        if (linkedCostType.isEmpty() || !StringUtils.hasText(master.getServiceCode())) {
            return null;
        }
        return masterIndex.get(masterKey(master.getServiceCode(), linkedCostType.get()));
    }

    public static boolean isCostChargeType(String chargeType) {
        return COST_CHARGE_TYPES.contains(normalize(chargeType));
    }

    public static boolean isRevenueChargeType(String chargeType) {
        return REVENUE_CHARGE_TYPES.contains(normalize(chargeType));
    }

    /**
     * Master-level eligibility before account {@code ServiceCharge} flags are applied.
     */
    public static boolean isMasterActive(ServiceCodes master) {
        if (master == null || Boolean.TRUE.equals(master.getIsDeleted())) {
            return false;
        }
        return Boolean.TRUE.equals(master.getIsActive());
    }

    /**
     * @deprecated Use {@link #isMasterActive(ServiceCodes)} — linked cost masters are no longer consulted.
     */
    @Deprecated
    public static boolean isMasterActive(ServiceCodes master, ServiceCodes linkedCostMaster) {
        return isMasterActive(master);
    }

    /**
     * AR account master eligibility before account {@code ServiceCharge} flags are applied.
     */
    public static boolean isMasterEligibleForAr(ServiceCodes master) {
        return isMasterActive(master);
    }

    /**
     * @deprecated Use {@link #isMasterEligibleForAr(ServiceCodes)} — linked cost masters are no longer consulted.
     */
    @Deprecated
    public static boolean isMasterEligibleForAr(ServiceCodes master, ServiceCodes linkedCostMaster) {
        return isMasterEligibleForAr(master);
    }
}
