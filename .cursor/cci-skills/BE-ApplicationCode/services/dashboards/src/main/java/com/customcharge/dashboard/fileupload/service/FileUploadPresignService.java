package com.customcharge.dashboard.fileupload.service;

import com.customcharge.dashboard.constants.FileUploadConstants;
import com.customcharge.dashboard.constants.FileUploadLogMessages;
import com.customcharge.dashboard.fileupload.config.FileUploadProperties;
import com.customcharge.dashboard.fileupload.dto.PresignUploadRequest;
import com.customcharge.dashboard.fileupload.dto.PresignUploadResponse;
import com.customcharge.persistencelayer.config.aws.S3Properties;
import com.customcharge.persistencelayer.entities.StagedFileUpload;
import com.customcharge.persistencelayer.enums.FileUploadStatus;
import com.customcharge.persistencelayer.exception.BadRequestException;
import com.customcharge.persistencelayer.repository.StagedFileUploadRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
@ConditionalOnBean(S3Presigner.class)
@Slf4j
public class FileUploadPresignService {

    private final S3Presigner presigner;
    private final S3Properties s3Properties;
    private final FileUploadProperties uploadProperties;
    private final StagedFileUploadRepository stagedFileUploadRepository;

    public FileUploadPresignService(
            S3Presigner presigner,
            S3Properties s3Properties,
            FileUploadProperties uploadProperties,
            StagedFileUploadRepository stagedFileUploadRepository) {
        this.presigner = presigner;
        this.s3Properties = s3Properties;
        this.uploadProperties = uploadProperties;
        this.stagedFileUploadRepository = stagedFileUploadRepository;
    }

    @Transactional(readOnly = false)
    public PresignUploadResponse presign(PresignUploadRequest req) {
        String safe = sanitize(req.getFileName());
        String low = safe.toLowerCase(Locale.ROOT);
        if (low.endsWith(FileUploadConstants.EXTENSION_XLS)
                || (!low.endsWith(FileUploadConstants.EXTENSION_CSV) && !low.endsWith(FileUploadConstants.EXTENSION_XLSX))) {
            throw new BadRequestException(FileUploadConstants.MESSAGE_INVALID_FILE_EXTENSION);
        }
        String bucket = s3Properties.getDefaultBucket();
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException(FileUploadConstants.MESSAGE_BUCKET_MISSING);
        }
        String prefix = uploadProperties.getIncomingUploadKeyPrefix()
                .replaceAll(FileUploadConstants.KEY_PREFIX_TRIM_PATTERN, "");
        String entityPath = req.getEntityName();
        String key = entityPath == null
                ? prefix + "/" + req.getFileId() + "/" + safe
                : prefix + "/" + entityPath + "/" + req.getFileId() + "/" + safe;
        if (key.contains(FileUploadConstants.PATH_TRAVERSAL_SEQUENCE)) {
            throw new BadRequestException(FileUploadConstants.MESSAGE_INVALID_S3_KEY);
        }

        String ct = req.getContentType() == null || req.getContentType().isBlank()
                ? FileUploadConstants.DEFAULT_CONTENT_TYPE
                : req.getContentType().trim();
        int semi = ct.indexOf(';');
        if (semi >= 0) {
            ct = ct.substring(0, semi).trim();
        }

        var put = PutObjectRequest.builder().bucket(bucket).key(key).contentType(ct).build();
        var signed = presigner.presignPutObject(
                r -> r.signatureDuration(Duration.ofSeconds(
                                Math.max(FileUploadConstants.MIN_PRESIGN_TTL_SECONDS, uploadProperties.getPresignTtlSeconds())))
                        .putObjectRequest(put));
        Map<String, String> headers = new LinkedHashMap<>();
        signed.httpRequest().headers().forEach((n, vals) -> {
            if (vals != null && !vals.isEmpty()) {
                headers.put(n, vals.get(0));
            }
        });

        StagedFileUpload entity = new StagedFileUpload();
        entity.setFileId(req.getFileId().toString());
        entity.setUploadProfile(req.getFileFor().strip());
        entity.setOriginalFileName(safe);
        entity.setS3Bucket(bucket);
        entity.setS3Key(key);
        entity.setStatus(FileUploadStatus.UPLOADED);
        entity.setEntityCode(entityPath);
        try {
            stagedFileUploadRepository.save(entity);
        } catch (DataIntegrityViolationException ex) {
            throw new BadRequestException(FileUploadConstants.MESSAGE_FILE_ID_EXISTS);
        }

        log.info(FileUploadLogMessages.FILE_UPLOAD_STAGED, req.getFileId(), req.getFileFor(), key);
        return new PresignUploadResponse(
                signed.url().toString(),
                key,
                signed.httpRequest().method().toString(),
                req.getFileId(),
                Map.copyOf(headers));
    }

    private static String sanitize(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new BadRequestException(FileUploadConstants.MESSAGE_FILE_NAME_REQUIRED);
        }
        int s = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
        String base = s >= 0 ? fileName.substring(s + 1) : fileName;
        if (base.contains(FileUploadConstants.PATH_TRAVERSAL_SEQUENCE)) {
            throw new BadRequestException(FileUploadConstants.MESSAGE_INVALID_FILE_NAME);
        }
        String cleaned = base.replaceAll(FileUploadConstants.FILE_NAME_SANITIZE_PATTERN, "_");
        if (cleaned.isBlank()) {
            throw new BadRequestException(FileUploadConstants.MESSAGE_INVALID_FILE_NAME);
        }
        if (cleaned.length() > FileUploadConstants.MAX_FILE_NAME_LENGTH) {
            cleaned = cleaned.substring(0, FileUploadConstants.MAX_FILE_NAME_LENGTH);
        }
        return cleaned;
    }
}
