package com.customcharge.dashboard.service.impl;

import com.customcharge.dashboard.dto.AccountAuditHistoryCommitDto;
import com.customcharge.dashboard.dto.AccountAuditHistoryRequestDto;
import com.customcharge.persistencelayer.audit.model.AuditCommitBlock;
import com.customcharge.persistencelayer.audit.model.AuditQueryRequest;
import com.customcharge.persistencelayer.audit.model.AuditStateChangeEntry;
import com.customcharge.persistencelayer.audit.service.PersistenceAuditQueryEngine;
import com.customcharge.persistencelayer.config.javers.AuditCommitProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountAuditServiceImplTest {

    @Mock
    private PersistenceAuditQueryEngine auditQueryEngine;

    @Mock
    private AccountAuditSnapshotEnricher snapshotEnricher;

    @Mock
    private ObjectProvider<AccountAuditServiceImpl> selfProvider;

    @InjectMocks
    private AccountAuditServiceImpl service;

    @BeforeEach
    void wireSelfProvider() {
        when(selfProvider.getObject()).thenReturn(service);
    }

    @Test
    void getAuditHistory_shouldRequireAccountOrEntity() {
        AccountAuditHistoryRequestDto request = new AccountAuditHistoryRequestDto();

        assertThatThrownBy(() -> service.getAuditHistory(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accountId or entityCode");
    }

    @Test
    void getAuditHistory_shouldQueryByAccountId() {
        UUID accountId = UUID.randomUUID();
        AccountAuditHistoryRequestDto request = new AccountAuditHistoryRequestDto();
        request.setAccountId(accountId);
        request.setPage(0);
        request.setSize(10);

        when(auditQueryEngine.getAuditHistory(any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        service.getAuditHistory(request);

        ArgumentCaptor<AuditQueryRequest> captor = ArgumentCaptor.forClass(AuditQueryRequest.class);
        verify(auditQueryEngine).getAuditHistory(captor.capture());
        assertThat(captor.getValue().getGroupName()).isEqualTo(AuditCommitProperties.GROUP_ACCOUNT_AR_AP);
        assertThat(captor.getValue().getCommitPropertyFilters())
                .containsEntry(AuditCommitProperties.ACCOUNT_ID, accountId.toString());
    }

    @Test
    void getAuditHistory_shouldQueryByEntityCode() {
        AccountAuditHistoryRequestDto request = new AccountAuditHistoryRequestDto();
        request.setEntityCode(" dxb ");
        request.setPage(0);
        request.setSize(5);

        AuditCommitBlock block = new AuditCommitBlock();
        block.setCommitId("c-1");
        when(auditQueryEngine.getAuditHistory(any()))
                .thenReturn(new PageImpl<>(List.of(block), PageRequest.of(0, 5), 1));

        var result = service.getAuditHistory(request);

        ArgumentCaptor<AuditQueryRequest> captor = ArgumentCaptor.forClass(AuditQueryRequest.class);
        verify(auditQueryEngine).getAuditHistory(captor.capture());
        assertThat(captor.getValue().getCommitPropertyFilters())
                .containsEntry(AuditCommitProperties.ENTITY_CODE, "DXB");
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void getAuditHistory_shouldSanitizeStateChangePayload() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode currentState = objectMapper.createObjectNode();
        currentState.put("id", "strip");
        currentState.put("name", "Account");

        AuditStateChangeEntry entry = new AuditStateChangeEntry();
        entry.setType("UPDATE");
        entry.setCurrentState(currentState);

        AuditCommitBlock block = new AuditCommitBlock();
        block.setCommitId("c-2");
        block.setManagedType("Account");
        block.setStateChangeData(List.of(entry));

        AccountAuditHistoryRequestDto request = new AccountAuditHistoryRequestDto();
        request.setAccountId(UUID.randomUUID());

        when(auditQueryEngine.getAuditHistory(any()))
                .thenReturn(new PageImpl<>(List.of(block), PageRequest.of(0, 10), 1));
        when(snapshotEnricher.enrich(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AccountAuditHistoryCommitDto dto = service.getAuditHistory(request).getContent().getFirst();

        assertThat(dto.getStateChangeData().getCurrentState().has("id")).isFalse();
        assertThat(dto.getStateChangeData().getCurrentState().get("name").asText()).isEqualTo("Account");
    }

    @Test
    void getAuditHistory_shouldMapAllStateChangeEntriesWithSharedCommitMetadata() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        AuditStateChangeEntry first = new AuditStateChangeEntry();
        first.setType("UPDATE");
        first.setCurrentState(objectMapper.createObjectNode().put("name", "Account"));

        AuditStateChangeEntry second = new AuditStateChangeEntry();
        second.setType("INITIAL");
        second.setCurrentState(objectMapper.createObjectNode().put("name", "ServiceCharge"));

        AuditCommitBlock block = new AuditCommitBlock();
        block.setCommitId("c-3");
        block.setManagedType("Account");
        block.setAuthor("tester");
        block.setStateChangeData(List.of(first, second));

        AccountAuditHistoryRequestDto request = new AccountAuditHistoryRequestDto();
        request.setAccountId(UUID.randomUUID());

        when(auditQueryEngine.getAuditHistory(any()))
                .thenReturn(new PageImpl<>(List.of(block), PageRequest.of(0, 10), 1));
        when(snapshotEnricher.enrich(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<AccountAuditHistoryCommitDto> content = service.getAuditHistory(request).getContent();

        assertThat(content).hasSize(2);
        assertThat(content).allSatisfy(dto -> {
            assertThat(dto.getCommitId()).isEqualTo("c-3");
            assertThat(dto.getManagedType()).isEqualTo("Account");
            assertThat(dto.getAuthor()).isEqualTo("tester");
        });
        assertThat(content.get(0).getStateChangeData().getType()).isEqualTo("UPDATE");
        assertThat(content.get(1).getStateChangeData().getType()).isEqualTo("INITIAL");
    }
}
