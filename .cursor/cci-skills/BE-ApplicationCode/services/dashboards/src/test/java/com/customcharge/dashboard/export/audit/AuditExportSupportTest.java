package com.customcharge.dashboard.export.audit;

import com.customcharge.dashboard.dto.AccountAuditHistoryCommitDto;
import com.customcharge.dashboard.dto.AccountAuditStateChangeDto;
import com.customcharge.persistencelayer.exception.BadRequestException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.customcharge.persistencelayer.export.registry.ExportItemReader;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditExportSupportTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void requireInstant_shouldRejectMissingValue() {
        ObjectNode root = objectMapper.createObjectNode();

        assertThatThrownBy(() -> AuditExportSupport.requireInstant(root, "fromDate"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void optionalUuid_shouldParseValue() {
        ObjectNode root = objectMapper.createObjectNode();
        UUID id = UUID.randomUUID();
        root.put("accountId", id.toString());

        assertThat(AuditExportSupport.optionalUuid(root, "accountId")).isEqualTo(id);
        assertThat(AuditExportSupport.optionalUuid(root, "missing")).isNull();
    }

    @Test
    void streamingReader_shouldExpandCommitChanges() throws Exception {
        Page<AccountAuditHistoryCommitDto> page = new PageImpl<>(List.of(sampleCommit("UPDATE", "tester")));
        ExportItemReader reader = AuditExportSupport.streamingReader(request -> page, null);

        AuditExportRow row = (AuditExportRow) reader.read();

        assertThat(row.fieldName()).isEqualTo("status");
        assertThat(row.oldValue()).isEqualTo("old");
        assertThat(row.newValue()).isEqualTo("new");
        assertThat(row.changedBy()).isEqualTo("tester");
        assertThat(row.operation()).isEqualTo("UPDATE");
        assertThat(reader.read()).isNull();
    }

    @Test
    void countFlattenedRows_shouldMatchStreamingOutput() {
        Page<AccountAuditHistoryCommitDto> page = new PageImpl<>(List.of(sampleCommit("INITIAL", "unknown")));

        assertThat(AuditExportSupport.countFlattenedRows(request -> page)).isEqualTo(1);
    }

    @Test
    void streamingReader_shouldUseSystemAuthorForUnknown() throws Exception {
        Page<AccountAuditHistoryCommitDto> page = new PageImpl<>(List.of(sampleCommit("INITIAL", "unknown")));
        ExportItemReader reader = AuditExportSupport.streamingReader(request -> page, null);

        AuditExportRow row = (AuditExportRow) reader.read();

        assertThat(row.changedBy()).isEqualTo("System");
        assertThat(row.operation()).isEqualTo("Create");
    }

    @Test
    void requireInstant_shouldParseIsoValue() {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("fromDate", "2026-01-01T00:00:00Z");

        assertThat(AuditExportSupport.requireInstant(root, "fromDate"))
                .isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    void streamingReader_shouldIncludeTrackingNumber_whenProvided() throws Exception {
        Page<AccountAuditHistoryCommitDto> page = new PageImpl<>(List.of(sampleCommit("UPDATE", "tester")));
        ExportItemReader reader = AuditExportSupport.streamingReader(request -> page, "AWB-1001");

        AuditExportRow row = (AuditExportRow) reader.read();

        assertThat(row.trackingNumber()).isEqualTo("AWB-1001");
    }

    @Test
    void optionalText_shouldTrimValue() {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("entityCode", " DXB ");

        assertThat(AuditExportSupport.optionalText(root, "entityCode")).isEqualTo("DXB");
    }

    private static AccountAuditHistoryCommitDto sampleCommit(String type, String author) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode changeParameter = mapper.createObjectNode();
        changeParameter.put("status", "updated");
        ObjectNode preState = mapper.createObjectNode();
        preState.put("status", "old");
        ObjectNode currentState = mapper.createObjectNode();
        currentState.put("status", "new");

        AccountAuditStateChangeDto stateChange = new AccountAuditStateChangeDto();
        stateChange.setType(type);
        stateChange.setChangeParameter(changeParameter);
        stateChange.setPreState(preState);
        stateChange.setCurrentState(currentState);

        AccountAuditHistoryCommitDto commit = new AccountAuditHistoryCommitDto();
        commit.setCommitDate(Instant.parse("2026-01-01T00:00:00Z"));
        commit.setAuthor(author);
        commit.setManagedType("Account");
        commit.setStateChangeData(stateChange);
        return commit;
    }
}
