package com.customcharge.persistencelayer.util;

import com.customcharge.persistencelayer.entities.ServiceCodes;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceCodeMasterActiveSupportTest {

    @Test
    void normalize_shouldUppercaseAndTrim() {
        assertThat(ServiceCodeMasterActiveSupport.normalize(" dtc ")).isEqualTo("DTC");
        assertThat(ServiceCodeMasterActiveSupport.normalize(" ")).isEmpty();
    }

    @Test
    void masterKey_shouldCombineServiceCodeAndChargeType() {
        assertThat(ServiceCodeMasterActiveSupport.masterKey(" sc01 ", " dtc "))
                .isEqualTo("SC01|DTC");
    }

    @Test
    void indexByServiceCodeAndChargeType_shouldSkipInvalidEntries() {
        ServiceCodes valid = serviceCode("SC01", "DTC", true, false);
        ServiceCodes blankCode = serviceCode(" ", "DTC", true, false);
        ServiceCodes blankCharge = serviceCode("SC02", " ", true, false);

        Map<String, ServiceCodes> indexed = ServiceCodeMasterActiveSupport.indexByServiceCodeAndChargeType(
                java.util.Arrays.asList(valid, blankCode, blankCharge));

        assertThat(indexed).containsOnlyKeys("SC01|DTC");
        assertThat(indexed.get("SC01|DTC")).isSameAs(valid);
    }

    @Test
    void linkedCostChargeType_shouldMapRevenueToCost() {
        assertThat(ServiceCodeMasterActiveSupport.linkedCostChargeType("DTA")).contains("ECD");
        assertThat(ServiceCodeMasterActiveSupport.linkedCostChargeType("DTS")).contains("ECS");
        assertThat(ServiceCodeMasterActiveSupport.linkedCostChargeType("DT3")).contains("EC3");
        assertThat(ServiceCodeMasterActiveSupport.linkedCostChargeType("OTHER")).isEmpty();
    }

    @Test
    void resolveLinkedCostMaster_shouldFindLinkedMaster() {
        ServiceCodes revenue = serviceCode("SC01", "DTA", true, false);
        ServiceCodes cost = serviceCode("SC01", "ECD", true, false);
        Map<String, ServiceCodes> index = ServiceCodeMasterActiveSupport.indexByServiceCodeAndChargeType(
                List.of(revenue, cost));

        assertThat(ServiceCodeMasterActiveSupport.resolveLinkedCostMaster(revenue, index)).isSameAs(cost);
        assertThat(ServiceCodeMasterActiveSupport.resolveLinkedCostMaster(null, index)).isNull();
    }

    @Test
    void chargeTypeChecks_shouldIdentifyCostAndRevenue() {
        assertThat(ServiceCodeMasterActiveSupport.isCostChargeType("ecd")).isTrue();
        assertThat(ServiceCodeMasterActiveSupport.isRevenueChargeType("dtc")).isTrue();
        assertThat(ServiceCodeMasterActiveSupport.isCostChargeType("dtc")).isFalse();
    }

    @Test
    void isMasterActive_shouldRespectDeletedAndActiveFlags() {
        assertThat(ServiceCodeMasterActiveSupport.isMasterActive(serviceCode("SC01", "DTC", true, false))).isTrue();
        assertThat(ServiceCodeMasterActiveSupport.isMasterActive(serviceCode("SC01", "DTC", false, false))).isFalse();
        assertThat(ServiceCodeMasterActiveSupport.isMasterActive(serviceCode("SC01", "DTC", true, true))).isFalse();
        assertThat(ServiceCodeMasterActiveSupport.isMasterActive(null)).isFalse();
    }

    @Test
    void deprecatedOverload_shouldDelegateToSingleMasterCheck() {
        ServiceCodes master = serviceCode("SC01", "DTC", true, false);
        assertThat(ServiceCodeMasterActiveSupport.isMasterActive(master, null)).isTrue();
        assertThat(ServiceCodeMasterActiveSupport.isMasterEligibleForAr(master, null)).isTrue();
    }

    private static ServiceCodes serviceCode(String code, String chargeType, boolean active, boolean deleted) {
        ServiceCodes master = new ServiceCodes();
        master.setServiceCode(code);
        master.setChargeType(chargeType);
        master.setIsActive(active);
        master.setIsDeleted(deleted);
        return master;
    }
}
