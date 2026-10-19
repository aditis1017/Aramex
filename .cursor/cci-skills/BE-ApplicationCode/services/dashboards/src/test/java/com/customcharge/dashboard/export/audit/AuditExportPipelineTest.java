package com.customcharge.dashboard.export.audit;

import com.customcharge.dashboard.service.AccountAuditService;
import com.customcharge.dashboard.service.ShipmentAuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AuditExportPipelineTest {

    @Test
    void pipelines_shouldExposeExportTypes() {
        AccountAuditExportPipeline account = new AccountAuditExportPipeline(
                mock(AccountAuditService.class), new ObjectMapper());
        ShipmentAuditExportPipeline shipment = new ShipmentAuditExportPipeline(
                mock(ShipmentAuditService.class), new ObjectMapper());

        assertThat(account.exportType()).isEqualTo("ACCOUNT_AUDIT");
        assertThat(shipment.exportType()).isEqualTo("SHIPMENT_AUDIT");
        assertThat(account.columnNames()).hasSize(7);
        assertThat(shipment.columnNames()).containsExactly(
                "Tracking Number",
                "Timestamp",
                "Section",
                "User",
                "Action",
                "Field Name",
                "Old Value",
                "New Value");
    }
}
