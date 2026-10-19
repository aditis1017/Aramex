package com.customcharge.persistencelayer.export.service;

import com.customcharge.persistencelayer.config.aws.S3Properties;
import com.customcharge.persistencelayer.export.ExportWriters;
import com.customcharge.persistencelayer.export.config.ExportEngineProperties;
import com.customcharge.persistencelayer.export.event.ExportProgressEvent;
import com.customcharge.persistencelayer.export.model.ExportContext;
import com.customcharge.persistencelayer.export.model.ExportFormat;
import com.customcharge.persistencelayer.export.model.ExportRow;
import com.customcharge.persistencelayer.export.registry.ExportItemProcessor;
import com.customcharge.persistencelayer.export.registry.ExportItemReader;
import com.customcharge.persistencelayer.export.registry.ExportPipeline;
import com.customcharge.persistencelayer.export.registry.ExportPipelineRegistry;
import com.customcharge.persistencelayer.export.repository.ExportJobRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.Nullable;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.regex.Pattern;

public class ExportJobRunner {

    private static final Logger log = LoggerFactory.getLogger(ExportJobRunner.class);
    private static final Pattern SAFE_EXPORT_TYPE_SEGMENT = Pattern.compile("^[A-Za-z0-9_]+$");

    private final ExportPipelineRegistry exportPipelineRegistry;
    private final ObjectMapper objectMapper;
    private final ExportJobProgressService exportJobProgressService;
    private final ExportJobRepository exportJobRepository;
    private final ObjectProvider<S3Client> s3Client;
    @Nullable
    private final S3Properties s3Properties;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ExportEngineProperties exportEngineProperties;

    public ExportJobRunner(
            ExportPipelineRegistry exportPipelineRegistry,
            ObjectMapper objectMapper,
            ExportJobProgressService exportJobProgressService,
            ExportJobRepository exportJobRepository,
            ObjectProvider<S3Client> s3Client,
            ObjectProvider<S3Properties> s3Properties,
            ApplicationEventPublisher applicationEventPublisher,
            ExportEngineProperties exportEngineProperties) {
        this.exportPipelineRegistry = exportPipelineRegistry;
        this.objectMapper = objectMapper;
        this.exportJobProgressService = exportJobProgressService;
        this.exportJobRepository = exportJobRepository;
        this.s3Client = s3Client;
        this.s3Properties = s3Properties.getIfAvailable();
        this.applicationEventPublisher = applicationEventPublisher;
        this.exportEngineProperties = exportEngineProperties;
    }

    public void run(ExportContext context) {
        UUID jobId = context.jobId();
        exportJobProgressService.markInProgress(jobId);
        publishProgress(jobId);
        try {
            long written = executeExport(context);
            uploadToS3(context, written);
        } catch (Exception e) {
            log.error("Export job failed jobId={}", jobId, e);
            exportJobProgressService.markFailed(jobId, "EXPORT_EXECUTION_FAILED", e.getMessage());
            publishProgress(jobId);
            deleteTempFileQuietly(context.localFilePath());
        }
    }

    private long executeExport(ExportContext context) throws Exception {
        ExportPipeline pipeline = exportPipelineRegistry.require(context.exportType());
        resolveTotalRecords(context, pipeline);

        ExportItemReader reader = pipeline.createReader(context);
        ExportItemProcessor processor = pipeline.createProcessor(context);

        int bufferSize = Math.max(1, exportEngineProperties.getChunkSize());
        int progressInterval = Math.max(1, exportEngineProperties.getProgressUpdateInterval());
        long written = 0L;
        List<ExportRow> buffer = new ArrayList<>(bufferSize);

        try (ExportWriters.Writer fileWriter = ExportWriters.open(context, objectMapper)) {
            Object item;
            while ((item = reader.read()) != null) {
                ExportRow row = processor.process(item);
                if (row == null) {
                    continue;
                }
                buffer.add(row);
                if (buffer.size() >= bufferSize) {
                    fileWriter.writeRows(buffer);
                    written += buffer.size();
                    buffer.clear();
                    maybePublishProgress(context.jobId(), written, progressInterval);
                }
            }
            if (!buffer.isEmpty()) {
                fileWriter.writeRows(buffer);
                written += buffer.size();
            }
            exportJobProgressService.applyWriteProgress(context.jobId(), written);
            publishProgress(context.jobId());
            return written;
        } finally {
            reader.close();
        }
    }

    private void resolveTotalRecords(ExportContext context, ExportPipeline pipeline) {
        try {
            OptionalLong total = pipeline.resolveExportTotalRecords(context);
            if (total.isPresent() && total.getAsLong() > 0) {
                exportJobProgressService.setTotalRecords(context.jobId(), total.getAsLong());
                publishProgress(context.jobId());
            }
        } catch (Exception e) {
            log.warn("Could not resolve export totalRecords exportType={}: {}", context.exportType(), e.getMessage());
        }
    }

    private void uploadToS3(ExportContext context, long written) {
        UUID jobId = context.jobId();
        Path local = context.localFilePath();
        try {
            S3Client client = s3Client.getIfAvailable();
            if (client == null) {
                exportJobProgressService.markFailed(jobId, "S3_DISABLED", "S3 client is not available");
                return;
            }
            if (s3Properties == null) {
                exportJobProgressService.markFailed(jobId, "S3_CONFIG", "S3 properties are not available");
                return;
            }
            String bucket = s3Properties.getDefaultBucket();
            if (bucket == null || bucket.isBlank()) {
                exportJobProgressService.markFailed(jobId, "S3_CONFIG", "mvp.persistence.aws.s3.default-bucket is not set");
                return;
            }

            String ext = context.format() == ExportFormat.XLSX ? ".xlsx" : ".csv";
            String contentType = ext.equals(".xlsx")
                    ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    : "text/csv";
            String key = "exports/" + sanitizeExportType(context.exportType()) + "/"
                    + (context.exportFileName() != null && !context.exportFileName().isBlank()
                    ? context.exportFileName()
                    : jobId + ext);

            long size = Files.exists(local) ? Files.size(local) : written;
            client.putObject(
                    PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build(),
                    RequestBody.fromFile(local));
            exportJobProgressService.markCompleted(jobId, bucket, key, size, contentType);
        } catch (Exception e) {
            log.error("Export finalize failed jobId={}", jobId, e);
            exportJobProgressService.markFailed(jobId, "EXPORT_FINALIZE_FAILED", e.getMessage());
        } finally {
            deleteTempFileQuietly(local);
            publishProgress(jobId);
        }
    }

    private void maybePublishProgress(UUID jobId, long written, int progressInterval) {
        if (written % progressInterval == 0) {
            exportJobProgressService.applyWriteProgress(jobId, written);
            publishProgress(jobId);
        }
    }

    private void publishProgress(UUID jobId) {
        exportJobRepository.findById(jobId).ifPresent(job -> applicationEventPublisher.publishEvent(
                new ExportProgressEvent(jobId, exportJobProgressService.toPayload(job))));
    }

    private static String sanitizeExportType(String exportType) {
        if (exportType == null || exportType.isBlank()) {
            return "UNKNOWN";
        }
        String t = exportType.trim().toUpperCase(Locale.ROOT);
        return SAFE_EXPORT_TYPE_SEGMENT.matcher(t).matches() ? t : t.replaceAll("[^A-Za-z0-9_]+", "_");
    }

    private static void deleteTempFileQuietly(Path local) {
        try {
            Files.deleteIfExists(local);
        } catch (Exception e) {
            log.debug("Could not delete temp export file {}", local, e);
        }
    }
}
