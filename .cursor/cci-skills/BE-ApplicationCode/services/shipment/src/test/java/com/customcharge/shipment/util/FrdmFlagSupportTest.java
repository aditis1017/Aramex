package com.customcharge.shipment.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static com.customcharge.shipment.util.FrdmFlagSupport.FRDM_FLAG_NO;
import static com.customcharge.shipment.util.FrdmFlagSupport.FRDM_FLAG_YES;
import static org.assertj.core.api.Assertions.assertThat;

class FrdmFlagSupportTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "FRDM",
            "CSBV,FRDM",
            "FRDM,ELIM",
            "CODS,FRDM,BYDDES",
            "FRDM,SIGR,BYDDES",
            "frdm,manu"
    })
    void resolve_shouldReturnYes_whenServicesContainFrdm(String services) {
        assertThat(FrdmFlagSupport.resolve(services)).isEqualTo(FRDM_FLAG_YES);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"MANU", "INSR", "CSBV,BYDDES", "RYD5,RMTDES"})
    void resolve_shouldReturnNo_whenServicesDoNotContainFrdm(String services) {
        assertThat(FrdmFlagSupport.resolve(services)).isEqualTo(FRDM_FLAG_NO);
    }

    @Test
    void resolve_shouldReturnNo_whenServicesContainOnlyWhitespaceTokens() {
        assertThat(FrdmFlagSupport.resolve("MANU,,BYDDES")).isEqualTo(FRDM_FLAG_NO);
    }
}
