package com.customcharge.persistencelayer.export.service;

import com.customcharge.persistencelayer.export.config.ExportEngineProperties;
import com.customcharge.persistencelayer.export.web.dto.ExportDownloadResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;

@Service
@ConditionalOnBean(S3Presigner.class)
@ConditionalOnProperty(prefix = "mvp.persistence.export", name = "enabled", havingValue = "true")
public class ExportDownloadLinkService {

    private final S3Presigner s3Presigner;
    private final ExportEngineProperties exportEngineProperties;

    public ExportDownloadLinkService(S3Presigner s3Presigner, ExportEngineProperties exportEngineProperties) {
        this.s3Presigner = s3Presigner;
        this.exportEngineProperties = exportEngineProperties;
    }

    public URL presignGetObject(String bucket, String key, String contentType) {
        GetObjectRequest.Builder getReq = GetObjectRequest.builder().bucket(bucket).key(key);
        if (contentType != null && !contentType.isBlank()) {
            getReq.responseContentType(contentType);
        }
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(exportEngineProperties.getPresignedUrlDurationMinutes()))
                .getObjectRequest(getReq.build())
                .build();
        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
        return presigned.url();
    }

    public ExportDownloadResponse presignDownload(String bucket, String key, String contentType) {
        URL url = presignGetObject(bucket, key, contentType);
        Instant expiresAt = Instant.now().plus(
                Duration.ofMinutes(exportEngineProperties.getPresignedUrlDurationMinutes()));
        return new ExportDownloadResponse(url, expiresAt);
    }
}
