package com.customcharge.dashboard.export;

import com.customcharge.dashboard.constants.ExportHeaderConstants;
import com.customcharge.dashboard.dto.EntityChargeGridRowDTO;
import com.customcharge.dashboard.service.EntityChargeSearchService;
import com.customcharge.persistencelayer.export.SmallDatasetExcelExporter;
import com.customcharge.persistencelayer.search.SearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;

@Service
public class EntityChargeExportService {

    private final EntityChargeSearchService entityChargeSearchService;
    private final SmallDatasetExcelExporter smallDatasetExcelExporter;

    public EntityChargeExportService(
            EntityChargeSearchService entityChargeSearchService,
            SmallDatasetExcelExporter smallDatasetExcelExporter) {
        this.entityChargeSearchService = entityChargeSearchService;
        this.smallDatasetExcelExporter = smallDatasetExcelExporter;
    }

    public StreamingResponseBody exportEntityChargeData(List<SearchCriteria> filters, Integer page, Integer size) {
        Page<EntityChargeGridRowDTO> result = entityChargeSearchService.search(filters, page, size);
        List<List<String>> rows = result.getContent().stream().map(this::toRow).toList();
        List<String> headers = ExportHeaderConstants.ENTITY_CHARGE_HEADERS;
        return outputStream -> {
            smallDatasetExcelExporter.writeExcelFile(headers, rows, outputStream);
            outputStream.flush();
        };
    }

    private List<String> toRow(EntityChargeGridRowDTO row) {
        return List.of(
                safe(row.getEntity()),
                safe(row.getTrackingNumber()),
                safe(row.getAccountNumber()),
                safe(row.getAccountName()),
                safe(row.getOrigin()),
                safe(row.getDestination()),
                safe(row.getThirdParty()),
                safe(row.getChargeGroup()),
                safe(row.getChargeCode()),
                safe(row.getCurrency()),
                safe(row.getTotalOutstanding()),
                safe(row.getTotalBalance()),
                safe(row.getStatus())
        );
    }

    private static String safe(Object value) {
        return value != null ? value.toString() : "";
    }
}
