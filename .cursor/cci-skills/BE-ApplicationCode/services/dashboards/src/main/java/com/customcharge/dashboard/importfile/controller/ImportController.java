package com.customcharge.dashboard.importfile.controller;

import com.customcharge.dashboard.importfile.dto.ImportColumnsResponse;
import com.customcharge.dashboard.importfile.dto.ImportResponse;
import com.customcharge.dashboard.importfile.service.ImportColumnsService;
import com.customcharge.dashboard.importfile.service.ImportService;
import com.customcharge.dashboard.importfile.service.ImportTemplateDownloadService;
import com.customcharge.persistencelayer.exception.BadRequestException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/import")
@Validated
public class ImportController {

    private static final MediaType XLSX_MEDIA_TYPE =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private final ImportService importService;
    private final ImportTemplateDownloadService importTemplateDownloadService;
    private final ImportColumnsService importColumnsService;
    private final ObjectMapper objectMapper;

    public ImportController(
            ImportService importService,
            ImportTemplateDownloadService importTemplateDownloadService,
            ImportColumnsService importColumnsService,
            ObjectMapper objectMapper) {
        this.importService = importService;
        this.importTemplateDownloadService = importTemplateDownloadService;
        this.importColumnsService = importColumnsService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/columns")
    @PreAuthorize("hasAnyAuthority('CUSTOMS_EXPORT', 'DOCUMENTS_LIST_EXPORT', 'REPORTS_EXPORT', 'AR_MANAGEMENT_EXPORT', 'AP_MANAGEMENT_EXPORT', 'GLOBAL_SETTINGS_EXPORT', 'ENTITY_SETTINGS_EXPORT','B2B_KEYWORDS_MANAGER_EXPORT')")
    public ImportColumnsResponse getColumns(
            @RequestParam("entity") @NotBlank String entity,
            @RequestParam("fileType") @NotBlank String fileType) {
        return new ImportColumnsResponse(importColumnsService.resolveColumns(entity, fileType));
    }

    @GetMapping("/template")
    @PreAuthorize("hasAnyAuthority('CUSTOMS_EXPORT', 'DOCUMENTS_LIST_EXPORT', 'REPORTS_EXPORT', 'AR_MANAGEMENT_EXPORT', 'AP_MANAGEMENT_EXPORT', 'GLOBAL_SETTINGS_EXPORT', 'ENTITY_SETTINGS_EXPORT','B2B_KEYWORDS_MANAGER_EXPORT')")
    public ResponseEntity<StreamingResponseBody> downloadTemplate(
            @RequestParam("templateKey") @NotBlank String templateKey,
            @RequestParam(value = "entity", required = false) String entity
    ) {
        ImportTemplateDownloadService.TemplateStream t = importTemplateDownloadService.open(templateKey, entity);
        return ResponseEntity.ok()
                .contentType(XLSX_MEDIA_TYPE)
                .header(HttpHeaders.CONNECTION, "close")
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(t.fileName(), StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .body(t.body());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('CUSTOMS_EXPORT', 'DOCUMENTS_LIST_EXPORT', 'REPORTS_EXPORT', 'AR_MANAGEMENT_EXPORT', 'AP_MANAGEMENT_EXPORT', 'GLOBAL_SETTINGS_EXPORT', 'ENTITY_SETTINGS_EXPORT','B2B_KEYWORDS_MANAGER_EXPORT')")
    public ImportResponse importFile(
            @RequestParam("screenName") @NotBlank String screenName,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "params", required = false) String params) {
        return importService.importFile(screenName, file, parseParams(params));
    }

    private Map<String, Object> parseParams(String params) {
        if (params == null || params.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(params, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception ex) {
            throw new BadRequestException("Parameter 'params' must be valid JSON.", ex);
        }
    }
}
