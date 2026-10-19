package com.customcharge.dashboard.export.audit;

import com.customcharge.dashboard.constants.AuditExportConstants;
import com.customcharge.dashboard.constants.ExportConstants;
import com.customcharge.dashboard.dto.AccountAuditHistoryCommitDto;
import com.customcharge.dashboard.dto.ShipmentAuditHistoryCommitDto;
import com.customcharge.dashboard.dto.ShipmentAuditHistoryRequestDto;
import com.customcharge.dashboard.service.ShipmentAuditService;
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
import java.util.OptionalLong;
import java.util.function.Function;

public class ShipmentAuditExportPipeline implements ExportPipeline {

    private final ShipmentAuditService shipmentAuditService;
    private final ObjectMapper objectMapper;

    public ShipmentAuditExportPipeline(ShipmentAuditService shipmentAuditService, ObjectMapper objectMapper) {
        this.shipmentAuditService = shipmentAuditService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String exportType() {
        return AuditExportConstants.EXPORT_TYPE_SHIPMENT_AUDIT;
    }

    @Override
    public ExportItemReader createReader(ExportContext context) throws Exception {
        ShipmentAuditHistoryRequestDto template = parseFilter(context);
        return AuditExportSupport.streamingReader(pageLoader(context, template), template.getHawbNumber());
    }

    @Override
    public OptionalLong resolveExportTotalRecords(ExportContext context) throws Exception {
        ShipmentAuditHistoryRequestDto template = parseFilter(context);
        long total = AuditExportSupport.countFlattenedRows(pageLoader(context, template));
        return total > 0 ? OptionalLong.of(total) : OptionalLong.empty();
    }

    @Override
    public List<String> columnNames() {
        return AuditExportSupport.SHIPMENT_COLUMN_NAMES;
    }

    @Override
    public String fileNamePrefix() {
        return AuditExportConstants.FILE_NAME_PREFIX_SHIPMENT;
    }

    @Override
    public ExportItemProcessor createProcessor(ExportContext context) {
        return item -> {
            AuditExportRow row = (AuditExportRow) item;
            String ts = row.changedOn() == null
                    ? ""
                    : ExportConstants.EXPORT_DATE_TIME_FORMAT.format(row.changedOn());
            return new ExportRow(List.of(
                    row.trackingNumber() != null ? row.trackingNumber() : "",
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
            ExportContext context,
            ShipmentAuditHistoryRequestDto template) {
        int pageSize = context.pageSize();
        return page -> {
            template.setPage(page.page());
            template.setSize(pageSize);
            Page<ShipmentAuditHistoryCommitDto> shipmentPage = shipmentAuditService.getAuditHistory(template);
            return shipmentPage.map(ShipmentAuditExportPipeline::toAccountCommit);
        };
    }

    private static AccountAuditHistoryCommitDto toAccountCommit(ShipmentAuditHistoryCommitDto source) {
        AccountAuditHistoryCommitDto target = new AccountAuditHistoryCommitDto();
        target.setCommitId(source.getCommitId());
        target.setManagedType(source.getManagedType());
        target.setAuthor(source.getAuthor());
        target.setCommitDate(source.getCommitDate());
        target.setStateChangeData(source.getStateChangeData());
        return target;
    }

    private ShipmentAuditHistoryRequestDto parseFilter(ExportContext context) throws Exception {
        String filterSpec = context.filterSpec();
        if (filterSpec == null || filterSpec.isBlank()) {
            throw new BadRequestException(AuditExportConstants.FILTER_SPEC_REQUIRED);
        }
        JsonNode root = objectMapper.readTree(filterSpec);
        String hawbNumber = AuditExportSupport.optionalText(root, AuditExportConstants.FILTER_KEY_HAWB_NUMBER);
        if (hawbNumber == null || hawbNumber.isBlank()) {
            throw new BadRequestException(AuditExportConstants.FILTER_SPEC_HAWB_NUMBER_REQUIRED);
        }
        ShipmentAuditHistoryRequestDto request = new ShipmentAuditHistoryRequestDto();
        request.setFromDate(AuditExportSupport.requireInstant(root, AuditExportConstants.FILTER_KEY_FROM_DATE));
        request.setToDate(AuditExportSupport.requireInstant(root, AuditExportConstants.FILTER_KEY_TO_DATE));
        request.setHawbNumber(hawbNumber);
        request.setManagedType(AuditExportSupport.optionalText(root, AuditExportConstants.FILTER_KEY_MANAGED_TYPE));
        return request;
    }
}
