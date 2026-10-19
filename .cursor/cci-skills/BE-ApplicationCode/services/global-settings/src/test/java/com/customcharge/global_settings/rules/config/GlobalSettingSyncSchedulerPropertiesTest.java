package com.customcharge.global_settings.rules.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalSettingSyncSchedulerPropertiesTest {

    @Test
    void shouldExposeDefaultValues() {
        GlobalSettingSyncSchedulerProperties properties = new GlobalSettingSyncSchedulerProperties();

        assertThat(properties.isOnStartupEnabled()).isTrue();
        assertThat(properties.isScheduledEnabled()).isTrue();
        assertThat(properties.getCron()).isEqualTo("1 0 0 * * ?");
        assertThat(properties.getZone()).isEqualTo("UTC");
    }

    @Test
    void shouldAllowOverridingValues() {
        GlobalSettingSyncSchedulerProperties properties = new GlobalSettingSyncSchedulerProperties();
        properties.setOnStartupEnabled(false);
        properties.setScheduledEnabled(false);
        properties.setCron("0 0 12 * * ?");
        properties.setZone("Asia/Dubai");

        assertThat(properties.isOnStartupEnabled()).isFalse();
        assertThat(properties.isScheduledEnabled()).isFalse();
        assertThat(properties.getCron()).isEqualTo("0 0 12 * * ?");
        assertThat(properties.getZone()).isEqualTo("Asia/Dubai");
    }
}
