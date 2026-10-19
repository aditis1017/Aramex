package com.customcharge.global_settings.controller;

import com.customcharge.persistencelayer.dto.response.HealthResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HealthControllerTest {

    private HealthController healthController;

    @BeforeEach
    void setUp() {
        Properties properties = new Properties();
        properties.setProperty("version", "0.1.0");
        properties.setProperty("name", "globalSettings");
        BuildProperties buildProperties = new BuildProperties(properties);
        @SuppressWarnings("unchecked")
        ObjectProvider<BuildProperties> buildPropertiesProvider = mock(ObjectProvider.class);
        when(buildPropertiesProvider.getIfAvailable()).thenReturn(buildProperties);

        healthController = new HealthController("globalSettings", buildPropertiesProvider);
    }

    @Test
    void getHealth_shouldReturnUpStatus() {
        HealthResponse response = healthController.getHealth();

        assertThat(response.getStatus()).isEqualTo("UP");
        assertThat(response.getApplication()).isEqualTo("globalSettings");
        assertThat(response.getApplicationVersion()).isEqualTo("0.1.0");
        assertThat(response.getTimestamp()).isNotNull();
    }
}
