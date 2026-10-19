package com.customcharge.persistencelayer.config.datasource;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class DataSourceConfigurationTest {

    @Test
    void isReadPoolConfigured_shouldBeFalse_whenReadUrlMissing() throws Exception {
        DatasourceProperties properties = new DatasourceProperties();
        properties.setUrl("jdbc:postgresql://localhost/db");

        assertThat(invokeIsReadPoolConfigured(properties)).isFalse();
    }

    @Test
    void isReadPoolConfigured_shouldBeTrue_whenReadUrlPresent() throws Exception {
        DatasourceProperties properties = new DatasourceProperties();
        properties.getRead().setUrl("jdbc:postgresql://reader/db");

        assertThat(invokeIsReadPoolConfigured(properties)).isTrue();
    }

    @Test
    void routingDefaults_shouldDisableRoutingAndEnableFallback() {
        DatasourceProperties properties = new DatasourceProperties();

        assertThat(properties.getRouting().isEnabled()).isFalse();
        assertThat(properties.getRouting().isFallbackToWrite()).isTrue();
    }

    private static boolean invokeIsReadPoolConfigured(DatasourceProperties properties) throws Exception {
        Method method = DataSourceConfiguration.class.getDeclaredMethod("isReadPoolConfigured", DatasourceProperties.class);
        method.setAccessible(true);
        return (boolean) method.invoke(null, properties);
    }
}
