package com.customcharge.chargescalculator.service.errorreport;

import com.customcharge.chargescalculator.config.CustomChargeProperties;
import com.customcharge.chargescalculator.model.ErrorReportStorageResult;
import com.customcharge.persistencelayer.config.aws.S3Properties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@ConditionalOnBean({S3Presigner.class})
public class ErrorReportStorageService {

    private final S3Client s3Client;
    private final S3Properties s3Properties;
    private final S3Presigner s3Presigner;
    private final CustomChargeProperties customChargeProperties;

    public ErrorReportStorageService(S3Client s3Client,
                                     S3Properties s3Properties,
                                     S3Presigner s3Presigner,
                                     CustomChargeProperties customChargeProperties) {
        this.s3Client = s3Client;
        this.s3Properties = s3Properties;
        this.s3Presigner = s3Presigner;
        this.customChargeProperties = customChargeProperties;
    }

    public ErrorReportStorageResult uploadAndGenerateLink(UUID fileId, String sourceFileName, Path reportPath) {
        String bucket = requireBucket();
        String key = buildKey(fileId, sourceFileName);
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                        .build(),
                RequestBody.fromFile(reportPath));
        String url = s3Presigner.presignGetObject(GetObjectPresignRequest.builder()
                        .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(key).build())
                        .signatureDuration(Duration.ofMinutes(customChargeProperties.getErrorReport().getPresignedUrlExpiryMinutes()))
                        .build())
                .url()
                .toExternalForm();
        return new ErrorReportStorageResult(bucket, key, url);
    }

    private String requireBucket() {
        String bucket = s3Properties.getDefaultBucket();
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("mvp.persistence.aws.s3.default-bucket must be configured");
        }
        return bucket;
    }

    private String buildKey(UUID fileId, String fileName) {
        String prefix = customChargeProperties.getErrorReport().getUploadPrefix();
        String normalizedPrefix = (prefix == null || prefix.isBlank()) ? "error-reports" : prefix;
        String cleanedName = fileName == null || fileName.isBlank() ? "error-report" : fileName;
        return normalizedPrefix + "/" + fileId + "/" + cleanedName + "-" + DateTimeFormatter.ISO_INSTANT.format(Instant.now()) + ".xlsx";
    }
}
