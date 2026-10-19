package com.customcharge.dashboard.service.impl;

import com.customcharge.dashboard.dto.ShipmentAuditHistoryCommitDto;
import com.customcharge.dashboard.dto.ShipmentAuditHistoryRequestDto;
import com.customcharge.persistencelayer.audit.model.AuditCommitBlock;
import com.customcharge.persistencelayer.audit.model.AuditQueryRequest;
import com.customcharge.persistencelayer.audit.model.AuditStateChangeEntry;
import com.customcharge.persistencelayer.audit.service.PersistenceAuditQueryEngine;
import com.customcharge.persistencelayer.config.javers.AuditCommitProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShipmentAuditServiceImplTest {

    @Mock
    private PersistenceAuditQueryEngine auditQueryEngine;

    @InjectMocks
    private ShipmentAuditServiceImpl service;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void getAuditHistory_shouldRejectBlankHawbNumber() {
        ShipmentAuditHistoryRequestDto request = new ShipmentAuditHistoryRequestDto();
        request.setHawbNumber("  ");

        assertThatThrownBy(() -> service.getAuditHistory(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("hawbNumber");
    }

    @Test
    void getAuditHistory_shouldQueryByGroupAndHawbNumber() throws Exception {
        ShipmentAuditHistoryRequestDto request = new ShipmentAuditHistoryRequestDto();
        request.setHawbNumber(" AWB-1001 ");
        request.setPage(0);
        request.setSize(10);
        request.setManagedType("com.customcharge.persistencelayer.entities.Charge");

        AuditCommitBlock block = new AuditCommitBlock();
        block.setCommitId("commit-1");
        block.setManagedType("com.customcharge.persistencelayer.entities.Charge");
        block.setAuthor("tester");
        block.setCommitDate(Instant.parse("2026-05-01T10:00:00Z"));

        ObjectNode currentState = objectMapper.createObjectNode();
        currentState.put("chargeCode", "DTC");
        currentState.put("id", "should-be-stripped");
        AuditStateChangeEntry entry = new AuditStateChangeEntry();
        entry.setType("INITIAL");
        entry.setCurrentState(currentState);
        block.setStateChangeData(List.of(entry));

        when(auditQueryEngine.getAuditHistory(any())).thenReturn(
                new PageImpl<>(List.of(block), PageRequest.of(0, 10), 1));

        var result = service.getAuditHistory(request);

        ArgumentCaptor<AuditQueryRequest> captor = ArgumentCaptor.forClass(AuditQueryRequest.class);
        verify(auditQueryEngine).getAuditHistory(captor.capture());
        AuditQueryRequest query = captor.getValue();
        assertThat(query.getGroupName()).isEqualTo(AuditCommitProperties.GROUP_SHIPMENT_CHARGE);
        assertThat(query.getCommitPropertyFilters())
                .containsEntry(AuditCommitProperties.HAWB_NUMBER, "AWB-1001");
        assertThat(query.getManagedType())
                .hasValue("com.customcharge.persistencelayer.entities.Charge");

        assertThat(result.getTotalElements()).isEqualTo(1);
        ShipmentAuditHistoryCommitDto dto = result.getContent().getFirst();
        assertThat(dto.getCommitId()).isEqualTo("commit-1");
        assertThat(dto.getStateChangeData().getCurrentState().has("id")).isFalse();
        assertThat(dto.getStateChangeData().getCurrentState().get("chargeCode").asText()).isEqualTo("DTC");
    }
}
