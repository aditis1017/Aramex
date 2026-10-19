package com.customcharge.dashboard.export.audit;

import com.customcharge.dashboard.service.AccountAuditService;
import com.customcharge.dashboard.service.ShipmentAuditService;
import com.customcharge.persistencelayer.export.registry.ExportPipeline;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuditExportPipelineConfiguration {

    @Bean
    public ExportPipeline accountAuditExportPipeline(
            AccountAuditService accountAuditService,
            ObjectMapper objectMapper) {
        return new AccountAuditExportPipeline(accountAuditService, objectMapper);
    }

    @Bean
    public ExportPipeline shipmentAuditExportPipeline(
            ShipmentAuditService shipmentAuditService,
            ObjectMapper objectMapper) {
        return new ShipmentAuditExportPipeline(shipmentAuditService, objectMapper);
    }
}
