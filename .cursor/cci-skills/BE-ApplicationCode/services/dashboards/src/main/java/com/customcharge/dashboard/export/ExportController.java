package com.customcharge.dashboard.export;

import com.customcharge.dashboard.constants.ExportConstants;
import com.customcharge.persistencelayer.exception.BadRequestException;
import com.customcharge.persistencelayer.search.SearchCriteria;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/export")
public class ExportController {

    private final ExportService roleExportService;
    private final EntityChargeExportService entityChargeExportService;

    public ExportController(ExportService roleExportService,
                            EntityChargeExportService entityChargeExportService) {
        this.roleExportService = roleExportService;
        this.entityChargeExportService = entityChargeExportService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('CUSTOMS_EXPORT', 'DOCUMENTS_LIST_EXPORT', 'REPORTS_EXPORT', 'AR_MANAGEMENT_EXPORT', 'AP_MANAGEMENT_EXPORT', 'GLOBAL_SETTINGS_EXPORT', 'ENTITY_SETTINGS_EXPORT','B2B_KEYWORDS_MANAGER_EXPORT')")
    public ResponseEntity<StreamingResponseBody> export() {
        try {
            StreamingResponseBody excelStream = roleExportService.exportRoleData();
            return buildResponse(excelStream, buildFileName("Role"));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ex.getMessage(), ex);
        }
    }

    @GetMapping("/service-code")
    @PreAuthorize("hasAnyAuthority('CUSTOMS_EXPORT', 'DOCUMENTS_LIST_EXPORT', 'REPORTS_EXPORT', 'AR_MANAGEMENT_EXPORT', 'AP_MANAGEMENT_EXPORT', 'ENTITY_SETTINGS_EXPORT')")
    public ResponseEntity<StreamingResponseBody> exportServiceCode(
            @RequestParam(required = false) String accountId,
            @RequestParam String entityCode,
            @RequestParam String chargeType) {
        try {
            StreamingResponseBody excelStream =
                    roleExportService.exportServiceChargeData(accountId, entityCode, chargeType);
            return buildResponse(excelStream, buildFileName("ServiceCode"));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ex.getMessage(), ex);
        }
    }

    @GetMapping("/rate-sheet")
    @PreAuthorize("hasAnyAuthority('CUSTOMS_EXPORT', 'DOCUMENTS_LIST_EXPORT', 'REPORTS_EXPORT', 'AR_MANAGEMENT_EXPORT', 'AP_MANAGEMENT_EXPORT', 'ENTITY_SETTINGS_EXPORT')")
    public ResponseEntity<StreamingResponseBody> exportRateSheet(@RequestParam UUID accountId) {
        try {
            StreamingResponseBody excelStream = roleExportService.exportRateSheetData(accountId);
            return buildResponse(excelStream, buildFileName("RateSheet"));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ex.getMessage(), ex);
        }
    }

    @GetMapping("/rate-sheet/import-template")
    @PreAuthorize("hasAnyAuthority('CUSTOMS_EXPORT', 'DOCUMENTS_LIST_EXPORT', 'REPORTS_EXPORT', 'AR_MANAGEMENT_EXPORT', 'AP_MANAGEMENT_EXPORT', 'ENTITY_SETTINGS_EXPORT')")
    public ResponseEntity<StreamingResponseBody> downloadRateSheetImportTemplate() {
        try {
            StreamingResponseBody excelStream = roleExportService.exportRateSheetImportTemplate();
            return buildResponse(excelStream, buildFileName("RateSheetImportTemplate"));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ex.getMessage(), ex);
        }
    }

    @PostMapping("/entity-charges")
    @PreAuthorize("hasAnyAuthority('CUSTOMS_EXPORT', 'DOCUMENTS_LIST_EXPORT', 'REPORTS_EXPORT', 'AR_MANAGEMENT_EXPORT', 'AP_MANAGEMENT_EXPORT', 'ENTITY_SETTINGS_EXPORT')")
    public ResponseEntity<StreamingResponseBody> exportEntityCharges(
            @RequestBody(required = false) List<SearchCriteria> filters,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {
        try {
            StreamingResponseBody excelStream = entityChargeExportService.exportEntityChargeData(filters, page, size);
            return buildResponse(excelStream, buildFileName("EntityCharges"));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ex.getMessage(), ex);
        }
    }

    private ResponseEntity<StreamingResponseBody> buildResponse(StreamingResponseBody excelStream, String fileName) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(ExportConstants.MEDIA_TYPE_XLSX))
                .header(HttpHeaders.CONNECTION, "close")
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(fileName, StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(excelStream);
    }

    private String buildFileName(String prefix) {
        return prefix + "_" + LocalDateTime.now().format(ExportConstants.EXPORT_FILE_TIMESTAMP_LOCAL) + ".xlsx";
    }
}
