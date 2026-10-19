package com.customcharge.persistencelayer.export.web;

import com.customcharge.persistencelayer.export.model.ExportJobStatus;
import com.customcharge.persistencelayer.export.service.ExportEngineService;
import com.customcharge.persistencelayer.export.service.ExportDownloadLinkService;
import com.customcharge.persistencelayer.export.web.dto.CreateExportRequest;
import com.customcharge.persistencelayer.export.web.dto.CreateExportResponse;
import com.customcharge.persistencelayer.export.web.dto.ExportDownloadResponse;
import com.customcharge.persistencelayer.export.web.dto.ExportJobStatusResponse;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/exports")
@ConditionalOnProperty(prefix = "mvp.persistence.export", name = "enabled", havingValue = "true")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class ExportRestController {

    private final ExportEngineService exportEngineService;
    private final Optional<ExportDownloadLinkService> exportDownloadLinkService;

    public ExportRestController(
            ExportEngineService exportEngineService,
            Optional<ExportDownloadLinkService> exportDownloadLinkService) {
        this.exportEngineService = exportEngineService;
        this.exportDownloadLinkService = exportDownloadLinkService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('REPORTS_EXPORT')")
    public ResponseEntity<CreateExportResponse> create(
            @Valid @RequestBody CreateExportRequest request,
            @RequestHeader(value = "X-Requested-By", required = false) String requestedBy) throws Exception {
        UUID jobId = exportEngineService.startExport(request, requestedBy);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new CreateExportResponse(jobId, ExportJobStatus.INITIATED));
    }

    @GetMapping("/{jobId}")
    @PreAuthorize("hasAuthority('REPORTS_VIEW')")
    public ExportJobStatusResponse status(@PathVariable UUID jobId) {
        return exportEngineService.buildStatusResponse(jobId);
    }

    @GetMapping("/{jobId}/download")
    @PreAuthorize("hasAuthority('REPORTS_EXPORT')")
    public ExportDownloadResponse download(@PathVariable UUID jobId) {
        return exportEngineService.createDownloadResponse(jobId, exportDownloadLinkService);
    }
}
