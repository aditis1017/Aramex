package com.customcharge.dashboard.util;

import com.customcharge.dashboard.constants.ArBusinessTypeConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static com.customcharge.dashboard.constants.ArBusinessTypeConstants.AR_BUSINESS_TYPE_CORPORATE;
import static com.customcharge.dashboard.constants.ArBusinessTypeConstants.AR_BUSINESS_TYPE_CORPORATE_CODE;
import static com.customcharge.dashboard.constants.ArBusinessTypeConstants.AR_BUSINESS_TYPE_E_COMMERCE_CODE;
import static com.customcharge.dashboard.constants.ArBusinessTypeConstants.AR_BUSINESS_TYPE_OIL_AND_GAS;
import static com.customcharge.dashboard.constants.ArBusinessTypeConstants.AR_BUSINESS_TYPE_OIL_AND_GAS_CODE;
import static com.customcharge.dashboard.constants.ArBusinessTypeConstants.AR_BUSINESS_TYPE_RETAIL_CASH;
import static com.customcharge.dashboard.constants.ArBusinessTypeConstants.AR_BUSINESS_TYPE_RETAIL_CASH_CODE;
import static com.customcharge.dashboard.constants.ArBusinessTypeConstants.AR_BUSINESS_TYPE_WHOLESALE;
import static com.customcharge.dashboard.constants.ArBusinessTypeConstants.AR_BUSINESS_TYPE_WHOLESALE_CODE;
import static com.customcharge.dashboard.constants.ArBusinessTypeConstants.BUSINESS_TYPE_E_COMMERCE;
import static com.customcharge.dashboard.constants.ArBusinessTypeConstants.SHIPMENT_BUSINESS_TYPE_B2B;
import static com.customcharge.dashboard.constants.ArBusinessTypeConstants.SHIPMENT_BUSINESS_TYPE_B2C;
import static org.assertj.core.api.Assertions.assertThat;

class ArShipmentBusinessTypeResolverTest {

    @ParameterizedTest
    @ValueSource(strings = {
            AR_BUSINESS_TYPE_WHOLESALE,
            AR_BUSINESS_TYPE_WHOLESALE_CODE,
            AR_BUSINESS_TYPE_CORPORATE,
            AR_BUSINESS_TYPE_CORPORATE_CODE,
            ArBusinessTypeConstants.AR_BUSINESS_TYPE_CORPORATE_LEGACY_CODE,
            AR_BUSINESS_TYPE_OIL_AND_GAS,
            AR_BUSINESS_TYPE_OIL_AND_GAS_CODE
    })
    void resolve_shouldReturnB2b_forCorporateBusinessTypes(String businessSegment) {
        assertThat(ArShipmentBusinessTypeResolver.resolve(businessSegment)).isEqualTo(SHIPMENT_BUSINESS_TYPE_B2B);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            BUSINESS_TYPE_E_COMMERCE,
            AR_BUSINESS_TYPE_E_COMMERCE_CODE,
            AR_BUSINESS_TYPE_RETAIL_CASH,
            AR_BUSINESS_TYPE_RETAIL_CASH_CODE
    })
    void resolve_shouldReturnB2c_forEcommerceBusinessTypes(String businessSegment) {
        assertThat(ArShipmentBusinessTypeResolver.resolve(businessSegment)).isEqualTo(SHIPMENT_BUSINESS_TYPE_B2C);
    }

    @Test
    void resolve_shouldReturnB2c_forEcommerceWithoutHyphen() {
        assertThat(ArShipmentBusinessTypeResolver.resolve("E Commerce")).isEqualTo(SHIPMENT_BUSINESS_TYPE_B2C);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "Unknown Type"})
    void resolve_shouldReturnNull_forBlankOrUnmappedBusinessType(String businessSegment) {
        assertThat(ArShipmentBusinessTypeResolver.resolve(businessSegment)).isNull();
    }

    @Test
    void resolveEffective_shouldUseExplicitBusinessType_whenPresent() {
        assertThat(ArShipmentBusinessTypeResolver.resolveEffective(SHIPMENT_BUSINESS_TYPE_B2B, BUSINESS_TYPE_E_COMMERCE))
                .isEqualTo(SHIPMENT_BUSINESS_TYPE_B2B);
    }

    @Test
    void resolveEffective_shouldDeriveFromBusinessSegment_whenBusinessTypeNull() {
        assertThat(ArShipmentBusinessTypeResolver.resolveEffective(null, AR_BUSINESS_TYPE_CORPORATE))
                .isEqualTo(SHIPMENT_BUSINESS_TYPE_B2B);
    }

    @Test
    void resolveEffective_shouldUseLegacyBusinessTypeAsSegment_whenBusinessTypeNotExplicit() {
        assertThat(ArShipmentBusinessTypeResolver.resolveEffective(AR_BUSINESS_TYPE_CORPORATE, null))
                .isEqualTo(SHIPMENT_BUSINESS_TYPE_B2B);
    }

    @ParameterizedTest
    @ValueSource(strings = {SHIPMENT_BUSINESS_TYPE_B2B, SHIPMENT_BUSINESS_TYPE_B2C, "b2b", "b2c"})
    void isExplicitBusinessType_shouldReturnTrue_forB2bOrB2c(String value) {
        assertThat(ArShipmentBusinessTypeResolver.isExplicitBusinessType(value)).isTrue();
    }

    @Test
    void isExplicitBusinessType_shouldReturnFalse_forSegmentValues() {
        assertThat(ArShipmentBusinessTypeResolver.isExplicitBusinessType(AR_BUSINESS_TYPE_CORPORATE)).isFalse();
    }
}
