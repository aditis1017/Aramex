package com.customcharge.global_settings.export;

import com.customcharge.global_settings.rules.dto.GlobalSettingAuditHistoryCommitDto;
import com.customcharge.global_settings.rules.dto.GlobalSettingAuditStateChangeDto;
import com.customcharge.global_settings.rules.service.GlobalSettingService;
import com.customcharge.persistencelayer.export.model.ExportContext;
import com.customcharge.persistencelayer.export.model.ExportFormat;
import com.customcharge.persistencelayer.export.model.ExportRow;
import com.customcharge.persistencelayer.exception.BadRequestException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.OptionalLong;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalSettingExportPipelineTest {

    @Mock
    private GlobalSettingService globalSettingService;

    private GlobalSettingExportPipeline pipeline;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        pipeline = new GlobalSettingExportPipeline(globalSettingService, objectMapper);
    }

    @Test
    void exportType_shouldReturnConstant() {
        assertThat(pipeline.exportType()).isEqualTo("GLOBAL_SETTING");
    }

    @Test
    void columnNames_shouldReturnConfiguredColumns() {
        assertThat(pipeline.columnNames()).hasSize(9);
    }

    @Test
    void fileNamePrefix_shouldReturnConfiguredPrefix() {
        assertThat(pipeline.fileNamePrefix()).isEqualTo("Global-Settings-History");
    }

    @Test
    void resolveExportTotalRecords_shouldThrow_whenFilterSpecMissing() {
        ExportContext context = contextWithFilter("", 100L);

        assertThatThrownBy(() -> pipeline.resolveExportTotalRecords(context))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void resolveExportTotalRecords_shouldReturnCount_whenAuditRowsExist() throws Exception {
        ExportContext context = contextWithFilter(
                "{\"fromDate\":\"2026-01-01T00:00:00Z\",\"toDate\":\"2026-01-31T23:59:59.999Z\"}", 100L);

        GlobalSettingAuditHistoryCommitDto commit = auditCommit();
        when(globalSettingService.getAuditHistory(any())).thenReturn(new PageImpl<>(List.of(commit)));

        OptionalLong total = pipeline.resolveExportTotalRecords(context);

        assertThat(total).hasValue(1);
    }

    @Test
    void resolveExportTotalRecords_shouldReturnEmpty_whenNoAuditRows() throws Exception {
        ExportContext context = contextWithFilter(
                "{\"fromDate\":\"2026-01-01T00:00:00Z\",\"toDate\":\"2026-01-31T23:59:59.999Z\"}", 1L);
        when(globalSettingService.getAuditHistory(any())).thenReturn(new PageImpl<>(List.of()));

        assertThat(pipeline.resolveExportTotalRecords(context)).isEmpty();
    }

    @Test
    void resolveExportTotalRecords_shouldThrow_whenFilterSpecInvalidJson() {
        ExportContext context = contextWithFilter("{invalid", 1L);

        assertThatThrownBy(() -> pipeline.resolveExportTotalRecords(context))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid filterSpec JSON");
    }

    @Test
    void resolveExportTotalRecords_shouldThrow_whenRequiredDateMissing() {
        ExportContext context = contextWithFilter("{\"toDate\":\"2026-01-31T23:59:59.999Z\"}", 1L);

        assertThatThrownBy(() -> pipeline.resolveExportTotalRecords(context))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("fromDate");
    }

    @Test
    void resolveExportTotalRecords_shouldPaginateAcrossAuditPages() throws Exception {
        ExportContext context = contextWithFilter(
                "{\"fromDate\":\"2026-01-01T00:00:00Z\",\"toDate\":\"2026-01-31T23:59:59.999Z\",\"groupName\":\"custom\"}", 1L);
        when(globalSettingService.getAuditHistory(any()))
                .thenReturn(new PageImpl<>(List.of(auditCommit()), org.springframework.data.domain.PageRequest.of(0, 1), 2))
                .thenReturn(new PageImpl<>(List.of(auditCommit("UPDATE", "DDP", "author@aramex.com")), org.springframework.data.domain.PageRequest.of(1, 1), 2));

        assertThat(pipeline.resolveExportTotalRecords(context)).hasValue(2);
    }

    @Test
    void createReader_shouldSkipInvalidAuditBlocks() throws Exception {
        ExportContext context = contextWithFilter(
                "{\"fromDate\":\"2026-01-01T00:00:00Z\",\"toDate\":\"2026-01-31T23:59:59.999Z\"}", 1L);
        GlobalSettingAuditHistoryCommitDto missingChange = new GlobalSettingAuditHistoryCommitDto();
        missingChange.setStateChangeData(new GlobalSettingAuditStateChangeDto());
        java.util.List<GlobalSettingAuditHistoryCommitDto> commits = new java.util.ArrayList<>();
        commits.add(null);
        commits.add(missingChange);
        commits.add(auditCommit());
        when(globalSettingService.getAuditHistory(any()))
                .thenReturn(new PageImpl<>(commits));

        var reader = pipeline.createReader(context);
        assertThat(reader.read()).isNotNull();
        assertThat(reader.read()).isNull();
        assertThatCode(reader::close).doesNotThrowAnyException();
    }

    @Test
    void createProcessor_shouldMapAuditExportItemToExportRow() throws Exception {
        ExportContext context = contextWithFilter(
                "{\"fromDate\":\"2026-01-01T00:00:00Z\",\"toDate\":\"2026-01-31T23:59:59.999Z\"}", 100L);
        var processor = pipeline.createProcessor(context);
        Object item = createAuditExportItemViaReader(context);

        ExportRow row = processor.process(item);

        assertThat(row.cells()).hasSize(9);
        assertThat(row.cells().get(4)).isEqualTo("Create");
        assertThat(row.cells().get(2)).isEqualTo("DDU");
        assertThat(row.cells().get(3)).isEqualTo("System");
    }

    @Test
    void createProcessor_shouldUseCategorySubSectionAndBlankTimestamp() throws Exception {
        ExportContext context = contextWithFilter(
                "{\"fromDate\":\"2026-01-01T00:00:00Z\",\"toDate\":\"2026-01-31T23:59:59.999Z\"}", 1L);
        GlobalSettingAuditHistoryCommitDto commit = auditCommit("UPDATE", "", "author@aramex.com");
        commit.setCommitDate(null);
        ObjectNode current = (ObjectNode) commit.getStateChangeData().getCurrentState();
        current.remove("sequenceNumber");
        current.put("sequenceNumber", "seq-text");
        when(globalSettingService.getAuditHistory(any())).thenReturn(new PageImpl<>(List.of(commit)));

        var reader = pipeline.createReader(context);
        ExportRow row = pipeline.createProcessor(context).process(reader.read());

        assertThat(row.cells().get(0)).isEmpty();
        assertThat(row.cells().get(2)).isEqualTo("Category");
        assertThat(row.cells().get(3)).isEqualTo("author@aramex.com");
        assertThat(row.cells().get(4)).isEqualTo("UPDATE");
        assertThat(row.cells().get(5)).isEqualTo("seq-text");
    }

    private Object createAuditExportItemViaReader(ExportContext context) throws Exception {
        when(globalSettingService.getAuditHistory(any())).thenReturn(new PageImpl<>(List.of(auditCommit())));

        var reader = pipeline.createReader(context);
        return reader.read();
    }

    private ExportContext contextWithFilter(String filterSpec, long chunkSize) {
        return new ExportContext(
                UUID.randomUUID(),
                "GLOBAL_SETTING",
                ExportFormat.CSV,
                Path.of("export.csv"),
                chunkSize,
                filterSpec,
                "[]",
                "export.csv");
    }

    private GlobalSettingAuditHistoryCommitDto auditCommit() {
        return auditCommit("INITIAL", "DDU", "unknown");
    }

    private GlobalSettingAuditHistoryCommitDto auditCommit(String type, String targetValue, String author) {
        GlobalSettingAuditHistoryCommitDto commit = new GlobalSettingAuditHistoryCommitDto();
        commit.setAuthor(author);
        commit.setCommitDate(Instant.parse("2026-01-15T10:00:00Z"));
        GlobalSettingAuditStateChangeDto state = new GlobalSettingAuditStateChangeDto();
        state.setType(type);
        ObjectNode current = objectMapper.createObjectNode();
        current.put("targetValue", targetValue);
        current.put("sequenceNumber", 2);
        state.setCurrentState(current);
        ObjectNode preState = objectMapper.createObjectNode();
        preState.put("destination", "RUH");
        state.setPreState(preState);
        ObjectNode change = objectMapper.createObjectNode();
        change.put("destination", "DXB");
        state.setChangeParameter(change);
        commit.setStateChangeData(state);
        return commit;
    }
}
