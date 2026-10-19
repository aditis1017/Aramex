package com.customcharge.chargescalculator.util;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RecordMapSupportTest {

    @Test
    void lowercaseKeys_shouldReturnEmptyMap_whenNull() {
        assertThat(RecordMapSupport.lowercaseKeys(null)).isEmpty();
    }

    @Test
    void lowercaseKeys_shouldReturnEmptyMap_whenEmpty() {
        assertThat(RecordMapSupport.lowercaseKeys(Map.of())).isEmpty();
    }

    @Test
    void lowercaseKeys_shouldLowercaseKeysAndPreserveValues() {
        Map<String, String> input = new HashMap<>();
        input.put("AWB", "123");
        input.put("Saddad", "S1");

        assertThat(RecordMapSupport.lowercaseKeys(input))
                .containsEntry("awb", "123")
                .containsEntry("saddad", "S1");
    }

    @Test
    void lowercaseKeys_shouldSkipNullKeys() {
        Map<String, String> input = new HashMap<>();
        input.put(null, "ignored");
        input.put("awb", "123");

        assertThat(RecordMapSupport.lowercaseKeys(input)).containsExactly(Map.entry("awb", "123"));
    }
}
