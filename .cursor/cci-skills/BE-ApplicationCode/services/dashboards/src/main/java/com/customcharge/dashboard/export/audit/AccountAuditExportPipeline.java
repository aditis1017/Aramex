package com.customcharge.dashboard.export.audit;

import com.customcharge.dashboard.constants.AuditExportConstants;
import com.customcharge.dashboard.constants.ExportConstants;
import com.customcharge.dashboard.dto.AccountAuditHistoryCommitDto;
import com.customcharge.dashboard.dto.AccountAuditHistoryRequestDto;
import com.customcharge.dashboard.service.AccountAuditService;
import com.customcharge.persistencelayer.export.model.ExportContext;
import com.customcharge.persistencelayer.export.model.ExportRow;
import com.customcharge.persistencelayer.exception.BadRequestException;
import com.customcharge.persistencelayer.export.registry.ExportItemProcessor;
import com.customcharge.persistencelayer.export.registry.ExportItemReader;
import com.customcharge.persistencelayer.export.registry.ExportPipeline;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Locale;
import java.util.OptionalLong;
import java.util.function.Function;

public class AccountAuditExportPipeline implements ExportPipeline {

    private final AccountAuditService accountAuditService;
    private final ObjectMapper objectMapper;

    public AccountAuditExportPipeline(AccountAuditService accountAuditService, ObjectMapper objectMapper) {
        this.accountAuditService = accountAuditService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String exportType() {
        return AuditExportConstants.EXPORT_TYPE_ACCOUNT_AUDIT;
    }

    @Override
    public ExportItemReader createReader(ExportContext context) throws Exception {
        return AuditExportSupport.streamingReader(pageLoader(context), null);
    }

    @Override
    public OptionalLong resolveExportTotalRecords(ExportContext context) throws Exception {
        long total = AuditExportSupport.countFlattenedRows(pageLoader(context));
        return total > 0 ? OptionalLong.of(total) : OptionalLong.empty();
    }

    @Override
    public List<String> columnNames() {
        return AuditExportSupport.COLUMN_NAMES;
    }

    @Override
    public String fileNamePrefix() {
        return AuditExportConstants.FILE_NAME_PREFIX_ACCOUNT;
    }

    @Override
    public ExportItemProcessor createProcessor(ExportContext context) {
        return item -> {
            AuditExportRow row = (AuditExportRow) item;
            String ts = row.changedOn() == null
                    ? ""
                    : ExportConstants.EXPORT_DATE_TIME_FORMAT.format(row.changedOn());
            return new ExportRow(List.of(
                    ts,
                    row.section(),
                    row.changedBy(),
                    row.operation(),
                    row.fieldName(),
                    row.oldValue(),
                    row.newValue()));
        };
    }

    private Function<AuditExportSupport.PageRequest, Page<AccountAuditHistoryCommitDto>> pageLoader(
            ExportContext context) throws Exception {
        int pageSize = context.pageSize();
        AccountAuditHistoryRequestDto template = parseFilter(context);
        return page -> {
            template.setPage(page.page());
            template.setSize(pageSize);
            return accountAuditService.getAuditHistory(template);
        };
    }

    private AccountAuditHistoryRequestDto parseFilter(ExportContext context) throws Exception {
        String filterSpec = context.filterSpec();
        if (filterSpec == null || filterSpec.isBlank()) {
            throw new BadRequestException(AuditExportConstants.FILTER_SPEC_REQUIRED_ACCOUNT);
        }
        JsonNode root = objectMapper.readTree(filterSpec);
        AccountAuditHistoryRequestDto request = new AccountAuditHistoryRequestDto();
        request.setFromDate(AuditExportSupport.requireInstant(root, AuditExportConstants.FILTER_KEY_FROM_DATE));
        request.setToDate(AuditExportSupport.requireInstant(root, AuditExportConstants.FILTER_KEY_TO_DATE));
        request.setAccountId(AuditExportSupport.optionalUuid(root, AuditExportConstants.FILTER_KEY_ACCOUNT_ID));
        String entityCode = AuditExportSupport.optionalText(root, AuditExportConstants.FILTER_KEY_ENTITY_CODE);
        request.setEntityCode(entityCode != null ? entityCode.toUpperCase(Locale.ROOT) : null);
        request.setManagedType(AuditExportSupport.optionalText(root, AuditExportConstants.FILTER_KEY_MANAGED_TYPE));
        return request;
    }
}
