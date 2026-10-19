package com.customcharge.global_settings.export;

import com.customcharge.global_settings.rules.service.GlobalSettingService;
import com.customcharge.persistencelayer.export.registry.ExportPipeline;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GlobalSettingExportPipelineConfigurationTest {

    @Test
    void globalSettingsExportPipeline_shouldCreateBean() {
        GlobalSettingExportPipelineConfiguration configuration = new GlobalSettingExportPipelineConfiguration();

        ExportPipeline pipeline = configuration.globalSettingsExportPipeline(
                mock(GlobalSettingService.class),
                new ObjectMapper());

        assertThat(pipeline).isInstanceOf(GlobalSettingExportPipeline.class);
        assertThat(pipeline.exportType()).isEqualTo(GlobalSettingExportPipeline.EXPORT_TYPE);
    }
}
