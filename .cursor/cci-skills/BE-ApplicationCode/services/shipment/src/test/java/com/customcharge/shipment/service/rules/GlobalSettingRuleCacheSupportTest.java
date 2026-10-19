package com.customcharge.shipment.service.rules;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalSettingRuleCacheSupportTest {

    @Test
    void buildRuleDrlMapCacheKey_shouldNormalizeParts() {
        assertThat(GlobalSettingRuleCacheSupport.buildRuleDrlMapCacheKey(" AE ", " DDU "))
                .isEqualTo("rule:AE:DDU");
    }

    @Test
    void normalizeKeyPart_shouldReplaceSpaces() {
        assertThat(GlobalSettingRuleCacheSupport.normalizeKeyPart("New York")).isEqualTo("New_York");
    }

    @Test
    void adaptDrlFactToShipmentDto_shouldRewriteFactClass() {
        String drl = "when com.customcharge.persistencelayer.entities.Shipment() then end";
        String adapted = GlobalSettingRuleCacheSupport.adaptDrlFactToShipmentDto(drl);

        assertThat(adapted).contains("com.customcharge.persistencelayer.dto.ShipmentDto");
    }

    @Test
    void adaptDrlForShipmentFullness_shouldRewriteSetters() {
        String drl = "when ShipmentDto() then shipmentDto.setCategoryType(\"FULL\");";
        String adapted = GlobalSettingRuleCacheSupport.adaptDrlForShipmentFullness(drl);

        assertThat(adapted).contains("setIsShipmentFull(true)");
    }

    @Test
    void adaptDrlForCategoryOnShipmentMessageDto_shouldRewriteCategorySetters() {
        String drl = "shipmentDto.setShipmentType(\"DDU\"); shipmentDto.setCategoryTypeCode(\"X\");";
        String adapted = GlobalSettingRuleCacheSupport.adaptDrlForCategoryOnShipmentMessageDto(drl);

        assertThat(adapted).contains("setCategoryType");
        assertThat(adapted).doesNotContain("setShipmentType");
    }

    @Test
    void adaptDrlFactToShipmentDto_shouldReturnBlankInputUnchanged() {
        assertThat(GlobalSettingRuleCacheSupport.adaptDrlFactToShipmentDto(null)).isNull();
        assertThat(GlobalSettingRuleCacheSupport.adaptDrlFactToShipmentDto("  ")).isEqualTo("  ");
    }

    @Test
    void normalizeKeyPart_shouldHandleNull() {
        assertThat(GlobalSettingRuleCacheSupport.normalizeKeyPart(null)).isEmpty();
    }
}
