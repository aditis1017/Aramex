package com.customcharge.fileorchestration.messaging;

import com.customcharge.fileorchestration.pipeline.FileOrchestrationPipeline;
import com.customcharge.fileorchestration.pipeline.FileProcessingRequest;
import com.customcharge.fileorchestration.process.FileProcessRedisStore;
import com.customcharge.fileorchestration.dto.s3.S3EventNotification;
import com.customcharge.fileorchestration.dto.s3.S3EventRecord;
import com.customcharge.fileorchestration.service.FileTypeSupportService;
import com.customcharge.persistencelayer.entities.StagedFileUpload;
import com.customcharge.persistencelayer.enums.FileUploadStatus;
import com.customcharge.persistencelayer.repository.StagedFileUploadRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Component
public class SqsUploadMessageListener {

    private static final Logger log = LoggerFactory.getLogger(SqsUploadMessageListener.class);
    private static final String TRACE_ID_MDC = "traceId";

    private final ObjectMapper objectMapper;
    private final FileTypeSupportService fileTypeSupportService;
    private final FileOrchestrationPipeline fileOrchestrationPipeline;
    private final FileProcessRedisStore processRedisStore;
    private final StagedFileUploadRepository stagedFileUploadRepository;

    public SqsUploadMessageListener(
            ObjectMapper objectMapper,
            FileTypeSupportService fileTypeSupportService,
            FileOrchestrationPipeline fileOrchestrationPipeline,
            FileProcessRedisStore processRedisStore,
            StagedFileUploadRepository stagedFileUploadRepository) {
        this.objectMapper = objectMapper;
        this.fileTypeSupportService = fileTypeSupportService;
        this.fileOrchestrationPipeline = fileOrchestrationPipeline;
        this.processRedisStore = processRedisStore;
        this.stagedFileUploadRepository = stagedFileUploadRepository;
    }

    @SqsListener(
            value = "${file-orchestration.batch.input-queue}",
            maxConcurrentMessages = "${file-orchestration.batch.listener-concurrency}",
            maxMessagesPerPoll = "${file-orchestration.batch.listener-concurrency}")
    public void listen(String rawMessage) throws Exception {
        String traceId = UUID.randomUUID().toString();
        MDC.put(TRACE_ID_MDC, traceId);
        try {
            if (rawMessage != null && rawMessage.contains("\"Event\":\"s3:TestEvent\"")) {
                log.info("Ignoring S3 test event");
                return;
            }
            log.info("Received Message From S3: {}", rawMessage);
            S3EventRecord record = parseCreateRecord(rawMessage);
            String bucket = record.getS3().getBucket().getName();
            String key = URLDecoder.decode(record.getS3().getObject().getKey(), StandardCharsets.UTF_8);

            StagedFileUpload staged = stagedFileUploadRepository
                    .findByS3BucketAndS3Key(bucket, key)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No staged upload for bucket=" + bucket + " key=" + key));

            if (staged.getStatus() == FileUploadStatus.VALIDATED) {
                log.info("Skipping already validated upload fileId={} key={}", staged.getFileId(), key);
                return;
            }

            if (staged.getExpectedDataChunks() != null) {
                log.info("Skipping already orchestrated upload fileId={} key={}", staged.getFileId(), key);
                return;
            }

            FileProcessingRequest request = buildRequest(staged, traceId);
            if (!processRedisStore.tryAcquire(request)) {
                log.info("Skipping upload already locked in Redis fileId={}", request.fileId());
                return;
            }

            fileOrchestrationPipeline.process(request);
            log.info("Pipeline completed documentPath={} fileName={} fileType={} fileId={} traceId={}",
                    staged.getS3Key(),
                    staged.getOriginalFileName(),
                    request.fileType(),
                    staged.getFileId(),
                    traceId);
        } finally {
            MDC.remove(TRACE_ID_MDC);
        }
    }

    private FileProcessingRequest buildRequest(StagedFileUpload staged, String traceId) {
        String fileType = resolveFileType(staged.getOriginalFileName());
        if (!fileTypeSupportService.supports(fileType)) {
            throw new IllegalArgumentException("Unsupported fileType: " + fileType);
        }

        return new FileProcessingRequest(
                staged.getFileId(),
                staged.getCreatedBy(),
                staged.getS3Key(),
                staged.getOriginalFileName(),
                fileType,
                staged.getUploadProfile(),
                staged.getEntityCode() != null ? staged.getEntityCode() : "",
                traceId);
    }

    private S3EventRecord parseCreateRecord(String rawMessage) throws Exception {
        S3EventNotification notification = objectMapper.readValue(rawMessage, S3EventNotification.class);
        List<S3EventRecord> records = notification.getRecords();
        if (records == null || records.isEmpty()) {
            throw new IllegalArgumentException("Invalid S3 event: missing Records");
        }
        S3EventRecord record = records.getFirst();
        if (record.getEventName() == null || !record.getEventName().startsWith("ObjectCreated:")) {
            throw new IllegalArgumentException("Unsupported S3 event: " + record.getEventName());
        }
        if (record.getS3() == null
                || record.getS3().getBucket() == null
                || record.getS3().getObject() == null
                || record.getS3().getBucket().getName() == null
                || record.getS3().getObject().getKey() == null) {
            throw new IllegalArgumentException("Invalid S3 event: missing bucket or object key");
        }
        return record;
    }

    private static String resolveFileType(String fileName) {
        String normalized = fileName.toLowerCase(Locale.ROOT);
        return normalized.endsWith(".xlsx") ? "XLSX" : "CSV";
    }
}
