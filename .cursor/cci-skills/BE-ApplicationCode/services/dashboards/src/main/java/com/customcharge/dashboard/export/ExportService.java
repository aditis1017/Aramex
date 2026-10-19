package com.customcharge.dashboard.export;

import com.customcharge.dashboard.constants.ChargeTypeConstants;
import com.customcharge.dashboard.constants.ExportConstants;
import com.customcharge.dashboard.constants.ExportHeaderConstants;
import com.customcharge.dashboard.dto.RoleDTO;
import com.customcharge.dashboard.dto.ServiceChargeResponse;
import com.customcharge.dashboard.service.RoleService;
import com.customcharge.dashboard.service.impl.ServiceChargeService;
import com.customcharge.dashboard.service.impl.ServiceCodesService;
import com.customcharge.persistencelayer.entities.RateSheetItem;
import com.customcharge.persistencelayer.entities.RateSheetItemRate;
import com.customcharge.persistencelayer.export.SmallDatasetExcelExporter;
import com.customcharge.persistencelayer.exception.ResourceNotFoundException;
import com.customcharge.persistencelayer.repository.RateSheetItemRepository;
import com.customcharge.persistencelayer.repository.RateSheetRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@Service
public class ExportService {
    private final RoleService roleService;
    private final ServiceChargeService serviceChargeService;
    private final RateSheetRepository rateSheetRepository;
    private final RateSheetItemRepository rateSheetItemRepository;
    private final SmallDatasetExcelExporter smallDatasetExcelExporter;

    public ExportService(RoleService roleService,
                         ServiceChargeService serviceChargeService,
                         RateSheetRepository rateSheetRepository,
                         RateSheetItemRepository rateSheetItemRepository,
                         SmallDatasetExcelExporter smallDatasetExcelExporter) {
        this.roleService = roleService;
        this.serviceChargeService = serviceChargeService;
        this.rateSheetRepository = rateSheetRepository;
        this.rateSheetItemRepository = rateSheetItemRepository;
        this.smallDatasetExcelExporter = smallDatasetExcelExporter;
    }

    public StreamingResponseBody exportRoleData() {
        List<List<String>> rows = roleService.findAll().stream().map(this::toRoleRow).toList();
        return writeExcelStream(ExportHeaderConstants.ROLE_HEADERS, rows);
    }

    public StreamingResponseBody exportServiceChargeData(String accountId, String entityCode, String chargeType) {
        String resolvedChargeType = ServiceCodesService.requireChargeType(chargeType);
        List<String> headers;
        Function<ServiceChargeResponse, List<String>> rowMapper;
        if (ChargeTypeConstants.COST_AP.contains(resolvedChargeType)) {
            headers = ExportHeaderConstants.SERVICE_CHARGE_HEADERS_COST_AP;
            rowMapper = this::toServiceChargeRowCostAp;
        } else if (ChargeTypeConstants.REVENUE_AR.contains(resolvedChargeType)) {
            headers = ExportHeaderConstants.SERVICE_CHARGE_HEADERS_REVENUE_DT;
            rowMapper = this::toServiceChargeRowRevenueDt;
        } else {
            throw new IllegalArgumentException(
                    "Service charge export is not supported for charge type "
                            + (resolvedChargeType.isEmpty() ? "(blank)" : resolvedChargeType)
                            + ". Supported types are ECD, ECS, EC3, DTS, DT3, DTA, and DTC.");
        }
        List<ServiceChargeResponse> serviceCharges =
                serviceChargeService.getServiceCharges(accountId, entityCode, chargeType);
        List<List<String>> rows = serviceCharges.stream().map(rowMapper).toList();
        return writeExcelStream(headers, rows);
    }

    public StreamingResponseBody exportRateSheetData(UUID accountId) {
        var rateSheet = rateSheetRepository.findByAccount_Id(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("RateSheet not found"));
        List<List<String>> rows = rateSheetItemRepository
                .findByRateSheet_IdAndIsDeletedFalse(rateSheet.getId())
                .stream()
                .map(this::toRateSheetExportRow)
                .toList();
        return outputStream -> smallDatasetExcelExporter.writeExcelFile(
                ExportHeaderConstants.RATE_SHEET_HEADERS, rows, outputStream);
    }

    public StreamingResponseBody exportRateSheetImportTemplate() {
        return writeExcelStream(ExportHeaderConstants.RATE_SHEET_HEADERS, Collections.emptyList());
    }

    private StreamingResponseBody writeExcelStream(List<String> headers, List<List<String>> rows) {
        return outputStream -> {
            smallDatasetExcelExporter.writeExcelFile(headers, rows, outputStream);
            outputStream.flush();
        };
    }

    private List<String> toRoleRow(RoleDTO role) {
        String label = role.getName() != null && !role.getName().isBlank()
                ? role.getName()
                : role.getDisplayName();
        return List.of(
                role.getId() != null ? role.getId().toString() : "",
                safe(role.getCode()),
                safe(label)
        );
    }

    private List<String> toServiceChargeRowCostAp(ServiceChargeResponse charge) {
        return List.of(
                safe(charge.getServiceCode()),
                safe(charge.getDescription()),
                safe(charge.getServiceDescription()),
                activeForExport(charge),
                safe(charge.getTaxCode()),
                formatTaxValue(charge.getTaxValue()),
                safe(charge.getDefaultAccountNumber()));
    }

    private List<String> toServiceChargeRowRevenueDt(ServiceChargeResponse charge) {
        return List.of(
                safe(charge.getServiceCode()),
                safe(charge.getDescription()),
                safe(charge.getServiceDescription()),
                activeForExport(charge),
                safe(charge.getTaxCode()),
                formatTaxValue(charge.getTaxValue()),
                billableOrNon(charge.getId() != null ? charge.getIsBillable() : null),
                safe(charge.getSplit() != null ? charge.getSplit() : ExportConstants.REVENUE_EXPORT_SPLIT));
    }

    /**
     * Master-only rows (no account service charge) are active by default;
     * configured charges use their stored {@code is_active} flag.
     */

    private static String activeForExport(ServiceChargeResponse charge) {
        if (charge.getId() == null) {
            return ExportConstants.EXPORT_VALUE_YES;
        }
        return yesNo(charge.getActive());
    }

    private static String yesNo(Boolean value) {
        if (value == null) {
            return "";
        }
        return Boolean.TRUE.equals(value) ? ExportConstants.EXPORT_VALUE_YES : ExportConstants.EXPORT_VALUE_NO;
    }

    private static String billableOrNon(Boolean billable) {
        if (billable == null) {
            return ExportConstants.EXPORT_VALUE_YES;
        }
        return Boolean.TRUE.equals(billable) ? ExportConstants.EXPORT_VALUE_YES : ExportConstants.EXPORT_VALUE_NO;
    }

    private static String formatTaxValue(Double taxValue) {
        return BigDecimal.valueOf(taxValue != null ? taxValue : 0.0).stripTrailingZeros().toPlainString();
    }

    private List<String> toRateSheetExportRow(RateSheetItem item) {
        List<String> row = new ArrayList<>();
        row.add(safe(item.getServiceCode()));
        row.add(safe(item.getDescription()));
        row.add(safe(item.getShipmentType()));
        row.add(safe(item.getBusinessType()));
        row.add(safe(item.getCategoryType()));
        List<RateSheetItemRate> rates = item.getRates() != null ? item.getRates() : List.of();

        for (int slot = 0; slot < ExportConstants.RATE_SHEET_RATE_SLOTS; slot++) {
            if (slot < rates.size()) {
                appendRateSlotColumns(row, rates.get(slot));
            } else {
                row.add(ExportConstants.RATE_SHEET_EMPTY_CELL);
                row.add(ExportConstants.RATE_SHEET_EMPTY_CELL);
                row.add(ExportConstants.RATE_SHEET_EMPTY_CELL);
            }
        }
        return List.copyOf(row);
    }

    private void appendRateSlotColumns(List<String> row, RateSheetItemRate rate) {
        row.add(exportRateType(rate));
        row.add(exportPercentageOf(rate));
        row.add(exportValue(rate));
    }

    private static String exportRateType(RateSheetItemRate rate) {
        if (rate == null || rate.getRateType() == null) {
            return ExportConstants.RATE_SHEET_EMPTY_CELL;
        }
        return rate.getRateType().getDisplayName();
    }

    private static String exportPercentageOf(RateSheetItemRate rate) {
        if (rate == null || rate.getPercentageOf() == null) {
            return ExportConstants.RATE_SHEET_EMPTY_CELL;
        }
        return rate.getPercentageOf().getDisplayName();
    }

    private static String exportValue(RateSheetItemRate rate) {
        if (rate == null || rate.getValue() == null) {
            return ExportConstants.RATE_SHEET_EMPTY_CELL;
        }
        return rate.getValue().stripTrailingZeros().toPlainString();
    }

    private String safe(String value) {
        return value != null ? value : "";
    }

    private String safe(Object value) {
        return value != null ? value.toString() : "";
    }
}
