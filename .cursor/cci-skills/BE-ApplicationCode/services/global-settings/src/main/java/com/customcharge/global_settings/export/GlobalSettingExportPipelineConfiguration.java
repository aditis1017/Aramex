package com.customcharge.global_settings.export;

import com.customcharge.global_settings.rules.service.GlobalSettingService;
import com.customcharge.persistencelayer.export.registry.ExportPipeline;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GlobalSettingExportPipelineConfiguration {

    @Bean
    public ExportPipeline globalSettingsExportPipeline(
            GlobalSettingService globalSettingService,
            ObjectMapper objectMapper) {
        return new GlobalSettingExportPipeline(globalSettingService, objectMapper);
    }
}
