package com.customcharge.fileorchestration.service.impl;

import com.customcharge.fileorchestration.config.BatchFlowProperties;
import com.customcharge.fileorchestration.support.FileOrchestrationTestDataFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConfiguredFileTypeSupportServiceTest {

    @Test
    void supports_shouldMatchConfiguredTypesCaseInsensitively() {
        BatchFlowProperties properties = FileOrchestrationTestDataFactory.batchFlowProperties();
        ConfiguredFileTypeSupportService service = new ConfiguredFileTypeSupportService(properties);

        assertThat(service.supports("csv")).isTrue();
        assertThat(service.supports("XLSX")).isTrue();
        assertThat(service.supports("PDF")).isFalse();
        assertThat(service.supports(null)).isFalse();
    }
}
