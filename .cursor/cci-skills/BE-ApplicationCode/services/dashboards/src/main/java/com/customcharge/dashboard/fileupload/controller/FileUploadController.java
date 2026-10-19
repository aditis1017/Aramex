package com.customcharge.dashboard.fileupload.controller;

import com.customcharge.dashboard.fileupload.dto.PresignUploadRequest;
import com.customcharge.dashboard.fileupload.dto.PresignUploadResponse;
import com.customcharge.dashboard.fileupload.service.FileUploadPresignService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/import-s3")
public class FileUploadController {

    private final Optional<FileUploadPresignService> presignService;

    public FileUploadController(Optional<FileUploadPresignService> presignService) {
        this.presignService = presignService;
    }

    @PostMapping("/presign")
    @PreAuthorize("hasAnyAuthority('CUSTOMS_EXPORT', 'DOCUMENTS_LIST_EXPORT', 'REPORTS_EXPORT', 'AR_MANAGEMENT_EXPORT', 'AP_MANAGEMENT_EXPORT', 'GLOBAL_SETTINGS_EXPORT', 'ENTITY_SETTINGS_EXPORT','B2B_KEYWORDS_MANAGER_EXPORT')")
    public PresignUploadResponse presign(@Valid @RequestBody PresignUploadRequest request) {
        request.setFileId(UUID.randomUUID());
        return presignService.orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "S3 presign not configured"))
                .presign(request);
    }
}
